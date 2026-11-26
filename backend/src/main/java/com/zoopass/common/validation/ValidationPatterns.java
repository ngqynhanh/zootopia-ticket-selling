package com.zoopass.common.validation;

/** Regular expressions and messages shared by request DTOs. */
public final class ValidationPatterns {

    /** 8-64 characters with at least one lowercase letter, one uppercase letter and one digit. */
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE =
            "Mật khẩu phải có 8–64 ký tự, gồm ít nhất một chữ hoa, một chữ thường và một chữ số";

    /** Vietnamese phone number (0xxxxxxxxx or +84xxxxxxxxx); empty is allowed for optional fields. */
    public static final String PHONE = "^$|^(0|\\+84)\\d{9}$";
    public static final String PHONE_MESSAGE = "Số điện thoại không hợp lệ (ví dụ: 0901234567)";

    private ValidationPatterns() {
    }
}
