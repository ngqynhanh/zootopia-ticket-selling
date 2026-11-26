package com.zoopass.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zoopass.catalog.TicketCategory;
import com.zoopass.common.validation.PlainText;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request bodies and search filters of the catalog endpoints. */
public final class CatalogRequests {

    private static final String CODE = "^[A-Za-z0-9_]{2,40}$";
    private static final String CODE_MESSAGE = "Mã gồm 2–40 ký tự chữ, số hoặc dấu gạch dưới";

    private CatalogRequests() {
    }

    /** Create or replace a ticket type. */
    public record TicketTypeRequest(
            @NotBlank(message = "Vui lòng nhập mã") @Pattern(regexp = CODE, message = CODE_MESSAGE)
            String code,

            @NotBlank(message = "Vui lòng nhập tên") @Size(max = 120, message = "Tên tối đa 120 ký tự") @PlainText
            String name,

            @NotNull(message = "Vui lòng chọn nhóm vé")
            TicketCategory category,

            @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") @PlainText
            String description,

            @NotNull(message = "Vui lòng nhập giá")
            @DecimalMin(value = "0", message = "Giá không được âm")
            @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
            BigDecimal price,

            @Min(value = 0, message = "Chiều cao không được âm") @Max(value = 250, message = "Chiều cao tối đa 250 cm")
            Integer minHeightCm,

            @Min(value = 1, message = "Chiều cao phải lớn hơn 0") @Max(value = 250, message = "Chiều cao tối đa 250 cm")
            Integer maxHeightCm,

            @NotNull(message = "Vui lòng nhập số lượng tối thiểu")
            @Min(value = 1, message = "Số lượng tối thiểu là 1") @Max(value = 100, message = "Tối đa 100")
            Integer minQuantity,

            @NotNull(message = "Vui lòng nhập số lượt vào")
            @Min(value = 1, message = "Ít nhất 1 lượt") @Max(value = 100, message = "Tối đa 100 lượt")
            Integer entriesPerUnit,

            @NotNull(message = "Vui lòng nhập số ngày hiệu lực")
            @Min(value = 1, message = "Ít nhất 1 ngày") @Max(value = 366, message = "Tối đa 366 ngày")
            Integer validityDays,

            @Min(value = 0, message = "Thứ tự không được âm") @Max(value = 1000, message = "Tối đa 1000")
            Integer displayOrder) {

        /** Cross-field rule; reported as field "heightRange". */
        @JsonIgnore
        @AssertTrue(message = "Chiều cao tối đa phải lớn hơn chiều cao tối thiểu")
        public boolean isHeightRange() {
            return minHeightCm == null || maxHeightCm == null || maxHeightCm > minHeightCm;
        }
    }

    /** Create or replace an experience combo. */
    public record ComboRequest(
            @NotBlank(message = "Vui lòng nhập mã") @Pattern(regexp = CODE, message = CODE_MESSAGE)
            String code,

            @NotBlank(message = "Vui lòng nhập tên") @Size(max = 150, message = "Tên tối đa 150 ký tự") @PlainText
            String name,

            @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") @PlainText
            String description,

            @NotNull(message = "Vui lòng nhập giá")
            @DecimalMin(value = "0", message = "Giá không được âm")
            @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
            BigDecimal price,

            @NotNull(message = "Vui lòng nhập thời lượng")
            @Min(value = 15, message = "Thời lượng tối thiểu 15 phút") @Max(value = 720, message = "Tối đa 720 phút")
            Integer durationMinutes,

            @NotNull(message = "Vui lòng nhập số người tối thiểu")
            @Min(value = 1, message = "Ít nhất 1 người") @Max(value = 500, message = "Tối đa 500 người")
            Integer minParticipants,

            @Min(value = 1, message = "Ít nhất 1 người") @Max(value = 500, message = "Tối đa 500 người")
            Integer maxParticipants,

            @Min(value = 0, message = "Tuổi không được âm") @Max(value = 100, message = "Tuổi tối đa 100")
            Integer minAge,

            @Size(max = 500, message = "Đường dẫn tối đa 500 ký tự")
            @Pattern(regexp = "^$|^https?://\\S+$", message = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://")
            String imageUrl) {

        @JsonIgnore
        @AssertTrue(message = "Số người tối đa phải lớn hơn hoặc bằng số người tối thiểu")
        public boolean isParticipantRange() {
            return maxParticipants == null || minParticipants == null || maxParticipants >= minParticipants;
        }
    }

    /**
     * Ticket-type filters (all optional).
     *
     * @param q        text in code or name
     * @param active   managers only; guests always see active types
     * @param heightCm only types a visitor of this height may use
     */
    public record TicketTypeSearch(String q, TicketCategory category, Boolean active, BigDecimal minPrice,
                                   BigDecimal maxPrice, Integer heightCm) {
    }

    /**
     * Combo filters (all optional).
     *
     * @param maxDuration combos lasting at most this many minutes
     * @param groupSize   combos that accept a group of this size
     * @param age         combos allowed for a visitor of this age
     */
    public record ComboSearch(String q, Boolean active, BigDecimal minPrice, BigDecimal maxPrice,
                              Integer maxDuration, Integer groupSize, Integer age) {
    }
}
