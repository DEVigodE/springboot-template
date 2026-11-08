package com.template.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.template.auth.application.user.UserRepository;
import com.template.auth.infrastructure.security.JwtAuthenticationFilter;
import com.template.auth.presentation.shared.ApiPaths;
import com.template.auth.presentation.shared.security.JsonSecurityErrorHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless bearer-token security. CSRF is off because nothing here is
 * cookie-authenticated: the access token travels in the Authorization header
 * and the refresh cookie is SameSite=Strict and only ever read by the two
 * routes that rotate/revoke it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
            UserRepository userRepository, JsonSecurityErrorHandler errorHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> headers
                        // Explicit opt-out of a few sensitive browser APIs this JSON API has no reason to grant.
                        .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()")))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                ApiPaths.AUTH + "/register",
                                ApiPaths.AUTH + "/login",
                                ApiPaths.AUTH + "/refresh",
                                ApiPaths.AUTH + "/logout",
                                ApiPaths.AUTH + "/request-password-reset",
                                ApiPaths.AUTH + "/reset-password",
                                ApiPaths.AUTH + "/verify-email").permitAll()
                        .requestMatchers("/health", "/health/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus",
                                "/actuator/info").permitAll()
                        .requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // STOMP handshake; the token is verified on CONNECT (StompJwtChannelInterceptor).
                        .requestMatchers("/ws/**").permitAll()
                        // Only exists with app.queue.admin-enabled (dev profile).
                        .requestMatchers("/admin/queues").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtDecoder, userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SecretKey jwtSecretKey(AppProperties properties) {
        return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        // Explicit algorithm allowlist - closes off any algorithm-confusion angle.
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Explicit allowlists instead of open-by-omission defaults. With CORS_ORIGIN
     * unset ("*") credentials are off - browsers reject "*" combined with
     * credentials anyway, which is why a front-end on another origin must set
     * CORS_ORIGIN for the refresh-token cookie to work.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        if (properties.cors().allowsAny()) {
            config.setAllowedOrigins(List.of(CorsConfiguration.ALL));
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(properties.cors().allowedOrigins());
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        // Lets client-side JS read the request id (browsers hide custom response headers by default).
        config.setExposedHeaders(List.of("X-Request-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
