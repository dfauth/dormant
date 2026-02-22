package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.service.PositionService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Positions", description = "View portfolio positions and performance statistics")
public class PositionController extends BaseController {

    private final PositionService positionService;

    public PositionController(UserService userService, PositionService positionService) {
        super(userService);
        this.positionService = positionService;
    }

    @Operation(summary = "Get all open positions", description = "Returns all open positions across all markets for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of open positions")
    @GetMapping
    public List<Position> getOpenPositions() {
        return authorize(u -> positionService.getOpenPositions(u.getId()));
    }

    @Operation(summary = "Get all closed positions", description = "Returns all closed positions across all markets for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of closed positions")
    @GetMapping("/closed")
    public List<Position> getClosedPositions() {
        return authorize(u -> positionService.getPositions(u.getId(), Position::isClosed));
    }

    @Operation(summary = "Get open positions by market", description = "Returns open positions in the specified market for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of open positions in the market")
    @GetMapping("/market/{market}")
    public List<Position> getPositionsByMarket(
            @Parameter(description = "Market code (e.g. ASX)") @PathVariable("market") String market) {
        return authorize(u -> positionService.getPositionsByMarket(u.getId(), market));
    }

    @Operation(summary = "Get positions for a specific security", description = "Returns all positions (open and closed) for a given market and security code.")
    @ApiResponse(responseCode = "200", description = "List of positions for the security")
    @GetMapping("/market/{market}/code/{code}")
    public List<Position> getPositions(
            @Parameter(description = "Market code (e.g. ASX)") @PathVariable("market") String market,
            @Parameter(description = "Security code (e.g. BHP)") @PathVariable("code") String code) {
        return authorize(u -> positionService.getPositions(u.getId(), market, code));
    }

    @Operation(summary = "Get performance statistics for a market", description = "Returns aggregated performance stats (win/loss ratio, average win/loss, expectancy) for closed positions in the market.")
    @ApiResponse(responseCode = "200", description = "Performance statistics")
    @GetMapping("/market/{market}/performance")
    public PerformanceStats getPerformanceStats(
            @Parameter(description = "Market code (e.g. ASX)") @PathVariable("market") String market) {
        return authorize(u -> positionService.getPerformanceStats(u.getId(), market));
    }
}
