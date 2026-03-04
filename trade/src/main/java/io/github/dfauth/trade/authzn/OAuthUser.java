package io.github.dfauth.trade.authzn;

import org.springframework.security.oauth2.core.user.OAuth2User;

public record OAuthUser(OAuth2User user, String registrationId) {}
