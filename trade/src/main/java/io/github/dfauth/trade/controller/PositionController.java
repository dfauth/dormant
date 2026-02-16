package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.service.PositionService;
import io.github.dfauth.trade.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;
    private final UserService userService;

    private Long resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return userService.findOrCreateUser(oidcUser).getId();
        } else if (principal instanceof Jwt jwt) {
            return userService.findOrCreateUser(jwt).getId();
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

    @GetMapping
    public List<Position> getOpenPositions(Authentication authentication) {
        return positionService.getOpenPositions(resolveUserId(authentication));
    }

    @GetMapping("/market/{market}")
    public List<Position> getPositionsByMarket(@PathVariable("market") String market, Authentication authentication) {
        return positionService.getPositionsByMarket(resolveUserId(authentication), market);
    }

    @GetMapping("/market/{market}/code/{code}")
    public List<Position> getPositions(@PathVariable("market") String market, @PathVariable("code") String code, Authentication authentication) {
        return positionService.getPositions(resolveUserId(authentication), market, code);
    }

    @GetMapping("/market/{market}/performance")
    public PerformanceStats getPerformanceStats(@PathVariable("market") String market, Authentication authentication) {
        return positionService.getPerformanceStats(resolveUserId(authentication), market);
    }
}
