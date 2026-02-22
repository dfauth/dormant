package io.github.dfauth.trade.authzn;

import io.github.dfauth.trycatch.Either;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- OidcUser principal ---

    @Test
    void authorize_withOidcUser_invokesFunction() {
        OidcUser oidcUser = mock(OidcUser.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oidcUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var holder = new Object() { boolean called = false; };
        Authorization.authorize(e -> { holder.called = true; return null; });
        assertTrue(holder.called);
    }

    @Test
    void authorize_withOidcUser_passesLeft() {
        OidcUser oidcUser = mock(OidcUser.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oidcUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Either<OidcUser, Jwt> result = Authorization.authorize(e -> e);
        assertTrue(result.isLeft());
        assertSame(oidcUser, result.left());
    }

    // --- Jwt principal ---

    @Test
    void authorize_withJwt_invokesFunction() {
        Jwt jwt = mock(Jwt.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var holder = new Object() { boolean called = false; };
        Authorization.authorize(e -> { holder.called = true; return null; });
        assertTrue(holder.called);
    }

    @Test
    void authorize_withJwt_passesRight() {
        Jwt jwt = mock(Jwt.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Either<OidcUser, Jwt> result = Authorization.authorize(e -> e);
        assertTrue(result.isRight());
        assertSame(jwt, result.right());
    }

    // --- Unsupported principal ---

    @Test
    void authorize_withUnsupportedPrincipal_throwsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not-a-valid-principal");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(IllegalStateException.class, () -> Authorization.authorize(e -> e));
    }

    @Test
    void authorize_withUnsupportedPrincipal_messageContainsType() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(42);
        SecurityContextHolder.getContext().setAuthentication(auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Authorization.authorize(e -> e));
        assertTrue(ex.getMessage().contains("Integer") || ex.getMessage().contains("Unsupported"));
    }

    // --- Return value pass-through ---

    @Test
    void authorize_functionReturnValueIsReturned() {
        OidcUser oidcUser = mock(OidcUser.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oidcUser);
        SecurityContextHolder.getContext().setAuthentication(auth);

        String result = Authorization.authorize(e -> "expected");
        assertEquals("expected", result);
    }
}
