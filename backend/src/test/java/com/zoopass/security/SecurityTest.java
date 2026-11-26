package com.zoopass.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zoopass.support.IntegrationTest;
import com.zoopass.support.TestUsers;
import com.zoopass.user.Role;
import com.zoopass.user.User;
import com.zoopass.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Filter-chain behaviour: who gets through, error format, headers, CORS. */
@IntegrationTest
class SecurityTest {

    // Any path that is neither public nor mapped: anonymous -> 401, authenticated -> 404.
    private static final String PROTECTED_PATH = "/api/v1/protected-probe";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void anonymousRequestToProtectedPathGets401Json() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void validTokenAuthenticatesTheRequest() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, users.bearer(Role.CUSTOMER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = users.bearer(Role.ADMIN);
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserIsRejectedEvenWithAValidToken() throws Exception {
        User user = users.create(Role.CUSTOMER);
        String bearer = users.bearer(user);
        user.softDelete();
        userRepository.saveAndFlush(user);

        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void securityHeadersAreSent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString(
                        "frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void corsAllowsTheConfiguredFrontendOnly() throws Exception {
        mockMvc.perform(options("/api/v1/ticket-types")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/v1/ticket-types")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
