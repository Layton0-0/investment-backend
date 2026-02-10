package com.investment.auth.service;

import com.investment.auth.dto.CreateAdminUserRequestDto;
import com.investment.auth.dto.CreateAdminUserResponseDto;
import com.investment.common.exception.DomainException;
import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService")
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("createAdminUser 정상 시 201 응답 DTO 반환")
    void createAdminUser_success_returnsResponse() {
        CreateAdminUserRequestDto request = new CreateAdminUserRequestDto();
        request.setUsername("admin1");
        request.setPassword("MyP@ssw0rd99");
        request.setRole("Admin");

        when(userRepository.existsByUsername("admin1")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        User saved = User.builder().username("admin1").passwordHash("encoded").role("Admin").build();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            User withId = User.builder()
                    .username(u.getUsername())
                    .passwordHash(u.getPasswordHash())
                    .role(u.getRole())
                    .build();
            ReflectionTestUtils.setField(withId, "id", "saved-id-1");
            return withId;
        });

        CreateAdminUserResponseDto result = adminUserService.createAdminUser(request);

        assertThat(result.getUserId()).isEqualTo("saved-id-1");
        assertThat(result.getUsername()).isEqualTo("admin1");
        assertThat(result.getRole()).isEqualTo("Admin");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("createAdminUser 중복 username 시 DomainException")
    void createAdminUser_duplicateUsername_throws() {
        CreateAdminUserRequestDto request = new CreateAdminUserRequestDto();
        request.setUsername("admin1");
        request.setPassword("MyP@ssw0rd99");
        request.setRole("Admin");
        when(userRepository.existsByUsername("admin1")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createAdminUser(request))
                .isInstanceOf(DomainException.class);
        verify(userRepository, never()).save(any());
    }
}
