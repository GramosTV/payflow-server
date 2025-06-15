package com.payflow.api.service;

import com.payflow.api.exception.ConflictException;
import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for user management operations. Handles user creation, retrieval, and validation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public User createUser(final SignUpRequest signUpRequest) {
    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      throw new ConflictException("Email is already taken");
    }
    final User user = new User();
    user.setFullName(signUpRequest.getFullName());
    user.setEmail(signUpRequest.getEmail());
    user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
    user.setPhoneNumber(signUpRequest.getPhoneNumber());
    user.setEnabled(true);
    user.setRole(User.UserRole.USER);

    return userRepository.save(user);
  }

  public User getUserById(final Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
  }

  public User getUserByEmail(final String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
  }

  public boolean existsByEmail(final String email) {
    return userRepository.existsByEmail(email);
  }
}
