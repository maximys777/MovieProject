package com.maximys777.project.security.controller;

import com.maximys777.project.AbstractIntegrationTest;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends AbstractIntegrationTest {
    private static final String EMAIL = "test@gmail.com";

    @Autowired
    private UserRepository userRepository;

    @Test
    void getCurrentUser_shouldReturnCurrentUserResponse_whenUserLoggedIn() throws Exception {
        UserEntity user = UserEntity.builder()
                .email(EMAIL)
                .userName("max")
                .googleId("google-id")
                .profilePictureUrl("profile-picture-url")
                .build();

        userRepository.save(user);

        mockMvc.perform(get("/users/me")
                        .with(authenticatedUser(EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.userName").value(user.getUserName()))
                .andExpect(jsonPath("$.profilePictureUrl").value("profile-picture-url"));
    }

    @Test
    void getCurrentUser_shouldThrowUserNotFound_whenUserNotExist() throws Exception {
        mockMvc.perform(get("/users/me")
                        .with(authenticatedUser(EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorized_whenUserNotAuthorized() throws Exception {
        mockMvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor authenticatedUser(String email) {
        return oidcLogin().idToken(token -> token.claim("email", email));
    }
}
