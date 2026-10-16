package com.maximys777.project.security.dto.response;

import lombok.Builder;

@Builder
public record CurrentUserResponse(
        Long id,
        String userName,
        String profilePictureUrl
) {
}
