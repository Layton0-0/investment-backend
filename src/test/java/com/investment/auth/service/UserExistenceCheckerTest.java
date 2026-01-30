package com.investment.auth.service;

import com.investment.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExistenceChecker")
class UserExistenceCheckerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserExistenceChecker userExistenceChecker;

    @Test
    @DisplayName("exists 사용자 존재 시 true")
    void exists_userExists_returnsTrue() {
        when(userRepository.existsById("user-uuid-1")).thenReturn(true);

        assertTrue(userExistenceChecker.exists("user-uuid-1"));
        verify(userRepository).existsById("user-uuid-1");
    }

    @Test
    @DisplayName("exists 사용자 없을 시 false")
    void exists_userNotExists_returnsFalse() {
        when(userRepository.existsById("user-uuid-2")).thenReturn(false);

        assertFalse(userExistenceChecker.exists("user-uuid-2"));
    }

    @Test
    @DisplayName("exists userId null 시 false")
    void exists_null_returnsFalse() {
        assertFalse(userExistenceChecker.exists(null));
        verify(userRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("exists userId 공백 시 false")
    void exists_blank_returnsFalse() {
        assertFalse(userExistenceChecker.exists("   "));
        verify(userRepository, never()).existsById(any());
    }
}
