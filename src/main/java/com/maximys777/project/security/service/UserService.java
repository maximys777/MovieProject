package com.maximys777.project.security.service;

import com.maximys777.project.security.dto.request.UserLoginRequest;
import com.maximys777.project.security.dto.response.UserCreatedResponse;
import com.maximys777.project.security.entity.UserEntity;
import com.maximys777.project.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void processOAuthPostLogin(UserLoginRequest userLoginRequest) {
        Optional<UserEntity> user = userRepository.findByEmail(userLoginRequest.email());

        UserEntity savedUser;

        if (user.isEmpty()) {
            UserEntity newUser = UserEntity.builder()
                    .email(userLoginRequest.email())
                    .googleId(userLoginRequest.googleId())
                    .userName(userLoginRequest.userName())
                    .profilePictureUrl(userLoginRequest.profilePictureUrl())
                    .build();

            savedUser = userRepository.save(newUser);
        } else {
            savedUser = user.get();
        }

        UserCreatedResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .userName(savedUser.getUserName())
                .googleId(savedUser.getGoogleId())
                .profilePictureUrl(savedUser.getProfilePictureUrl())
                .build();
    }
}
