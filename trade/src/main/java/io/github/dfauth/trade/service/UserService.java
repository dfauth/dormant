package io.github.dfauth.trade.service;

import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findOrCreateUser(OidcUser oidcUser) {
        return findOrCreateUser(oidcUser.getSubject(), oidcUser.getEmail(), oidcUser.getFullName());
    }

    public User findOrCreateUser(Jwt jwt) {
        return findOrCreateUser(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
    }

    public User findOrCreateUser(String googleId, String email, String name) {
        return findById(googleId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleId(googleId)
                        .email(email)
                        .name(name)
                        .build()));
    }

    public Optional<User> findById(OidcUser oidcUser) {
        return findById(oidcUser.getSubject());
    }

    public Optional<User> findById(Jwt jwt) {
        return findById(jwt.getSubject());
    }

    public Optional<User> findById(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }
}
