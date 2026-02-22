package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.PerformanceStats;
import io.github.dfauth.trade.model.Position;
import io.github.dfauth.trade.model.PositionPredicate;
import io.github.dfauth.trade.service.PositionService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static io.github.dfauth.trade.model.PositionPredicate.CLOSED;
import static io.github.dfauth.trade.model.PositionPredicate.OPEN;
import static io.github.dfauth.trycatch.Optionals.or;
import static io.github.dfauth.trycatch.Predicates.always;
import static java.util.Optional.empty;

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
    @GetMapping("/open")
    public List<Position> getOpenPositions() {
        return getPositions(Optional.of(OPEN), empty(), empty(), empty());
    }

    @Operation(summary = "Get all closed positions", description = "Returns all closed positions across all markets for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of closed positions")
    @GetMapping("/closed")
    public List<Position> getClosedPositions() {
        return getPositions(Optional.of(CLOSED), empty(), empty(), empty());
    }

    @Operation(summary = "Get positions", description = "Returns positions across all markets for the authenticated user filtered by the provided predicates.")
    @ApiResponse(responseCode = "200", description = "List of matching positions")
    @GetMapping
    public List<Position> getPositions(@Parameter(description = "Position predicate (ie. OPEN / CLOSED)") @RequestParam("predicate") Optional<PositionPredicate> positionPredicate,
                                       @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenor,
                                       @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
                                       @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        Optional<Predicate<Position>> dateRangePredicate = DateRange.resolve(tenor, startFrom, endAt)
                .map(dr -> p ->
                        (p.getOpenDate().isAfter(dr.start()) && p.getCloseDate().map(cd -> dr.end().isBefore(cd)).orElse(true))
                );
        Optional<Predicate<Position>> openClosePredicate = positionPredicate.map(_p -> (Predicate<Position>) _p);
        return authorize(u -> positionService.getPositions(u.getId(), or(Predicate::and, dateRangePredicate, openClosePredicate).orElse(always())));
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
