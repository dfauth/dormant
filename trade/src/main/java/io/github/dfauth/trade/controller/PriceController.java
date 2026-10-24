package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.dfauth.dormant.model.Price;
import io.github.dfauth.ta.Dated;
import io.github.dfauth.ta.Trend;
import io.github.dfauth.ta.TrendCalculator;
import io.github.dfauth.ta.TrendState;
import io.github.dfauth.dormant.kafka.KafkaPublisher;
import io.github.dfauth.trade.model.*;
import io.github.dfauth.dormant.model.repository.PriceRepository;
import io.github.dfauth.trade.service.UserService;
import io.github.dfauth.trade.utils.LocalDateDeserializer;
import io.github.dfauth.trade.utils.TenorDeserializer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.dfauth.trade.model.Tenor._1Y;
import static io.github.dfauth.trycatch.TryCatch.tryCatch;
import static io.github.dfauth.trycatch.Utils.oops;
import static java.lang.Math.abs;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

@Slf4j
@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices", description = "Manage historical OHLCV price data")
public class PriceController extends BaseController implements ControllerMixIn {

    private final PriceRepository priceRepository;
    private final KafkaPublisher<String, Price> kafkaPublisher;

    public PriceController(UserService userService, PriceRepository priceRepository, KafkaPublisher<String, Price> kafkaPublisher) {
        super(userService);
        this.priceRepository = priceRepository;
        this.kafkaPublisher = kafkaPublisher;
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
        // publish new prices
        newPrices.forEach(p -> tryCatch(() -> kafkaPublisher.publish(p.getMarketCode(), p)));
        return ResponseEntity.status(HttpStatus.CREATED).body(newPrices.size());
    }

    @Operation(summary = "Stocks near recent high / low", description = "Returns stocks whose current price is within a configurable threshold of their proximity to a recent high / low, sorted by proximity to that watermark.")
    @ApiResponse(responseCode = "200", description = "List of high / low summaries")
    @GetMapping("/watermark")
    public List<Watermark<Price>> getWatermark(
            @Parameter(description = "Market code (e.g. ASX) defaults to users default market") @RequestParam("market") Optional<String> market,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y); defaults to 1Y") @RequestParam("tenor") @JsonDeserialize(using = TenorDeserializer.class) Optional<Tenor> tenor,
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
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y); defaults to 1Y") @RequestParam("tenor") @JsonDeserialize(using = TenorDeserializer.class) Optional<Tenor> tenor,
            @Parameter(description = "direction (e.g. HIGH or LOW, defaults to High)") @RequestParam("direction") Optional<Direction> optDirection
    ) {
        return authorize(u -> {
            return u.resolveCode(code, (m, c) -> {
                Direction direction = optDirection.orElse(Direction.RISING);
                DateRange dateRange = tenor.orElse(_1Y).endNow();
                return priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(m, c, dateRange.start(), dateRange.end())
                        .stream().reduce(new Watermark<>(direction, p -> p.getClose().doubleValue()), Watermark::update, oops());
            });
        });
    }

    @Operation(summary = "Get trending stocks", description = "Calculates the current trend state for every stock in the given market and optionally filters by sentiment.")
    @ApiResponse(responseCode = "200", description = "List of trend summaries")
    @GetMapping("/trending/{code}")
    public List<Trend> getTrending(
            @Parameter(description = "ASX code") @PathVariable("code") String code) {
        Function<Double, Optional<Trend>> trendStream = TrendCalculator.trendStream();
        return priceRepository.findByCodeOrderByDateAsc(code).stream()
                .map(Price::getClose)
                .map(BigDecimal::doubleValue)
                .map(trendStream)
                .flatMap(Optional::stream)
                .toList();
    }

    @Operation(summary = "Get trending stocks", description = "Calculates the current trend state for every stock in the given market and optionally filters by sentiment.")
    @ApiResponse(responseCode = "200", description = "List of trend summaries")
    @GetMapping("/trending")
    public List<TrendSummary> getTrending(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") Optional<String> mkt,
            @Parameter(description = "Trend state filter (e.g. BULL, BEAR)") @RequestParam("sentiment") Optional<String> sentiment) {
        String market = mkt.orElse("ASX");
        return priceRepository.findDistinctCodesByMarket(market).stream()
                .flatMap(code -> {
                    double[] prices = priceRepository.findByMarketAndCodeOrderByDateAsc(market, code)
                            .stream()
                            .mapToDouble(p -> p.getClose().doubleValue())
                            .toArray();
                    return TrendCalculator.trend(prices, 8, 21, 200)
                            .stream()
                            .map(trend -> new TrendSummary(market, code, trend.price(), trend));
                })
                .filter(ts -> sentiment.isEmpty() || ts.getTrendState().trendState().name().equals(sentiment.get()))
                .sorted(Comparator.comparing(TrendSummary::getCode))
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get trending stocks", description = "Calculates the current trend state for every stock in the given market and optionally filters by sentiment.")
    @ApiResponse(responseCode = "200", description = "List of trend summaries")
    @GetMapping("/trending/all")
    public Map<String, TrendSummary> getAllTrending(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") Optional<String> mkt) {
        return getTrending(mkt, empty()).stream()
                .map(ts -> Map.entry(ts.getMarket() + ":" + ts.getCode(), ts))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    @Operation(summary = "Get all prices", description = "Returns OHLCV prices ordered by date ascending, optionally filtered by date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of prices")
    @GetMapping()
    public Map<String,List<Price>> getAllPrices(
            @Parameter(description = "Market code (e.g. ASX)") @RequestParam("market") Optional<String> market,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            String mkt = market.orElse("ASX");
            Optional<DateRange> dateRange = DateRange.resolve(tenor, startFrom, endAt);
            return dateRange
                            .map(dr -> priceRepository.findByMarketAndDateBetweenOrderByDateAsc(mkt, dr.start(), dr.end()))
                            .orElseGet(() -> priceRepository.findByMarketOrderByDateAsc(mkt))
                    .stream().collect(Collectors.groupingBy(Price::getCode));
        });
    }

    @Operation(summary = "Get trend for a security", description = "Returns TrendSummary for the given code, date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of prices")
    @GetMapping("/trend/{code}")
    public List<Dated<Trend>> getTrend(
            @Parameter(description = "Security code (e.g. BHP)") @PathVariable("code") String code,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<Tenor> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") @JsonDeserialize(using = LocalDateDeserializer.class) Optional<LocalDate> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") @JsonDeserialize(using = LocalDateDeserializer.class) Optional<LocalDate> endAt) {
        return authorize(u -> {
            Function<Double, Optional<Trend>> trendStream = TrendCalculator.trendStream();
            DateRange dateRange = tenor.map(t -> startFrom.map(t::startFrom).orElseGet(() -> endAt.map(t::endAt).orElse(t.endNow())))
                    .or(() -> startFrom.flatMap(s -> endAt.map(e -> DateRange.of(s, e)))).orElse(_1Y.endNow());
            return u.resolveCode(code).map((mkt, cd) -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(mkt, cd, dateRange.start(), dateRange.end()).stream())
                    .map(p -> trendStream.apply(p.close()).map(t -> Dated.withPayload(p.getDate(), t)))
                    .flatMap(Optional::stream)
                    .toList();
        });
    }

    @Operation(summary = "Get trend for a list securities", description = "Returns Trend and a summary for the given code, date range or tenor.")
    @ApiResponse(responseCode = "200", description = "a map of Trend objects plus a summary value")
    @PostMapping("/trend")
    public TrendAggregation postTrend(
            @Parameter(description = "Security code (e.g. BHP or ASX:BHP)") @RequestBody List<List<String>> codes,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") @JsonDeserialize(using = TenorDeserializer.class) Optional<Tenor> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") @JsonDeserialize(using = LocalDateDeserializer.class) Optional<LocalDate> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") @JsonDeserialize(using = LocalDateDeserializer.class) Optional<LocalDate> endAt) {
        return authorize(u -> streamCodes(codes, c -> u.resolveCode(c).map((mkt, code) -> new CodeAware<>(mkt, code, getTrend(c, tenor, startFrom, endAt))))
                .reduce(new TrendAggregation(),
                        (a, ca) -> {
                            String code = ca.getCode();
                            ca.getPayload().forEach(d -> {
                                a.aggregate(code, d.date(), d.payload());
                            });
                            return a;
                        },
                        oops()));
    }

    public record TrendAggregation(
            @JsonIgnore SortedMap<LocalDate, Map<String, Trend>> trends) {
        public TrendAggregation() {
            this(new TreeMap<>());
        }
        public void aggregate(String code, LocalDate date, Trend t) {
            trends.computeIfAbsent(date, k -> new HashMap<>());
            trends.get(date).put(code, t);
        }
        @JsonProperty("t")
        public Map<String, Trend> getTrends() {
            return trends.isEmpty() ? emptyMap() : trends.lastEntry().getValue();
        }

        @JsonProperty("s")
        public Optional<MapEntry<LocalDate, Double>> getSummary() {
            List<Map.Entry<LocalDate, Double>> latest = trends.entrySet().stream()
                    .flatMap(e -> e.getValue().values().stream()
                            .map(Trend::trendState)
                            .mapToDouble(TrendState::value).average().stream().mapToObj(d -> Map.entry(e.getKey(), d)))
                    .toList();
            return latest.isEmpty() ? empty() : Optional.of(new MapEntry<>(latest.getLast()));
        }
    }

}
