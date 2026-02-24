package com.portfolio.app.auth;

import com.portfolio.app.auth.dto.LoginRequest;
import com.portfolio.app.auth.dto.SignupRequest;
import com.portfolio.app.auth.dto.TokenResponse;
import com.portfolio.app.security.JwtTokenProvider;
import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = tokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                List.copyOf(user.getRoles())
        );
        return new TokenResponse(token);
    }
}
