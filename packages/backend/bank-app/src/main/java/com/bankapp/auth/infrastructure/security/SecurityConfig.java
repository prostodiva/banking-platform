package com.bankapp.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
    SecurityFilterChain filterChain(
        HttpSecurity http,
        ProblemDetailAuthenticationEntryPoint entryPoint
    ) throws Exception {
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
                // MUST be above the /api/auth/** line. Matchers are evaluated in
                // order and the first match wins; below it, this endpoint is
                // public and almost nothing warns you. The controller throws an
                // NPE on the null @AuthenticationPrincipal, that 500 is forwarded
                // to /error, and /error falls through to anyRequest().authenticated()
                // — which answers 401 through the same entry point. The status is
                // right by accident, and only LogoutE2ETest.authenticatesBeforeIt-
                // Validates can tell the difference.
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                // TODO(NFR-11): still open. Deleting these two lines starts the
                // retrofit.
                .requestMatchers("/api/accounts/**", "/api/payments/**").permitAll()
                .anyRequest().authenticated()
            )
            // Both entry points on purpose: this one answers a token that was
            // present and bad...
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(entryPoint)
            )
            // ...and this one a request that carried no token at all. Setting only
            // the first leaves the no-token case on Spring Security's empty body.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
            .build();
    }
}
