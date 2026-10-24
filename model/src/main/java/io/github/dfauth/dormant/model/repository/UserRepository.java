package io.github.dfauth.dormant.model.repository;

import io.github.dfauth.dormant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByGoogleIdAndProvider(String googleId, String provider);

    Optional<User> findByEmail(String email);
}
