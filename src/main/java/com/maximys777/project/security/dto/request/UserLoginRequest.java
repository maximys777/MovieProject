package com.maximys777.project.security.dto.request;

public record UserLoginRequest(
        String email,
        String userName,
        String googleId,
        String profilePictureUrl
) {
}
