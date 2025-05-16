package com.payflow.api.service;

import com.payflow.api.exception.BadRequestException;
import com.payflow.api.model.dto.request.LoginRequest;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.dto.response.JwtAuthResponse;
import com.payflow.api.model.entity.User;
import com.payflow.api.security.JwtTokenProvider;
import com.payflow.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final WalletService walletService;

    /**
     * Register a new user
     */
    @Transactional
    public JwtAuthResponse register(SignUpRequest signUpRequest) {
        // Create user
        User user = userService.createUser(signUpRequest);

        // Create default USD wallet
        walletService.createDefaultWallet(user);

        // Authenticate user
        return authenticateUser(signUpRequest.getEmail(), signUpRequest.getPassword());
    }

    /**
     * Login a user
     */
    public JwtAuthResponse login(LoginRequest loginRequest) {
        return authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
    }

    /**
     * Authenticate user and generate JWT token
     */
    private JwtAuthResponse authenticateUser(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            return new JwtAuthResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getEmail(),
                    userPrincipal.getFullName(),
                    userPrincipal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
        } catch (Exception e) {
            throw new BadRequestException("Invalid email or password");
        }
    }
}
