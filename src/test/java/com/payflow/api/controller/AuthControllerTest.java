package com.payflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.api.model.dto.request.LoginRequest;
import com.payflow.api.model.dto.request.SignUpRequest;
import com.payflow.api.model.dto.response.JwtAuthResponse;
import com.payflow.api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private SignUpRequest signUpRequest;
    private LoginRequest loginRequest;
    private JwtAuthResponse authResponse;

    @BeforeEach
    public void setup() {
        // Initialize signup request
        signUpRequest = new SignUpRequest();
        signUpRequest.setFullName("Test User");
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setPhoneNumber("+1234567890");

        // Initialize login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Initialize auth response
        authResponse = new JwtAuthResponse(
                "test-jwt-token",
                "Bearer", // Added tokenType argument
                1L,
                "test@example.com",
                "Test User",
                "ROLE_USER");
    }

    @Test
    public void testRegisterUser() throws Exception {
        when(authService.register(any(SignUpRequest.class))).thenReturn(authResponse);

        MvcResult result = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JwtAuthResponse response = objectMapper.readValue(content, JwtAuthResponse.class);

        assertEquals(authResponse.getAccessToken(), response.getAccessToken());
    }

    @Test
    public void testLoginUser() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JwtAuthResponse response = objectMapper.readValue(content, JwtAuthResponse.class);

        assertEquals(authResponse.getAccessToken(), response.getAccessToken());
    }

    @Test
    public void testRegisterUser_InvalidData() throws Exception {
        // Create invalid request (missing required fields)
        SignUpRequest invalidRequest = new SignUpRequest();
        invalidRequest.setEmail(""); // Invalid email

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLoginUser_InvalidData() throws Exception {
        // Create invalid request
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setEmail(""); // Invalid email

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
