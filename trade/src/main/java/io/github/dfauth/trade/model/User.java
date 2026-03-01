package io.github.dfauth.trade.model;

import io.github.dfauth.trycatch.Tuple2;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.function.BiFunction;

import static io.github.dfauth.trycatch.Tuple2.tuple2;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @Column(nullable = false)
    private String email;

    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String defaultMarket = "ASX";

    public <T> T resolveCode(String code, BiFunction<String,String, T> f2) {
        return resolveCode(code).map(f2);
    }

    public Tuple2<String, String> resolveCode(String marketCodeString) {
        return Optional.of(marketCodeString.split(":"))
                .filter(arr -> arr.length == 2)
                .map(arr -> tuple2(arr[0], arr[1]))
                .orElse(tuple2(defaultMarket, marketCodeString));
    }
}
