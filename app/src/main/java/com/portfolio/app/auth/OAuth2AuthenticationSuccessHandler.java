package com.portfolio.app.auth;

import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
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

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            @Value("${oauth2.redirect-uri:http://localhost:3000/callback}") String redirectUri) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // OIDC 플로우에서 enriched attributes가 전달되지 않을 수 있으므로 DB에서 직접 조회
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByProviderAndProviderId("GOOGLE", providerId)
                .orElseGet(() -> userRepository.findByUsername(email).orElse(null));

        if (user == null) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=oauth_user_not_found");
            return;
        }

        List<String> roles = List.copyOf(user.getRoles());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
