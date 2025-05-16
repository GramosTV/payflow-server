package com.payflow.api.service;

import com.payflow.api.exception.ConflictException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private SignUpRequest signUpRequest;

    @BeforeEach
    public void setup() {
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("encoded_password");
        testUser.setPhoneNumber("+1234567890");
        testUser.setEnabled(true);
        testUser.setRole(User.UserRole.USER);

        // Initialize signup request
        signUpRequest = new SignUpRequest();
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setFullName("Test User");
        signUpRequest.setPassword("password123");
        signUpRequest.setPhoneNumber("+1234567890");
    }

    @Test
    public void testCreateUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.createUser(signUpRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getFullName(), result.getFullName());
        assertEquals(testUser.getPassword(), result.getPassword());

        // Verify
        verify(userRepository).existsByEmail(signUpRequest.getEmail());
        verify(passwordEncoder).encode(signUpRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testCreateUser_EmailTaken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userService.createUser(signUpRequest));

        // Verify
        verify(userRepository).existsByEmail(signUpRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());

        // Verify
        verify(userRepository).findById(1L);
    }

    @Test
    public void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));

        // Verify
        verify(userRepository).findById(999L);
    }

    @Test
    public void testGetUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());

        // Verify
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    public void testGetUserByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("notfound@example.com"));

        // Verify
        verify(userRepository).findByEmail("notfound@example.com");
    }

    @Test
    public void testExistsByEmail() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act
        boolean exists = userService.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);

        // Verify
        verify(userRepository).existsByEmail("test@example.com");
    }
}
