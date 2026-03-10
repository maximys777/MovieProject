package com.maximys777.project.security.dto.response;

import lombok.Builder;

@Builder
public record UserCreatedResponse(
        Long id,
        String email,
        String userName,
        String googleId,
        String profilePictureUrl
) {
}
