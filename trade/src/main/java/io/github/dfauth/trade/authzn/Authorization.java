package io.github.dfauth.trade.authzn;

import io.github.dfauth.trycatch.Either;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.function.Function;

import static io.github.dfauth.trycatch.Either.left;
import static io.github.dfauth.trycatch.Either.right;

public class Authorization {

    public static <T> T authorize(Function<Either<OidcUser, Jwt>, T> f) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication  != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                return f.apply(left(oidcUser));
            } else if (principal instanceof Jwt jwt) {
                return f.apply(right(jwt));
            }
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        } else {
            throw new IllegalStateException("Unsupported authentication type: " + authentication.getClass());
        }
    }

}
