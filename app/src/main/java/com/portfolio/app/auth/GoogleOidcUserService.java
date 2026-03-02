package com.portfolio.app.auth;

import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public GoogleOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // ID 토큰에서 직접 sub, email 추출 (userinfo endpoint 호출 불필요)
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerId = oidcUser.getSubject();  // sub — ID 토큰에 항상 존재
        String email = oidcUser.getEmail();          // email — email scope 요청 시 ID 토큰에 포함

        userRepository.findByProviderAndProviderId("GOOGLE", providerId)
                .orElseGet(() -> userRepository.findByUsername(email)
                        .orElseGet(() -> userRepository.saveAndFlush(
                                new User(email, "GOOGLE", providerId))));

        return oidcUser;
    }
}
