package com.oursociety.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class OAuthRedirectHandlers {
    @Bean
    AuthenticationSuccessHandler oauthSuccessHandler(
        @Value("${app.oauth.success-redirect}") String redirect
    ) {
        return (request, response, authentication) -> response.sendRedirect(redirect);
    }

    @Bean
    AuthenticationFailureHandler oauthFailureHandler(
        @Value("${app.oauth.failure-redirect}") String redirect
    ) {
        return (request, response, exception) -> response.sendRedirect(
            redirect + "?error=" + URLEncoder.encode(safeMessage(exception), StandardCharsets.UTF_8)
        );
    }

    private static String safeMessage(AuthenticationException exception) {
        return exception.getClass().getSimpleName();
    }
}
