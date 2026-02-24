package com.portfolio.app.auth;

import com.portfolio.app.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            @Value("${oauth2.redirect-uri:http://localhost:3000/callback}") String redirectUri) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Long userId = (Long) oAuth2User.getAttribute("userId");
        String username = (String) oAuth2User.getAttribute("dbUsername");
        @SuppressWarnings("unchecked")
        Set<String> rolesSet = (Set<String>) oAuth2User.getAttribute("roles");
        List<String> roles = rolesSet != null ? List.copyOf(rolesSet) : List.of("ROLE_USER");

        String token = jwtTokenProvider.generateToken(userId, username, roles);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
