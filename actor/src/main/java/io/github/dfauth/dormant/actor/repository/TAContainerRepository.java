package io.github.dfauth.dormant.actor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TAContainerRepository extends JpaRepository<TAContainer, TAContainerId> {
    Optional<TAContainer> findByMarketAndCodeAndDate(String market, String code, LocalDate date);
}
