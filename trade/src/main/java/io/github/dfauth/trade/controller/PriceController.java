package io.github.dfauth.trade.controller;

import io.github.dfauth.ta.TrendCalculator;
import io.github.dfauth.trade.model.*;
import io.github.dfauth.trade.repository.PriceRepository;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.dfauth.trycatch.Utils.oops;
import static java.lang.Math.abs;

@Slf4j
@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices", description = "Manage historical OHLCV price data")
public class PriceController extends BaseController {

    private final PriceRepository priceRepository;

    public PriceController(PriceRepository priceRepository, UserService userService) {
        super(userService);
        this.priceRepository = priceRepository;
    }

    @Operation(summary = "Batch insert prices", description = "Persists a list of prices for a security, silently skipping any that already exist for the same market, code, and date.")
    @ApiResponse(responseCode = "201", description = "Number of new prices persisted")
    @PostMapping("/batch/{code}")
    public ResponseEntity<Integer> createPrices(
            @Parameter(description = "Security code (e.g. BHP)") @PathVariable("code") String code,
            @RequestBody List<Price> prices) {
        List<Price> newPrices = prices.stream()
                .filter(p -> !priceRepository.existsByMarketAndCodeAndDate(p.getMarket(), p.getCode(), p.getDate()))
                .toList();
        priceRepository.saveAll(newPrices);
        log.info("Persisted {} of {} prices ({} duplicates skipped)", newPrices.size(), prices.size(), prices.size() - newPrices.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(newPrices.size());
    }

    @Operation(summary = "Stocks near recent high / low", description = "Returns stocks whose current price is within a configurable threshold of their proximity to a recent high / low, sorted by proximity to that watermark.")
    @ApiResponse(responseCode = "200", description = "List of high / low summaries")
    @GetMapping("/watermark")
    public List<Watermark<Price>> getWatermark(
            @Parameter(description = "Market code (e.g. ASX) defaults to users default market") @RequestParam("market") Optional<String> market,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y); defaults to 1Y") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "threshold as a % of the high / low (e.g. 0.15, defaults to 0.1)") @RequestParam("threshold") Optional<Double> optThreshold,
            @Parameter(description = "direction (e.g. HIGH or LOW, defaults to High)") @RequestParam("direction") Optional<Direction> optDirection
    ) {
        return authorize(u -> {
            String mkt = market.orElse(u.getDefaultMarket());
            double threshold = optThreshold.orElse(0.1);
            return priceRepository.findDistinctCodesByMarket(mkt).stream()
                    .map(code -> {
                        return getWatermarkByCode(code, tenor, optDirection);
                    })
                    .filter(w ->
                            abs(w.getDistance()) < threshold)
                    .sorted(Comparator.comparingDouble(Watermark::getDistance))
                    .collect(Collectors.toList());
        });
    }


    @Operation(summary = "Stocks near recent high / low", description = "Returns stocks whose current price is within a configurable threshold of their proximity to a recent high / low, sorted by proximity to that watermark.")
    @ApiResponse(responseCode = "200", description = "List of high / low summaries")
    @GetMapping("/watermark/{code}")
    public Watermark<Price> getWatermarkByCode(
            @Parameter(description = "code") @PathVariable("code") String code,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y); defaults to 1Y") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "direction (e.g. HIGH or LOW, defaults to High)") @RequestParam("direction") Optional<Direction> optDirection
    ) {
        return authorize(u -> {
            return u.resolveCode(code, (m, c) -> {
                TenorRange tenorRange = TenorRange.parse(tenor.orElse("1Y"));
                Direction direction = optDirection.orElse(Direction.RISING);
                return priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(m, c, tenorRange.start(), tenorRange.end())
                        .stream().reduce(new Watermark<>(direction, p -> p.getClose().doubleValue()), Watermark::update, oops());
            });
        });
    }

    @Operation(summary = "Get trending stocks", description = "Calculates the current trend state for every stock in the given market and optionally filters by sentiment.")
    @ApiResponse(responseCode = "200", description = "List of trend summaries")
    @GetMapping("/trending")
    public List<TrendSummary> getTrending(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") String market,
            @Parameter(description = "Trend state filter (e.g. BULL, BEAR)") @RequestParam("sentiment") Optional<String> sentiment) {
        return priceRepository.findDistinctCodesByMarket(market).stream()
                .flatMap(code -> {
                    double[] prices = priceRepository.findByMarketAndCodeOrderByDateAsc(market, code)
                            .stream()
                            .mapToDouble(p -> p.getClose().doubleValue())
                            .toArray();
                    return TrendCalculator.trend(prices, 8, 21, 200)
                            .stream()
                            .map(trend -> new TrendSummary(market, code, trend.getPrice(), trend));
                })
                .filter(ts -> sentiment.isEmpty() || ts.getTrendState().getTrendState().name().equals(sentiment.get()))
                .sorted(Comparator.comparing(TrendSummary::getCode))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get prices for a security", description = "Returns OHLCV prices ordered by date ascending, optionally filtered by date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of prices")
    @GetMapping("/{code}")
    public List<Price> getPrices(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") Optional<String> market,
            @Parameter(description = "Security code (e.g. BHP)") @PathVariable("code") String code,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            Optional<DateRange> dateRange = DateRange.resolve(tenor, startFrom, endAt);
            return u.resolveCode(code).map((mkt, cd) -> dateRange
                            .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(mkt, cd, dr.start(), dr.end()))
                            .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(mkt, cd)));
        });
    }
}
