package com.zoopass.auth.dto;

import com.zoopass.common.validation.PlainText;
import com.zoopass.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request bodies of the authentication endpoints. */
public final class AuthRequests {

    private AuthRequests() {
    }

    /**
     * Registration. The frontend collects these fields in three steps (credentials, personal
     * information, then email verification) and submits them together.
     */
    public record Register(
            @NotBlank(message = "Vui lòng nhập email")
            @Email(message = "Email không đúng định dạng")
            @Size(max = 255, message = "Email tối đa 255 ký tự")
            String email,

            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "Vui lòng nhập họ tên")
            @Size(max = 120, message = "Họ tên tối đa 120 ký tự")
            @PlainText
            String fullName,

            @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
            String phone,

            @Past(message = "Ngày sinh phải ở trong quá khứ")
            LocalDate dateOfBirth) {
    }

    public record Login(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không đúng định dạng") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    /** A token received by email (verification or password reset). */
    public record Token(@NotBlank(message = "Thiếu mã xác thực") String token) {
    }

    public record EmailOnly(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không đúng định dạng") String email) {
    }

    public record ResetPassword(
            @NotBlank(message = "Thiếu mã xác thực") String token,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MESSAGE)
            String newPassword) {
    }
}
