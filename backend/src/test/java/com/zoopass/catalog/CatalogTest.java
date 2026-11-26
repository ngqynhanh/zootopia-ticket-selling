package com.zoopass.catalog;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.zoopass.user.Role;
import com.zoopass.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Public catalog search and manager CRUD for ticket types and combos. */
@IntegrationTest
class CatalogTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private String managerBearer;

    @BeforeEach
    void setUp() {
        managerBearer = new TestUsers(userRepository, passwordEncoder, jwtService).bearer(Role.MANAGER);
    }

    // ----- public search -----------------------------------------------------------------------

    @Test
    void guestsSeeReferenceTicketTypesInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("GATE_ADULT"))
                .andExpect(jsonPath("$.content[0].price").value(60000.0))
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(13)));
    }

    @Test
    void filtersByCategoryAndVisitorHeight() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types").param("category", "GATE").param("heightCm", "110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("GATE_CHILD"));

        mockMvc.perform(get("/api/v1/ticket-types").param("category", "TOUR").param("minPrice", "160000"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("TOUR_VIP"))
                .andExpect(jsonPath("$.content[0].minQuantity").value(6));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types").param("category", "BOAT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));
        mockMvc.perform(get("/api/v1/ticket-types").param("minPrice", "100000").param("maxPrice", "50000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("minPrice"));
        mockMvc.perform(get("/api/v1/combos").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void combosArePaginatedWithStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.from").value(6))
                .andExpect(jsonPath("$.to").value(10))
                .andExpect(jsonPath("$.content[0].code").value("COMBO_06"));
    }

    @Test
    void combosFilterByGroupSizeAgeAndSortByPrice() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("groupSize", "45").param("age", "5")
                        .param("sort", "price:desc"))
                .andExpect(status().isOk())
                // Only combos accepting 45 people and 5-year-olds, most expensive first
                .andExpect(jsonPath("$.content[*].code", hasItem("COMBO_01")))
                .andExpect(jsonPath("$.content[*].code", not(hasItem("COMBO_04"))))
                .andExpect(jsonPath("$.content[0].code").value("COMBO_01"));
    }

    // ----- manager CRUD ------------------------------------------------------------------------

    @Test
    void managerCreatesUpdatesDeletesAndRestoresATicketType() throws Exception {
        String code = uniqueCode();
        String location = mockMvc.perform(post("/api/v1/ticket-types").header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketType(code, 45000, null, null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        // Duplicate code (different case) -> 409
        mockMvc.perform(post("/api/v1/ticket-types").header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketType(code.toLowerCase(), 1, null, null)))
                .andExpect(status().isConflict());

        mockMvc.perform(put(location).header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketType(code, 50000, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(50000));

        mockMvc.perform(delete(location).header(HttpHeaders.AUTHORIZATION, managerBearer))
                .andExpect(status().isNoContent());
        // Guests no longer see it; managers still do, marked inactive
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
        mockMvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, managerBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/v1/ticket-types").param("q", code))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/ticket-types").param("q", code).param("active", "false")
                        .header(HttpHeaders.AUTHORIZATION, managerBearer))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch(location + "/restore").header(HttpHeaders.AUTHORIZATION, managerBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(get(location)).andExpect(status().isOk());
    }

    @Test
    void ticketTypeValidationIncludesCrossFieldRules() throws Exception {
        mockMvc.perform(post("/api/v1/ticket-types").header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketType("bad code!", -5, 130, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("code")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("heightRange")));
    }

    @Test
    void onlyManagersAndAdminsChangeTheCatalog() throws Exception {
        String body = ticketType(uniqueCode(), 1000, null, null);
        mockMvc.perform(post("/api/v1/ticket-types").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        for (Role role : new Role[] {Role.CUSTOMER, Role.STAFF}) {
            String bearer = new TestUsers(userRepository, passwordEncoder, jwtService).bearer(role);
            mockMvc.perform(post("/api/v1/ticket-types").header(HttpHeaders.AUTHORIZATION, bearer)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        String adminBearer = new TestUsers(userRepository, passwordEncoder, jwtService).bearer(Role.ADMIN);
        mockMvc.perform(post("/api/v1/ticket-types").header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void managerCreatesAndDeletesACombo() throws Exception {
        String code = uniqueCode();
        String body = """
                {"code":"%s","name":"Combo thử nghiệm","description":"Dùng cho kiểm thử","price":99000,
                 "durationMinutes":60,"minParticipants":2,"maxParticipants":10,"minAge":6,
                 "imageUrl":"https://example.com/combo.jpg"}
                """.formatted(code);
        String location = mockMvc.perform(post("/api/v1/combos").header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(delete(location).header(HttpHeaders.AUTHORIZATION, managerBearer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/combos").header(HttpHeaders.AUTHORIZATION, managerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(code, uniqueCode()).replace("\"maxParticipants\":10",
                                "\"maxParticipants\":1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("participantRange"));
    }

    @Test
    void publicSettingsHideInternalOnes() throws Exception {
        mockMvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['group.min_size']").value("10"))
                .andExpect(jsonPath("$['ticket.sales_cutoff_time']").value("17:30"))
                .andExpect(jsonPath("$['order.payment_timeout_minutes']").doesNotExist());
    }

    private static String uniqueCode() {
        return "TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** SERVICE category with a high display order so test data never changes the public price page order. */
    private static String ticketType(String code, int price, Integer minHeight, Integer maxHeight) {
        return """
                {"code":"%s","name":"Vé thử nghiệm","category":"SERVICE","description":"Kiểm thử","price":%d,
                 "minHeightCm":%s,"maxHeightCm":%s,"minQuantity":1,"entriesPerUnit":1,"validityDays":30,
                 "displayOrder":999}
                """.formatted(code, price, minHeight, maxHeight);
    }
}
