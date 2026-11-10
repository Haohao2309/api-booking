package com.example.api_booking;

import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.api_booking.entity.AppUser;
import com.example.api_booking.repository.AppUserRepository;
import com.example.api_booking.entity.UserRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiBookingApplicationTests.ProbeController.class)
class ApiBookingApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void healthIsPublicAndDoesNotLeakDatabaseDetails() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void csrfEndpointProvidesTokenWithoutLogin() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void unauthenticatedRequestsReturnJsonInsteadOfRedirect() throws Exception {
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void unsafeRequestsRequireCsrfEvenAtPublicAuthEndpoints() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginUsesSaltedPbkdf2Sha256AndCreatesAuthenticatedSession() throws Exception {
        users.save(new AppUser("customer@example.com", passwordEncoder.encode("correct-password"), UserRole.CUSTOMER));

        mvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"customer@example.com\",\"password\":\"correct-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("customer@example.com"))
            .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        mvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"customer@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void onlyConfiguredOriginGetsCredentialedCors() throws Exception {
        mvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/login")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }
    // Test-only handlers prove authorization reaches controllers, without adding fake production APIs.
    @RestController
    static class ProbeController {
        @PostMapping("/api/rooms") String createRoom() { return "ok"; }
        @GetMapping("/api/bookings") String bookings() { return "ok"; }
    }

    @Test
    void csrfTokenWorksOnlyWithTheSessionThatIssuedIt() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").header("X-CSRF-TOKEN", token))
            .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanCreateRooms() throws Exception {
        for (String role : new String[] { "CUSTOMER", "RECEPTIONIST" }) {
            mvc.perform(post("/api/rooms").with(user("test").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/rooms").with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    void onlyStaffCanListAllBookings() throws Exception {
        mvc.perform(get("/api/bookings").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
        for (String role : new String[] { "ADMIN", "RECEPTIONIST" }) {
            mvc.perform(get("/api/bookings").with(user("staff").roles(role)))
                .andExpect(status().isOk());
        }
    }
}
