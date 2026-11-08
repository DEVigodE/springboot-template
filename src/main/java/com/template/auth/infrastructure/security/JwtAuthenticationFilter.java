package com.template.auth.infrastructure.security;

import com.template.auth.application.user.UserRepository;
import com.template.auth.domain.shared.exception.UserInactiveException;
import com.template.auth.domain.user.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer-token authentication. The user is re-loaded from the DB on every
 * request rather than trusted from the token's claims: a deactivated/deleted
 * account or a changed role set takes effect immediately, not at token expiry.
 * An invalid token simply leaves the request anonymous - the entry point then
 * answers 401 for protected routes, while public routes keep working.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Jwt jwt = jwtDecoder.decode(token);
                UUID userId = UUID.fromString(jwt.getSubject());
                UserEntity user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    user.verifyActive();
                    SecurityContextHolder.getContext()
                            .setAuthentication(AuthenticatedUserFactory.authenticationFor(user));
                }
            } catch (JwtException | IllegalArgumentException | UserInactiveException e) {
                log.debug("Rejected bearer token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
