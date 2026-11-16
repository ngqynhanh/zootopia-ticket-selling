package com.zoopass.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zoopass.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Public catalog endpoints, plus the shape of error responses produced by the
 * GlobalExceptionHandler.
 */
@IntegrationTest
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAllActiveTicketTypesInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$[0].code").value("GATE_ADULT"))
                .andExpect(jsonPath("$[0].price").value(60000.0))
                .andExpect(jsonPath("$[0].minHeightCm").value(130));
    }

    @Test
    void filtersTicketTypesByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types").param("category", "TOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].minQuantity").value(6))
                .andExpect(jsonPath("$[1].code").value("TOUR_VIP"));
    }

    @Test
    void rejectsUnknownCategoryWithValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types").param("category", "BOAT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("category"));
    }

    @Test
    void returnsStructuredNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/ticket-types/999999").header("X-Request-Id", "test-request-0001"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "test-request-0001"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/ticket-types/999999"))
                .andExpect(jsonPath("$.requestId").value("test-request-0001"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.debug").doesNotExist());
    }

    @Test
    void paginatesCombosWithResultStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.from").value(6))
                .andExpect(jsonPath("$.to").value(10))
                .andExpect(jsonPath("$.content[0].code").value("COMBO_06"));
    }

    @Test
    void sortsCombosByPriceDescending() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("sortBy", "price").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("COMBO_10"));
    }

    @Test
    void rejectsInvalidPagingAndSortParameters() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("size", "500").param("sortBy", "passwordHash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void rejectsNonNumericPage() throws Exception {
        mockMvc.perform(get("/api/v1/combos").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void unknownPathReturnsJson404() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete("/api/v1/ticket-types"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void exposesPublicSettingsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['group.min_size']").value("10"))
                .andExpect(jsonPath("$['ticket.sales_cutoff_time']").value("17:30"))
                .andExpect(jsonPath("$['order.payment_timeout_minutes']").doesNotExist());
    }
}
