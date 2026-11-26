package com.zoopass.security;

import com.zoopass.user.User;
import com.zoopass.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies it and puts a {@link CurrentUser} in the
 * SecurityContext.
 *
 * <p>The user is reloaded from the database on every request, so a deactivated account or a changed
 * role takes effect immediately instead of when the token expires. A missing or invalid token leaves
 * the request anonymous; protected endpoints then answer 401.
 *
 * <p>Not a {@code @Component}: it is created in SecurityConfig so it runs only inside the security
 * filter chain (a component would also be registered a second time as a plain servlet filter).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String MDC_USER = "user";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parseUserId(header.substring(BEARER.length()))
                    .flatMap(userRepository::findById)
                    .filter(User::isActive)
                    .ifPresent(user -> authenticate(user, request));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER);
        }
    }

    private void authenticate(User user, HttpServletRequest request) {
        CurrentUser principal = CurrentUser.from(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Every log line of this request now shows who made it.
        MDC.put(MDC_USER, user.getEmail());
    }
}
