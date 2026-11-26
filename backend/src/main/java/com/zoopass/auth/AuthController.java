package com.zoopass.auth;

import com.zoopass.auth.dto.AuthRequests;
import com.zoopass.auth.dto.AuthResponse;
import com.zoopass.common.exception.ApiException;
import com.zoopass.common.exception.ErrorCode;
import com.zoopass.config.AppProperties;
import com.zoopass.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. The refresh token travels only in an HttpOnly cookie limited to
 * {@code /api/v1/auth}, so JavaScript (and therefore an XSS attack) cannot read it.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, tokens, password reset")
public class AuthController {

    public static final String REFRESH_COOKIE = "zoopass_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final AppProperties.Security security;
    private final List<String> allowedOrigins;

    public AuthController(AuthService authService, AppProperties properties) {
        this.authService = authService;
        this.security = properties.security();
        this.allowedOrigins = properties.cors().allowedOrigins();
    }

    @Operation(summary = "Check whether an email can still be registered (registration step 1)")
    @GetMapping("/email-available")
    public Map<String, Boolean> emailAvailable(
            @RequestParam @NotBlank @Email(message = "Email không đúng định dạng") String email) {
        return Map.of("available", authService.isEmailAvailable(email));
    }

    @Operation(summary = "Register a customer account and send the verification email")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody AuthRequests.Register request) {
        return authService.register(request);
    }

    @Operation(summary = "Verify the email address with the token from the email")
    @PostMapping("/verify-email")
    public UserResponse verifyEmail(@Valid @RequestBody AuthRequests.Token request) {
        return authService.verifyEmail(request.token());
    }

    @Operation(summary = "Send the verification email again (same answer whether the email exists or not)")
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody AuthRequests.EmailOnly request) {
        authService.resendVerification(request.email());
    }

    @Operation(summary = "Log in: returns an access token and sets the refresh-token cookie")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequests.Login body,
                                              HttpServletRequest request) {
        return withCookie(authService.login(body, request.getRemoteAddr()));
    }

    @Operation(summary = "Get a new access token using the refresh-token cookie (rotates the cookie)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest request) {
        checkOrigin(request);
        if (refreshToken == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại");
        }
        return withCookie(authService.refresh(refreshToken));
    }

    @Operation(summary = "Log out: revoke the refresh token and clear the cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest request) {
        checkOrigin(request);
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @Operation(summary = "Email a password-reset link (same answer whether the email exists or not)")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody AuthRequests.EmailOnly request) {
        authService.forgotPassword(request.email());
    }

    @Operation(summary = "Set a new password with the token from the reset email")
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody AuthRequests.ResetPassword request) {
        authService.resetPassword(request);
    }

    private ResponseEntity<AuthResponse> withCookie(AuthService.Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(session.refreshToken(), security.refreshTokenTtl()).toString())
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(security.refreshCookieSecure())
                .sameSite(security.refreshCookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    /**
     * CSRF defence for the two endpoints that rely on a cookie: a browser always sends the Origin
     * header on cross-site POSTs, so a request from another website is rejected here.
     */
    private void checkOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !allowedOrigins.contains(origin)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Nguồn yêu cầu không được phép");
        }
    }
}
