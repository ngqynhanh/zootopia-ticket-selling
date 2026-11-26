package com.zoopass.common.exception;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zoopass.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Errors come back in the shared {@link ApiError} JSON shape.
 * Requests are authenticated with {@code with(user(...))} so they pass the security filter
 * and reach Spring MVC, which raises the 404.
 */
@IntegrationTest
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownPathReturnsStructuredJson404() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(user("tester"))
                        .header("X-Request-Id", "test-request-0001"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "test-request-0001"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"))
                .andExpect(jsonPath("$.requestId").value("test-request-0001"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                // test profile behaves like prod: no exception detail
                .andExpect(jsonPath("$.debug").doesNotExist());
    }

    @Test
    void unsafeRequestIdIsReplaced() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(user("tester"))
                        .header("X-Request-Id", "bad id\nINJECTED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.requestId").value(matchesPattern("[0-9a-f-]{36}")));
    }
}
