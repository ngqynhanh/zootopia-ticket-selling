package com.zoopass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopass.support.CapturingEmailService;
import com.zoopass.support.IntegrationTest;
import com.zoopass.support.TestUsers;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** End-to-end authentication flows through the real API, database and security chain. */
@IntegrationTest
class AuthFlowTest {

    private static final String PASSWORD = "Zoo12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CapturingEmailService mail;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerVerifyLoginRefreshAndLogout() throws Exception {
        String email = TestUsers.uniqueEmail("flow");

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, PASSWORD, "Nguyễn Văn An")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Not verified yet
        login(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        String verificationToken = mail.latestToken(email).orElseThrow();
        mockMvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", verificationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
        // A verification link works only once
        mockMvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", verificationToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        MvcResult loginResult = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andReturn();
        String accessToken = body(loginResult).get("accessToken").asText();
        Cookie firstRefresh = loginResult.getResponse().getCookie(AuthController.REFRESH_COOKIE);

        // The access token authenticates requests (unknown protected path -> 404, not 401)
        mockMvc.perform(get("/api/v1/protected-probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        // Refresh rotates the cookie
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie secondRefresh = refreshResult.getResponse().getCookie(AuthController.REFRESH_COOKIE);
        assertThat(secondRefresh.getValue()).isNotEqualTo(firstRefresh.getValue());

        // Logout revokes the current refresh token and clears the cookie
        mockMvc.perform(post("/api/v1/auth/logout").cookie(secondRefresh))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(secondRefresh))
                .andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForList("SELECT action FROM audit_logs WHERE actor_email = ?", String.class, email))
                .contains("USER_REGISTERED", "EMAIL_VERIFIED", "LOGIN_SUCCEEDED");
    }

    @Test
    void reusingARotatedRefreshTokenRevokesTheWholeSession() throws Exception {
        String email = verifiedUser("reuse");
        Cookie first = login(email, PASSWORD).andReturn().getResponse().getCookie(AuthController.REFRESH_COOKIE);
        Cookie second = mockMvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andReturn().getResponse().getCookie(AuthController.REFRESH_COOKIE);

        // An attacker replays the old token: rejected, and the legitimate new token is revoked too.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshFromAForeignOriginIsRejected() throws Exception {
        String email = verifiedUser("origin");
        Cookie refresh = login(email, PASSWORD).andReturn().getResponse().getCookie(AuthController.REFRESH_COOKIE);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh).header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        String email = verifiedUser("dup");

        mockMvc.perform(get("/api/v1/auth/email-available").param("email", email.toUpperCase()))
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), PASSWORD, "Someone Else")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void registrationValidatesEveryField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"12345","fullName":"<script>x</script>",
                                 "phone":"123","dateOfBirth":"2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("fullName")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("phone")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("dateOfBirth")));
    }

    @Test
    void wrongPasswordIsLimitedToFiveAttempts() throws Exception {
        String email = verifiedUser("brute");
        for (int i = 0; i < 5; i++) {
            login(email, "Wrong1234").andExpect(status().isUnauthorized());
        }
        login(email, "Wrong1234")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
        // Even the right password is refused while locked
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameAnswer() throws Exception {
        login(TestUsers.uniqueEmail("nobody"), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void forgotAndResetPassword() throws Exception {
        String email = verifiedUser("reset");
        Cookie oldSession = login(email, PASSWORD).andReturn().getResponse().getCookie(AuthController.REFRESH_COOKIE);

        // Unknown emails get the same 202 and no email is sent
        String unknown = TestUsers.uniqueEmail("unknown");
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", unknown)))
                .andExpect(status().isAccepted());
        assertThat(mail.to(unknown)).isEmpty();

        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email)))
                .andExpect(status().isAccepted());
        String resetToken = mail.latestToken(email).orElseThrow();

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "NewPass123").andExpect(status().isOk());
        // Sessions opened before the reset are revoked
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldSession)).andExpect(status().isUnauthorized());
    }

    /** Registers through the API and verifies the email, like a real user. */
    private String verifiedUser(String prefix) throws Exception {
        String email = TestUsers.uniqueEmail(prefix);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, PASSWORD, "Test User")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", mail.latestToken(email).orElseThrow())))
                .andExpect(status().isOk());
        return email;
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private static String registerBody(String email, String password, String fullName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"" + fullName
                + "\",\"phone\":\"0901234567\",\"dateOfBirth\":\"1999-05-20\"}";
    }

    private static String json(String field, String value) {
        return "{\"" + field + "\":\"" + value + "\"}";
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
