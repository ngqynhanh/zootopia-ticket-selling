package com.zoopass.auth;

import com.zoopass.audit.AuditAction;
import com.zoopass.audit.AuditLogService;
import com.zoopass.auth.dto.AuthRequests;
import com.zoopass.auth.dto.AuthResponse;
import com.zoopass.common.exception.ApiException;
import com.zoopass.common.exception.ConflictException;
import com.zoopass.common.exception.ErrorCode;
import com.zoopass.config.AppProperties;
import com.zoopass.notification.EmailService;
import com.zoopass.security.JwtService;
import com.zoopass.user.Role;
import com.zoopass.user.User;
import com.zoopass.user.UserRepository;
import com.zoopass.user.dto.UserResponse;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, email verification, login, token refresh, logout and password reset.
 *
 * <p>Endpoints that take an email address and may send mail (resend verification, forgot password)
 * always answer the same way, whether or not the account exists, so they cannot be used to discover
 * which emails are registered.
 */
@Service
public class AuthService {

    private static final String USER = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttempts;
    private final EmailService emailService;
    private final AuditLogService auditLog;
    private final String frontendUrl;
    /** Compared against when the email is unknown, so both cases take the same time. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       TokenService tokenService, LoginAttemptService loginAttempts, EmailService emailService,
                       AuditLogService auditLog, AppProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
        this.emailService = emailService;
        this.auditLog = auditLog;
        this.frontendUrl = properties.frontendUrl();
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    /** Result of login/refresh: the JSON body plus the new refresh token for the cookie. */
    public record Session(AuthResponse response, String refreshToken) {
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(User.normalizeEmail(email));
    }

    @Transactional
    public UserResponse register(AuthRequests.Register request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email này đã được đăng ký");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(),
                Role.CUSTOMER);
        user.setPhone(blankToNull(request.phone()));
        user.setDateOfBirth(request.dateOfBirth());
        userRepository.save(user);
        sendVerificationEmail(user);
        // actor_id stays NULL: the new user row is not committed yet when the audit entry is written.
        auditLog.recordFor(null, email, AuditAction.USER_REGISTERED, USER, user.getId(), Map.of("email", email));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse verifyEmail(String token) {
        User user = tokenService.consume(token, TokenPurpose.EMAIL_VERIFICATION);
        user.markEmailVerified();
        auditLog.recordFor(user.getId(), user.getEmail(), AuditAction.EMAIL_VERIFIED, USER, user.getId(), null);
        return UserResponse.from(user);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(User.normalizeEmail(email))
                .filter(user -> user.isActive() && !user.isEmailVerified())
                .ifPresent(user -> {
                    tokenService.revokeAll(user, TokenPurpose.EMAIL_VERIFICATION);
                    sendVerificationEmail(user);
                });
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session login(AuthRequests.Login request, String ip) {
        String email = User.normalizeEmail(request.email());
        loginAttempts.checkAllowed(email, ip);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        if (!passwordEncoder.matches(request.password(), hash) || user == null) {
            loginAttempts.recordFailure(email, ip);
            auditLog.recordFor(user == null ? null : user.getId(), email, AuditAction.LOGIN_FAILED, USER,
                    user == null ? null : user.getId(), null);
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email chưa được xác thực. Vui lòng kiểm tra hộp thư hoặc gửi lại email xác thực");
        }
        loginAttempts.reset(email, ip);
        user.recordLogin();
        auditLog.recordFor(user.getId(), email, AuditAction.LOGIN_SUCCEEDED, USER, user.getId(), null);
        return newSession(user);
    }

    /** Exchanges a valid refresh token for a new access token and a new (rotated) refresh token. */
    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String refreshToken) {
        User user = tokenService.rotateRefreshToken(refreshToken);
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị khóa");
        }
        return newSession(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(User.normalizeEmail(email))
                .filter(User::isActive)
                .ifPresent(user -> {
                    tokenService.revokeAll(user, TokenPurpose.PASSWORD_RESET);
                    String token = tokenService.create(user, TokenPurpose.PASSWORD_RESET);
                    emailService.send(user.getEmail(), "ZooPass - Đặt lại mật khẩu",
                            "Xin chào " + user.getFullName() + ",\n\n"
                                    + "Nhấn vào liên kết sau để đặt lại mật khẩu (hiệu lực "
                                    + tokenService.ttl(TokenPurpose.PASSWORD_RESET).toMinutes() + " phút):\n"
                                    + frontendUrl + "/reset-password?token=" + token + "\n\n"
                                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");
                });
    }

    @Transactional
    public void resetPassword(AuthRequests.ResetPassword request) {
        User user = tokenService.consume(request.token(), TokenPurpose.PASSWORD_RESET);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Log out every device: a reset usually means the old password may be known to someone else.
        tokenService.revokeAll(user, TokenPurpose.REFRESH);
        auditLog.recordFor(user.getId(), user.getEmail(), AuditAction.PASSWORD_RESET, USER, user.getId(), null);
    }

    private Session newSession(User user) {
        String refreshToken = tokenService.create(user, TokenPurpose.REFRESH);
        AuthResponse response = new AuthResponse(jwtService.issueAccessToken(user), "Bearer",
                jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
        return new Session(response, refreshToken);
    }

    private void sendVerificationEmail(User user) {
        String token = tokenService.create(user, TokenPurpose.EMAIL_VERIFICATION);
        emailService.send(user.getEmail(), "ZooPass - Xác thực email",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Cảm ơn bạn đã đăng ký ZooPass. Nhấn vào liên kết sau để xác thực email (hiệu lực "
                        + tokenService.ttl(TokenPurpose.EMAIL_VERIFICATION).toHours() + " giờ):\n"
                        + frontendUrl + "/verify-email?token=" + token);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
