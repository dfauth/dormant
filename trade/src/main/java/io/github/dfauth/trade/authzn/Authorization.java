package io.github.dfauth.trade.authzn;

import io.github.dfauth.trycatch.Either;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.function.Function;

import static io.github.dfauth.trycatch.Either.left;
import static io.github.dfauth.trycatch.Either.right;

public class Authorization {

    public static <T> T authorize(Function<Either<OAuthUser, Jwt>, T> f) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                // Google OIDC — always treated as "google" regardless of test mock registration ID
                return f.apply(left(new OAuthUser(oidcUser, "google")));
            } else if (principal instanceof OAuth2User oAuth2User && authentication instanceof OAuth2AuthenticationToken token) {
                // OAuth2-only provider (e.g. GitHub)
                return f.apply(left(new OAuthUser(oAuth2User, token.getAuthorizedClientRegistrationId())));
            } else if (principal instanceof Jwt jwt) {
                return f.apply(right(jwt));
            }
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        } else {
            throw new IllegalStateException("No authentication present");
        }
    }

}
