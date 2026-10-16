package com.maximys777.project.security.service;

import com.maximys777.project.exceptions.exceptions.UsernameNotFoundException;
import com.maximys777.project.security.dto.response.CurrentUserResponse;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final String EMAIL = "test@gmail.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_shouldReturnCurrentUserResponse_WhenSuccess() {
        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .userName("max")
                .email(EMAIL)
                .googleId("google-id")
                .profilePictureUrl("profile-picture-url")
                .build();


        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(userEntity));

        CurrentUserResponse expectedResponse = CurrentUserResponse.builder()
                .id(userEntity.getId())
                .userName(userEntity.getUserName())
                .profilePictureUrl(userEntity.getProfilePictureUrl())
                .build();

        CurrentUserResponse actualResponse = userService.getCurrentUser(oidcUser);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(expectedResponse, actualResponse);

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(EMAIL);
    }

    @Test
    void getCurrentUser_shouldThrowUserNotFound_WhenUserEmailNotFound() {
        Mockito.when(oidcUser.getEmail()).thenReturn(EMAIL);
        Mockito.when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> userService.getCurrentUser(oidcUser));

        Assertions.assertEquals("User not found", exception.getMessage());

        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(EMAIL);
    }
}
