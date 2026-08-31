package com.bankapp.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The one place that sees every context's URL space, which is what "deny by
 * default" (NFR-11) requires: a rule that lives next to each controller can only
 * ever say what to allow, never what nobody allowed.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // No cookies and no session, so there is no ambient credential for a
            // cross-site form post to ride on — which is the only thing CSRF
            // protection defends. It comes back the day the refresh token
            // becomes a cookie (docs/07).
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                // TODO(NFR-11): temporary. Slices 1-4 are not authenticated yet;
                // that retrofit is its own story. Deleting these two lines is
                // what starts it.
                .requestMatchers("/api/accounts/**", "/api/payments/**").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}
