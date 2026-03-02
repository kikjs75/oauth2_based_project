package com.portfolio.app.security;

import com.portfolio.app.auth.GoogleOAuth2UserService;
import com.portfolio.app.auth.GoogleOidcUserService;
import com.portfolio.app.auth.OAuth2AuthenticationSuccessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleOidcUserService googleOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtTokenProvider tokenProvider,
                          GoogleOAuth2UserService googleOAuth2UserService,
                          GoogleOidcUserService googleOidcUserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler) {
        this.tokenProvider = tokenProvider;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleOidcUserService = googleOidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> {
                    // non-OIDC OAuth2 (openid 스코프 없을 때)
                    ui.userService(googleOAuth2UserService);
                    // OIDC (openid 스코프 포함 시 — Google 기본값)
                    // ID 토큰에서 sub/email 직접 추출하여 DB 저장 처리
                    ui.oidcUserService(googleOidcUserService);
                })
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
