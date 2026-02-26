package com.portfolio.app.security;

import com.portfolio.app.auth.GoogleOAuth2UserService;
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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtTokenProvider tokenProvider,
                          GoogleOAuth2UserService googleOAuth2UserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler) {
        this.tokenProvider = tokenProvider;
        this.googleOAuth2UserService = googleOAuth2UserService;
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
                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> {
                    // non-OIDC OAuth2 (openid 스코프 없을 때)
                    ui.userService(googleOAuth2UserService);
                    // OIDC (openid 스코프 포함 시 — Google 기본값)
                    // DefaultOidcUserService 가 userinfo 엔드포인트 호출을 googleOAuth2UserService 에 위임
                    OidcUserService oidcUserService = new OidcUserService();
                    oidcUserService.setOauth2UserService(googleOAuth2UserService);
                    ui.oidcUserService(oidcUserService);
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
