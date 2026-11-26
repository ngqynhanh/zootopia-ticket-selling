package com.zoopass.user;

import com.zoopass.common.dto.PageResponse;
import com.zoopass.common.web.PageRequests;
import com.zoopass.security.CurrentUser;
import com.zoopass.user.dto.UserRequests;
import com.zoopass.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /users/me}: any logged-in user manages their own profile.
 * Everything else: admin-only user management.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Own profile and admin user management")
public class UserController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "fullName", "email", "role", "lastLoginAt");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Current user's profile")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return userService.get(currentUser.id());
    }

    @Operation(summary = "Update own name, phone and date of birth")
    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal CurrentUser currentUser,
                                 @Valid @RequestBody UserRequests.UpdateProfile request) {
        return userService.updateProfile(currentUser.id(), request);
    }

    @Operation(summary = "Change own password (current password required); other sessions are logged out")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                               @Valid @RequestBody UserRequests.ChangePassword request) {
        userService.changePassword(currentUser.id(), request);
    }

    @Operation(summary = "Search users (admin). Sort example: sort=createdAt:desc,fullName:asc")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> search(UserRequests.Search search,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) List<String> sort) {
        return userService.search(search,
                PageRequests.of(page, size, sort, SORT_FIELDS, Sort.by(Sort.Order.desc("createdAt"))));
    }

    @Operation(summary = "Get a user (admin)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Create a user with any role (admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequests.Create request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @Operation(summary = "Edit a user's profile (admin)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequests.UpdateProfile request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Change a user's role (admin, not on yourself)")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id,
                                   @Valid @RequestBody UserRequests.ChangeRole request) {
        return userService.changeRole(currentUser.id(), id, request.role());
    }

    @Operation(summary = "Deactivate (soft delete) or reactivate a user (admin, not yourself)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeStatus(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id,
                                     @Valid @RequestBody UserRequests.ChangeStatus request) {
        return userService.changeStatus(currentUser.id(), id, request.active());
    }
}
