package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.service.PositionService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public List<Position> getOpenPositions() {
        return positionService.getOpenPositions();
    }

    @GetMapping("/market/{market}")
    public List<Position> getPositionsByMarket(@PathVariable("market") String market) {
        return positionService.getPositionsByMarket(market);
    }

    @GetMapping("/market/{market}/code/{code}")
    public List<Position> getPositions(@PathVariable("market") String market, @PathVariable("code") String code) {
        return positionService.getPositions(market, code);
    }
}
