package com.example.api_booking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTests {
  @Autowired MockMvc mvc;

  private ResultActions json(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      MockHttpSession session,
      String body)
      throws Exception {
    if (session != null) request.session(session);
    return mvc.perform(request.with(csrf()).contentType("application/json").content(body));
  }

  private MockHttpSession registerAndLogin(String email, String password) throws Exception {
    json(
            post("/api/v1/auth/register"),
            null,
            "{\"fullName\":\"Khách thử\",\"email\":\""
                + email
                + "\",\"password\":\""
                + password
                + "\",\"confirmPassword\":\""
                + password
                + "\"}")
        .andExpect(status().isCreated());
    return login(email, password);
  }

  private MockHttpSession login(String email, String password) throws Exception {
    return (MockHttpSession)
        json(
                post("/api/v1/auth/login"),
                null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void profileIsReadAndPatchedOnlyForTheSignedInUser() throws Exception {
    mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    MockHttpSession session = registerAndLogin("profile-a@example.com", "password-a1");

    mvc.perform(get("/api/v1/users/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("profile-a@example.com"))
        .andExpect(jsonPath("$.data.fullName").value("Khách thử"))
        .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
        .andExpect(jsonPath("$.data.phone").isEmpty())
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

    // Only sent fields change; email/role in the body are ignored.
    json(
            patch("/api/v1/users/me"),
            session,
            "{\"phone\":\"0901234567\",\"email\":\"x@example.com\",\"role\":\"ADMIN\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").value("0901234567"))
        .andExpect(jsonPath("$.data.fullName").value("Khách thử"))
        .andExpect(jsonPath("$.data.email").value("profile-a@example.com"))
        .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    json(patch("/api/v1/users/me"), session, "{\"fullName\":\"  Nguyễn Minh Anh  \"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Nguyễn Minh Anh"))
        .andExpect(jsonPath("$.data.phone").value("0901234567"));
    // null clears the phone; fullName cannot be blank; an empty body is rejected.
    json(patch("/api/v1/users/me"), session, "{\"phone\":null}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").isEmpty());
    json(patch("/api/v1/users/me"), session, "{\"fullName\":\"   \"}")
        .andExpect(status().isBadRequest());
    json(patch("/api/v1/users/me"), session, "{}").andExpect(status().isBadRequest());
    json(patch("/api/v1/users/me"), session, "{\"phone\":\"12ab\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.fieldErrors.phone").exists());

    // A phone number belongs to one account.
    json(patch("/api/v1/users/me"), session, "{\"phone\":\"0911111111\"}")
        .andExpect(status().isOk());
    MockHttpSession other = registerAndLogin("profile-b@example.com", "password-b1");
    json(patch("/api/v1/users/me"), other, "{\"phone\":\"0911111111\"}")
        .andExpect(status().isConflict());
    mvc.perform(
            patch("/api/v1/users/me")
                .session(other)
                .contentType("application/json")
                .content("{\"phone\":\"0922222222\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void changingPasswordChecksTheOldOneAndEndsExistingSessions() throws Exception {
    MockHttpSession session = registerAndLogin("profile-pass@example.com", "old-password-1");
    json(
            put("/api/v1/users/me/password"),
            session,
            "{\"oldPassword\":\"wrong-password\",\"newPassword\":\"new-password-1\",\"confirmNewPassword\":\"new-password-1\"}")
        .andExpect(status().isBadRequest());
    json(
            put("/api/v1/users/me/password"),
            session,
            "{\"oldPassword\":\"old-password-1\",\"newPassword\":\"new-password-1\",\"confirmNewPassword\":\"different-1\"}")
        .andExpect(status().isBadRequest());
    json(
            put("/api/v1/users/me/password"),
            session,
            "{\"oldPassword\":\"old-password-1\",\"newPassword\":\"short\",\"confirmNewPassword\":\"short\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.fieldErrors.newPassword").exists());
    json(
            put("/api/v1/users/me/password"),
            session,
            "{\"oldPassword\":\"old-password-1\",\"newPassword\":\"new-password-1\",\"confirmNewPassword\":\"new-password-1\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());

    mvc.perform(get("/api/v1/users/me").session(session)).andExpect(status().isUnauthorized());
    json(
            post("/api/v1/auth/login"),
            null,
            "{\"email\":\"profile-pass@example.com\",\"password\":\"old-password-1\"}")
        .andExpect(status().isUnauthorized());
    MockHttpSession fresh = login("profile-pass@example.com", "new-password-1");
    mvc.perform(get("/api/v1/users/me").session(fresh)).andExpect(status().isOk());
  }
}
