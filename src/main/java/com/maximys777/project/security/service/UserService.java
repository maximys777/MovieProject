package com.maximys777.project.security.service;

import com.maximys777.project.exceptions.exceptions.UsernameNotFoundException;
import com.maximys777.project.security.dto.request.UserLoginRequest;
import com.maximys777.project.security.dto.response.CurrentUserResponse;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public void processOAuthPostLogin(UserLoginRequest userLoginRequest) {
        Optional<UserEntity> user = userRepository.findByEmail(userLoginRequest.email());

        if (user.isEmpty()) {
            UserEntity newUser = UserEntity.builder()
                    .email(userLoginRequest.email())
                    .googleId(userLoginRequest.googleId())
                    .userName(userLoginRequest.userName())
                    .profilePictureUrl(userLoginRequest.profilePictureUrl())
                    .build();

            userRepository.save(newUser);
        } else {
            UserEntity existingUser = user.get();

            existingUser.setUserName(userLoginRequest.userName());
            existingUser.setProfilePictureUrl(userLoginRequest.profilePictureUrl());
        }
    }

    public CurrentUserResponse getCurrentUser(OidcUser oidcUser) {
        UserEntity user = handleUserNotFound(oidcUser.getEmail());

        return CurrentUserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private UserEntity handleUserNotFound(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
