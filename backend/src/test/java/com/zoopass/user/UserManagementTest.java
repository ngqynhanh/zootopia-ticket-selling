package com.zoopass.user;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zoopass.security.JwtService;
import com.zoopass.support.IntegrationTest;
import com.zoopass.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Own profile, admin user management and role checks. */
@IntegrationTest
class UserManagementTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private TestUsers users;
    private User admin;
    private String adminBearer;

    @BeforeEach
    void setUp() {
        users = new TestUsers(userRepository, passwordEncoder, jwtService);
        admin = users.create(Role.ADMIN);
        adminBearer = users.bearer(admin);
    }

    @Test
    void userReadsAndUpdatesOwnProfile() throws Exception {
        User customer = users.create(Role.CUSTOMER);
        String bearer = users.bearer(customer);

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.getEmail()));

        mockMvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Trần Thị Bình\",\"phone\":\"0912345678\",\"dateOfBirth\":\"2001-02-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Trần Thị Bình"))
                .andExpect(jsonPath("$.phone").value("0912345678"));
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() throws Exception {
        String bearer = users.bearer(Role.CUSTOMER);

        mockMvc.perform(put("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Wrong1234\",\"newPassword\":\"Better123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));

        mockMvc.perform(put("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TestUsers.PASSWORD + "\",\"newPassword\":\"Better123\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void onlyAdminsManageUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        for (Role role : new Role[] {Role.CUSTOMER, Role.STAFF, Role.MANAGER}) {
            mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, users.bearer(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isOk());
    }

    @Test
    void adminSearchesWithFiltersSortingAndPaging() throws Exception {
        users.create(Role.STAFF);
        users.create(Role.STAFF);

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .param("role", "STAFF").param("active", "true").param("q", "staff-")
                        .param("sort", "createdAt:desc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[*].role", everyItem(is("STAFF"))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.from").value(1))
                .andExpect(jsonPath("$.to").value(1));

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .param("sort", "passwordHash:asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void adminCreatesUserAndChangesRole() throws Exception {
        String email = TestUsers.uniqueEmail("newstaff");
        String location = mockMvc.perform(post("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Staff1234\",\"fullName\":\"Lê Văn Cường\","
                                + "\"role\":\"STAFF\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(patch(location + "/role").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(get("/api/v1/audit-logs").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .param("action", "USER_ROLE_CHANGED").param("actorId", admin.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].oldValue.role").value("STAFF"))
                .andExpect(jsonPath("$.content[0].newValue.role").value("MANAGER"));
    }

    @Test
    void adminCannotDemoteOrDeactivateThemselves() throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + admin.getId() + "/role").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        mockMvc.perform(patch("/api/v1/users/" + admin.getId() + "/status").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deactivatedUserCannotLogInOrUseTheirToken() throws Exception {
        User customer = users.create(Role.CUSTOMER);
        String customerBearer = users.bearer(customer);

        mockMvc.perform(patch("/api/v1/users/" + customer.getId() + "/status").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, customerBearer))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + customer.getEmail() + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        mockMvc.perform(patch("/api/v1/users/" + customer.getId() + "/status").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unknownUserIs404() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999999").header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
