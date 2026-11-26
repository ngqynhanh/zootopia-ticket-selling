package com.zoopass.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void computesShowingFromTo() {
        var page = new PageImpl<>(List.of(21, 22, 23), PageRequest.of(1, 20), 156);

        PageResponse<String> response = PageResponse.of(page, String::valueOf);

        assertThat(response.from()).isEqualTo(21);
        assertThat(response.to()).isEqualTo(23);
        assertThat(response.totalElements()).isEqualTo(156);
        assertThat(response.totalPages()).isEqualTo(8);
        assertThat(response.content()).containsExactly("21", "22", "23");
    }

    @Test
    void emptyPageHasZeroRange() {
        var page = new PageImpl<Integer>(List.of(), PageRequest.of(0, 10), 0);

        PageResponse<Integer> response = PageResponse.of(page, i -> i);

        assertThat(response.from()).isZero();
        assertThat(response.to()).isZero();
    }
}
