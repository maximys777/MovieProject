package com.maximys777.project.security.service;

import com.maximys777.project.security.dto.request.UserLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");
        String googleId = oidcUser.getAttribute("sub");
        String picture = oidcUser.getAttribute("picture");

        UserLoginRequest loginRequest = new UserLoginRequest(email, name, googleId, picture);

        userService.processOAuthPostLogin(loginRequest);

        return oidcUser;
    }
}
