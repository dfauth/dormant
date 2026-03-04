package io.github.dfauth.trade.service;

import io.github.dfauth.trade.authzn.OAuthUser;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findOrCreateUser(OAuthUser oAuthUser) {
        OAuth2User user = oAuthUser.user();
        String provider = user instanceof OidcUser ? "google" : oAuthUser.registrationId();
        String subject = extractSubject(user);
        String email = user.getAttribute("email");
        String name = user instanceof OidcUser oidcUser
                ? oidcUser.getFullName()
                : Optional.ofNullable((String) user.getAttribute("name"))
                        .orElse(user.getAttribute("login"));
        return findOrCreate(subject, email, name, provider);
    }

    public User findOrCreateUser(Jwt jwt) {
        return findOrCreate(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"), "google");
    }

    private User findOrCreate(String subject, String email, String name, String provider) {
        return findByEmail(email).orElseGet(() ->userRepository.findByGoogleIdAndProvider(subject, provider)
                .or(() -> email != null ? userRepository.findByEmail(email) : Optional.empty())
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleId(subject)
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .build())));
    }

    public Optional<User> findById(OAuthUser oAuthUser) {
        String provider = oAuthUser.user() instanceof OidcUser ? "google" : oAuthUser.registrationId();
        String subject = extractSubject(oAuthUser.user());
        return userRepository.findByGoogleIdAndProvider(subject, provider)
                .or(() -> {
                    String email = oAuthUser.user().getAttribute("email");
                    return email != null ? userRepository.findByEmail(email) : Optional.empty();
                });
    }

    public Optional<User> findById(Jwt jwt) {
        return userRepository.findByGoogleIdAndProvider(jwt.getSubject(), "google");
    }

    public Optional<User> findByEmail(Jwt jwt) {
        return userRepository.findByEmail(jwt.getClaimAsString("email"));
    }

    public Optional<User> findByEmail(OAuthUser oAuthUser) {
        return userRepository.findByEmail(oAuthUser.user().getAttribute("email"));
    }

    public Optional<User> findByEmail(String emailAddress) {
        return userRepository.findByEmail(emailAddress);
    }

    private String extractSubject(OAuth2User user) {
        if (user instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        // GitHub returns id as an Integer attribute
        Object id = user.getAttribute("id");
        if (id != null) {
            return id.toString();
        }
        throw new IllegalStateException("Cannot extract subject from OAuth2User attributes");
    }
}
