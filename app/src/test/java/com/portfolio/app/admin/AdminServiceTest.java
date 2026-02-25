package com.portfolio.app.admin;

import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("WRITER 권한 부여 성공")
    void grantWriter_success() {
        User user = new User("user@example.com", "hash");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        adminService.grantWriter(1L);

        assertThat(user.getRoles()).contains("ROLE_WRITER");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("WRITER 권한 부여 시 ROLE_USER는 유지됨")
    void grantWriter_retainsExistingRoles() {
        User user = new User("user@example.com", "hash");  // already has ROLE_USER
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);

        adminService.grantWriter(1L);

        assertThat(user.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_WRITER");
    }

    @Test
    @DisplayName("존재하지 않는 유저에게 WRITER 부여 → 예외")
    void grantWriter_userNotFound_throwsException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.grantWriter(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
