package com.zoopass.user.dto;

import com.zoopass.common.validation.PlainText;
import com.zoopass.common.validation.ValidationPatterns;
import com.zoopass.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Request bodies and query parameters of the user endpoints. */
public final class UserRequests {

    private UserRequests() {
    }

    /** Profile fields a user (or an admin) may edit. Email and role are changed elsewhere. */
    public record UpdateProfile(
            @NotBlank(message = "Vui lòng nhập họ tên")
            @Size(max = 120, message = "Họ tên tối đa 120 ký tự")
            @PlainText
            String fullName,

            @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
            String phone,

            @Past(message = "Ngày sinh phải ở trong quá khứ")
            LocalDate dateOfBirth) {
    }

    public record ChangePassword(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MESSAGE)
            String newPassword) {
    }

    /** Admin creates an account directly (e.g. a staff member); it is verified immediately. */
    public record Create(
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

            @NotNull(message = "Vui lòng chọn vai trò")
            Role role) {
    }

    public record ChangeRole(@NotNull(message = "Vui lòng chọn vai trò") Role role) {
    }

    public record ChangeStatus(@NotNull(message = "Vui lòng chọn trạng thái") Boolean active) {
    }

    /**
     * Filters of the admin user list (all optional).
     *
     * @param q             text matched against name, email and phone
     * @param role          exact role
     * @param active        active (true) or deactivated (false)
     * @param emailVerified verified or not
     * @param createdFrom   created at or after
     * @param createdTo     created before
     */
    public record Search(String q, Role role, Boolean active, Boolean emailVerified,
                         Instant createdFrom, Instant createdTo) {
    }
}
