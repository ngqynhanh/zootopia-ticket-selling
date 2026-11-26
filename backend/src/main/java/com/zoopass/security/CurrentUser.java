package com.zoopass.security;

import com.zoopass.user.Role;
import com.zoopass.user.User;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The authenticated user stored in the SecurityContext for one request.
 * Controllers receive it with {@code @AuthenticationPrincipal CurrentUser user}.
 *
 * @param id    user id
 * @param email login email
 * @param role  role, exposed to Spring Security as authority {@code ROLE_<role>}
 */
public record CurrentUser(Long id, String email, Role role) {

    public static CurrentUser from(User user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getRole());
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean hasAnyRole(Role... roles) {
        for (Role candidate : roles) {
            if (candidate == role) {
                return true;
            }
        }
        return false;
    }
}
