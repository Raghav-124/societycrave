package com.oursociety.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(c -> {}).csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .authorizeHttpRequests(a -> a.requestMatchers("/", "/error", "/oauth2/**", "/login/**", "/api/societies/search").permitAll().anyRequest().authenticated())
            .oauth2Login(o -> o
                .successHandler(oauthSuccessHandler)
                .failureHandler(oauthFailureHandler))
            .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((request, response, auth) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String origin) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(origin)); c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", c); return source;
    }

    private final org.springframework.security.web.authentication.AuthenticationSuccessHandler oauthSuccessHandler;
    private final org.springframework.security.web.authentication.AuthenticationFailureHandler oauthFailureHandler;

    public SecurityConfig(
        org.springframework.security.web.authentication.AuthenticationSuccessHandler oauthSuccessHandler,
        org.springframework.security.web.authentication.AuthenticationFailureHandler oauthFailureHandler
    ) {
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.oauthFailureHandler = oauthFailureHandler;
    }
}
