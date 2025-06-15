package com.payflow.api.security;

import com.payflow.api.exception.ResourceNotFoundException;
import com.payflow.api.model.entity.User;
import com.payflow.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Custom user details service for Spring Security authentication. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Loads user details by email for authentication.
   *
   * @param email the user's email
   * @return user details for authentication
   * @throws UsernameNotFoundException if user not found
   */
  @Override
  @Transactional
  public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
    final User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with email : " + email));
    return UserPrincipal.create(user);
  }

  /**
   * Loads user details by ID.
   *
   * @param id the user ID
   * @return user details
   * @throws ResourceNotFoundException if user not found
   */
  @Transactional
  public UserDetails loadUserById(final Long id) {
    final User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    return UserPrincipal.create(user);
  }
}
