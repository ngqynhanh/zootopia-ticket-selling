package com.zoopass.user;

import com.zoopass.audit.AuditAction;
import com.zoopass.audit.AuditLogService;
import com.zoopass.auth.TokenPurpose;
import com.zoopass.auth.TokenService;
import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.exception.BusinessRuleException;
import com.zoopass.common.exception.ConflictException;
import com.zoopass.common.exception.FieldValidationException;
import com.zoopass.common.exception.ResourceNotFoundException;
import com.zoopass.user.dto.UserRequests;
import com.zoopass.user.dto.UserResponse;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Own-profile operations and the admin user management. */
@Service
public class UserService {

    private static final String USER = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLog;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       AuditLogService auditLog) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLog = auditLog;
    }

    // ----- own profile -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse updateProfile(Long id, UserRequests.UpdateProfile request) {
        User user = find(id);
        UserResponse before = UserResponse.from(user);
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setDateOfBirth(request.dateOfBirth());
        auditLog.record(AuditAction.PROFILE_UPDATED, USER, id, before, UserResponse.from(user));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long id, UserRequests.ChangePassword request) {
        User user = find(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("currentPassword", "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Other devices must log in again with the new password.
        tokenService.revokeAll(user, TokenPurpose.REFRESH);
        auditLog.record(AuditAction.PASSWORD_CHANGED, USER, id, null, null);
    }

    // ----- admin -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserRequests.Search search, Pageable pageable) {
        return PageResponse.of(userRepository.findAll(UserSpecifications.matching(search), pageable),
                UserResponse::from);
    }

    @Transactional
    public UserResponse create(UserRequests.Create request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email này đã được đăng ký");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(),
                request.role());
        user.setPhone(blankToNull(request.phone()));
        // The admin vouches for the address, so no verification email is needed.
        user.markEmailVerified();
        userRepository.save(user);
        auditLog.record(AuditAction.USER_CREATED, USER, user.getId(), null,
                Map.of("email", email, "role", request.role()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequests.UpdateProfile request) {
        User user = find(id);
        UserResponse before = UserResponse.from(user);
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setDateOfBirth(request.dateOfBirth());
        auditLog.record(AuditAction.USER_UPDATED, USER, id, before, UserResponse.from(user));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeRole(Long actorId, Long id, Role role) {
        if (actorId.equals(id)) {
            // Prevents an admin from accidentally removing the last admin access (their own).
            throw new BusinessRuleException("Bạn không thể thay đổi vai trò của chính mình");
        }
        User user = find(id);
        Role before = user.getRole();
        user.setRole(role);
        auditLog.record(AuditAction.USER_ROLE_CHANGED, USER, id, Map.of("role", before), Map.of("role", role));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeStatus(Long actorId, Long id, boolean active) {
        if (actorId.equals(id)) {
            throw new BusinessRuleException("Bạn không thể khóa tài khoản của chính mình");
        }
        User user = find(id);
        if (active) {
            user.restore();
            auditLog.record(AuditAction.USER_REACTIVATED, USER, id, null, null);
        } else {
            user.softDelete();
            tokenService.revokeAll(user, TokenPurpose.REFRESH);
            auditLog.record(AuditAction.USER_DEACTIVATED, USER, id, null, null);
        }
        return UserResponse.from(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("người dùng", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
