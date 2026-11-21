package com.example.api_booking;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api_booking.service.PasswordResetMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetTests {
  private static final String SAME_ANSWER =
      "Nếu email đã đăng ký, mã xác nhận sẽ được gửi trong ít phút.";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @MockitoBean PasswordResetMailer mailer;

  @BeforeEach
  void resetMailer() {
    clearInvocations(mailer);
  }

  private ResultActions postJson(String path, String json) throws Exception {
    return mvc.perform(post(path).with(csrf()).contentType("application/json").content(json));
  }

  private void register(String email, String password) throws Exception {
    postJson(
            "/api/v1/auth/register",
            "{\"fullName\":\"Khách thử\",\"email\":\""
                + email
                + "\",\"password\":\""
                + password
                + "\",\"confirmPassword\":\""
                + password
                + "\"}")
        .andExpect(status().isCreated());
  }

  private MockHttpSession login(String email, String password) throws Exception {
    return (MockHttpSession)
        postJson(
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  /** Requests a code and returns what would have been emailed. */
  private String requestCode(String email) throws Exception {
    clearInvocations(mailer);
    postJson("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(SAME_ANSWER));
    var code = ArgumentCaptor.forClass(String.class);
    verify(mailer).send(eq(email), code.capture());
    return code.getValue();
  }

  private String body(String email, String code, String password, String confirm) {
    return "{\"email\":\""
        + email
        + "\",\"code\":\""
        + code
        + "\",\"newPassword\":\""
        + password
        + "\",\"confirmPassword\":\""
        + confirm
        + "\"}";
  }

  private static String wrong(String code) {
    return code.equals("123456") ? "654321" : "123456";
  }

  @Test
  void resetChangesPasswordOnceAndEndsExistingSessions() throws Exception {
    String email = "reset-flow@example.com";
    register(email, "old-password-1");
    MockHttpSession oldSession = login(email, "old-password-1");

    String code = requestCode(email);
    org.assertj.core.api.Assertions.assertThat(code).matches("\\d{6}");
    org.assertj.core.api.Assertions.assertThat(
            db.queryForObject(
                "select code_hash from password_reset_codes c join app_users u on u.id=c.user_id"
                    + " where u.email=?",
                String.class,
                email))
        .isNotEqualTo(code);

    postJson("/api/v1/auth/verify-reset-code", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}")
        .andExpect(status().isOk());
    postJson("/api/v1/auth/reset-password", body(email, code, "new-password-1", "different-1"))
        .andExpect(status().isBadRequest());
    postJson("/api/auth/reset-password", body(email, code, "new-password-1", "new-password-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isEmpty());

    // The old session is invalidated by the security_version bump.
    mvc.perform(get("/api/v1/auth/me").session(oldSession)).andExpect(status().isUnauthorized());
    postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"old-password-1\"}")
        .andExpect(status().isUnauthorized());
    login(email, "new-password-1");

    // A code is single use.
    postJson("/api/v1/auth/reset-password", body(email, code, "other-password-1", "other-password-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.code").value("INVALID_RESET_CODE"));
    org.assertj.core.api.Assertions.assertThat(
            db.queryForObject(
                "select count(*) from audit_events a join app_users u on u.id=a.target_id"
                    + " where u.email=? and a.action='PASSWORD_RESET'",
                Long.class,
                email))
        .isEqualTo(1L);
  }

  @Test
  void unknownEmailGetsTheSameAnswerWithoutMail() throws Exception {
    postJson("/api/v1/auth/forgot-password", "{\"email\":\"nobody@example.com\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value(SAME_ANSWER));
    verify(mailer, never()).send(anyString(), anyString());
    postJson(
            "/api/v1/auth/verify-reset-code",
            "{\"email\":\"nobody@example.com\",\"code\":\"123456\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.code").value("INVALID_RESET_CODE"));
    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\":\"nobody@example.com\"}"))
        .andExpect(status().isForbidden());
    postJson("/api/v1/auth/verify-reset-code", "{\"email\":\"nobody@example.com\",\"code\":\"12ab\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.fieldErrors.code").exists());
  }

  @Test
  void codeIsLockedAfterFiveWrongAttempts() throws Exception {
    String email = "reset-attempts@example.com";
    register(email, "old-password-1");
    String code = requestCode(email);
    for (int i = 0; i < 5; i++)
      postJson(
              "/api/v1/auth/verify-reset-code",
              "{\"email\":\"" + email + "\",\"code\":\"" + wrong(code) + "\"}")
          .andExpect(status().isBadRequest());
    postJson("/api/v1/auth/verify-reset-code", "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.code").value("INVALID_RESET_CODE"));
  }

  @Test
  void expiredCodeIsRejectedAndResendRespectsCooldown() throws Exception {
    String email = "reset-expiry@example.com";
    register(email, "old-password-1");
    String first = requestCode(email);

    // Within the cooldown a second request answers the same but sends nothing.
    clearInvocations(mailer);
    postJson("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}")
        .andExpect(status().isOk());
    verify(mailer, never()).send(anyString(), anyString());

    db.update(
        "update password_reset_codes set expires_at=?, created_at=? where user_id=(select id from"
            + " app_users where email=?)",
        java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1),
        java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(11),
        email);
    postJson("/api/v1/auth/verify-reset-code", "{\"email\":\"" + email + "\",\"code\":\"" + first + "\"}")
        .andExpect(status().isBadRequest());

    // A new code retires the old one.
    String second = requestCode(email);
    verify(mailer, times(1)).send(eq(email), anyString());
    if (!second.equals(first))
      postJson(
              "/api/v1/auth/verify-reset-code",
              "{\"email\":\"" + email + "\",\"code\":\"" + first + "\"}")
          .andExpect(status().isBadRequest());
    postJson("/api/v1/auth/verify-reset-code", "{\"email\":\"" + email + "\",\"code\":\"" + second + "\"}")
        .andExpect(status().isOk());
  }
}
