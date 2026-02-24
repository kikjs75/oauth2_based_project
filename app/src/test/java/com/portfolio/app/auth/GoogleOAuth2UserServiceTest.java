package com.portfolio.app.auth;

import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private GoogleOAuth2UserService service = new GoogleOAuth2UserService(null);

    private static final String PROVIDER_ID = "google-sub-123";
    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        // Inject the mock repository via field (constructor was called with null above)
        // We re-create the spy with the mock injected
        service = spy(new GoogleOAuth2UserService(userRepository));
    }

    private OAuth2User googleOAuth2User() {
        return new DefaultOAuth2User(
                Set.of(() -> "ROLE_USER"),
                Map.of("sub", PROVIDER_ID, "email", EMAIL, "name", "Test User"),
                "email"
        );
    }

    @Test
    @DisplayName("신규 Google 사용자 → DB에 저장 후 OAuth2User 반환")
    void loadUser_newGoogleUser_savesToDb() {
        // given
        doReturn(googleOAuth2User()).when(service).fetchFromGoogle(any());

        given(userRepository.findByProviderAndProviderId("GOOGLE", PROVIDER_ID))
                .willReturn(Optional.empty());
        given(userRepository.findByUsername(EMAIL))
                .willReturn(Optional.empty());

        User savedUser = new User(EMAIL, "GOOGLE", PROVIDER_ID);
        // Simulate JPA-assigned ID via reflection
        setId(savedUser, 42L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        OAuth2User result = service.loadUser(mock(OAuth2UserRequest.class));

        // then
        verify(userRepository, times(1)).save(any(User.class));
        assertThat((Long) result.getAttribute("userId")).isEqualTo(42L);
        assertThat((String) result.getAttribute("dbUsername")).isEqualTo(EMAIL);
        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) result.getAttribute("roles");
        assertThat(roles).contains("ROLE_USER");
    }

    @Test
    @DisplayName("동일 providerId Google 사용자 재로그인 → 저장 없이 기존 User 반환")
    void loadUser_existingGoogleUser_doesNotSave() {
        // given
        doReturn(googleOAuth2User()).when(service).fetchFromGoogle(any());

        User existingUser = new User(EMAIL, "GOOGLE", PROVIDER_ID);
        setId(existingUser, 7L);
        given(userRepository.findByProviderAndProviderId("GOOGLE", PROVIDER_ID))
                .willReturn(Optional.of(existingUser));

        // when
        OAuth2User result = service.loadUser(mock(OAuth2UserRequest.class));

        // then
        verify(userRepository, never()).save(any());
        assertThat((Long) result.getAttribute("userId")).isEqualTo(7L);
    }

    @Test
    @DisplayName("동일 email로 LOCAL 가입 이력 있을 때 → 기존 LOCAL User 반환, 저장 없음")
    void loadUser_emailAlreadyRegisteredLocally_returnsExistingUser() {
        // given
        doReturn(googleOAuth2User()).when(service).fetchFromGoogle(any());

        given(userRepository.findByProviderAndProviderId("GOOGLE", PROVIDER_ID))
                .willReturn(Optional.empty());

        User localUser = new User(EMAIL, "password-hash");
        setId(localUser, 3L);
        given(userRepository.findByUsername(EMAIL))
                .willReturn(Optional.of(localUser));

        // when
        OAuth2User result = service.loadUser(mock(OAuth2UserRequest.class));

        // then
        verify(userRepository, never()).save(any());
        assertThat((Long) result.getAttribute("userId")).isEqualTo(3L);
        assertThat((String) result.getAttribute("dbUsername")).isEqualTo(EMAIL);
    }

    private static void setId(User user, long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
