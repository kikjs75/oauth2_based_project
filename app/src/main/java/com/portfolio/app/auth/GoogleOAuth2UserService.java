package com.portfolio.app.auth;

import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Separated for testability — allows @Spy to stub the Google HTTP call. */
    protected OAuth2User fetchFromGoogle(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = fetchFromGoogle(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");

        User user = userRepository.findByProviderAndProviderId("GOOGLE", providerId)
                .orElseGet(() -> {
                    // If the email already exists as a LOCAL user, return that user
                    return userRepository.findByUsername(email)
                            .orElseGet(() -> userRepository.save(new User(email, "GOOGLE", providerId)));
                });

        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("userId", user.getId());
        enrichedAttributes.put("dbUsername", user.getUsername());
        enrichedAttributes.put("roles", user.getRoles());

        return new DefaultOAuth2User(
                user.getRoles().stream()
                        .map(role -> (org.springframework.security.core.GrantedAuthority)
                                () -> role)
                        .collect(Collectors.toList()),
                enrichedAttributes,
                "email"
        );
    }
}
