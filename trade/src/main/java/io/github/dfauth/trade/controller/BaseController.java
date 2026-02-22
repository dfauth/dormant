package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.authzn.Authorization;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class BaseController {

    private final UserService userService;

    protected <T> T authorize(Function<User, T> f) {
        return Authorization.authorize(e -> {
            return e.isLeft() ? e.mapLeft(oidcUser -> f.apply(userService.findById(oidcUser).orElseThrow())) :
                    e.mapRight(jwt -> f.apply(userService.findById(jwt).orElseThrow()));
        });
    }

}
