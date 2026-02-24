package com.portfolio.app.auth;

import com.portfolio.app.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private static final String REDIRECT_URI = "http://localhost:3000/callback";

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, REDIRECT_URI);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 → JWT 발급 후 redirectUri?token=<JWT> 로 리다이렉트")
    void onAuthenticationSuccess_redirectsWithToken() throws IOException {
        // given
        OAuth2User principal = new DefaultOAuth2User(
                Set.of(() -> "ROLE_USER"),
                Map.of(
                        "email", "user@example.com",
                        "userId", 1L,
                        "dbUsername", "user@example.com",
                        "roles", Set.of("ROLE_USER")
                ),
                "email"
        );
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtTokenProvider.generateToken(1L, "user@example.com", List.of("ROLE_USER")))
                .willReturn("test-jwt-token");
        given(response.encodeRedirectURL(anyString())).will(returnsFirstArg());

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(urlCaptor.capture());

        String redirectUrl = urlCaptor.getValue();
        assertThat(redirectUrl).startsWith(REDIRECT_URI);
        assertThat(redirectUrl).contains("token=test-jwt-token");
    }

    @Test
    @DisplayName("roles 속성이 null일 때 → ROLE_USER 기본값으로 JWT 발급")
    void onAuthenticationSuccess_nullRoles_usesDefaultRole() throws IOException {
        // given
        OAuth2User principal = new DefaultOAuth2User(
                Set.of(() -> "ROLE_USER"),
                Map.of(
                        "email", "user@example.com",
                        "userId", 2L,
                        "dbUsername", "user@example.com"
                        // roles 속성 없음
                ),
                "email"
        );
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtTokenProvider.generateToken(eq(2L), eq("user@example.com"), anyList()))
                .willReturn("fallback-jwt");
        given(response.encodeRedirectURL(anyString())).will(returnsFirstArg());

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("token=fallback-jwt");
    }

    @Test
    @DisplayName("커스텀 redirectUri 설정 → 해당 URI로 리다이렉트")
    void onAuthenticationSuccess_customRedirectUri() throws IOException {
        // given
        String customUri = "https://myapp.com/auth/callback";
        handler = new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, customUri);

        OAuth2User principal = new DefaultOAuth2User(
                Set.of(() -> "ROLE_USER"),
                Map.of(
                        "email", "user@example.com",
                        "userId", 3L,
                        "dbUsername", "user@example.com",
                        "roles", Set.of("ROLE_WRITER")
                ),
                "email"
        );
        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtTokenProvider.generateToken(anyLong(), anyString(), anyList()))
                .willReturn("custom-jwt");
        given(response.encodeRedirectURL(anyString())).will(returnsFirstArg());

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).startsWith(customUri);
        assertThat(urlCaptor.getValue()).contains("token=custom-jwt");
    }
}
