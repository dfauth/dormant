package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dfauth.ta.RelativeStrengthIndex;
import io.github.dfauth.ta.TrendVelocity;
import io.github.dfauth.trade.model.*;
import io.github.dfauth.trade.repository.PriceRepository;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.dfauth.trycatch.Utils.oops;
import static io.github.dfauth.trycatch.Utils.right;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;

@Slf4j
@RestController
@RequestMapping("/api/ta")
@Tag(name = "ATR", description = "Average True Range data")
public class TechnicalAnalysisController extends BaseController {

    private final PriceRepository priceRepository;

    public TechnicalAnalysisController(PriceRepository priceRepository, UserService userService) {
        super(userService);
        this.priceRepository = priceRepository;
    }

    @Operation(summary = "Get RSI for all securities in a market", description = "Returns the latest RSI for every code in the market, excluding codes with insufficient price history.")
    @ApiResponse(responseCode = "200", description = "List of RSI values")
    @GetMapping("/rsi")
    public List<CodeAware<Double>> getRsiAll(
            @Parameter(description = "Market (default: user's default market)") @RequestParam("market") Optional<String> optMarket,
            @Parameter(description = "RSI period (default 14)") @RequestParam("period") Optional<Integer> optPeriod,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            String market = optMarket.orElse(u.getDefaultMarket());
            int period = optPeriod.orElse(14);
            Optional<DateRange> dateRange = DateRange.resolve(empty(), empty(), endAt);
            return priceRepository.findDistinctCodesByMarket(market).stream()
                    .map(code -> {
                        Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(period);
                        Optional<Double> last = dateRange
                                .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(market, code, dr.start(), dr.end()))
                                .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(market, code))
                                .stream()
                                .flatMap(p -> rsi.apply(p.getClose().doubleValue()).stream())
                                .reduce((a, b) -> b);
                        return new CodeAware<>(market, code, last.orElse(Double.NaN));
                    })
                    .filter(ca -> !Double.isNaN(ca.getPayload()))
                    .collect(Collectors.toList());
        });
    }

    @Operation(summary = "Get RSI for a security", description = "Returns the latest RSI value for the security using Wilder's smoothing.")
    @ApiResponse(responseCode = "200", description = "RSI value")
    @GetMapping("/rsi/{code}")
    public CodeAware<Double> getRsi(
            @Parameter(description = "Security code (e.g. BHP or ASX:BHP)") @PathVariable("code") String marketCodeString,
            @Parameter(description = "RSI period (default 14)") @RequestParam("period") Optional<Integer> optPeriod,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            Optional<DateRange> dateRange = DateRange.resolve(empty(), empty(), endAt);
            int period = optPeriod.orElse(14);
            Function<Double, Optional<Double>> rsi = RelativeStrengthIndex.rsiStream(period);
            return u.resolveCode(marketCodeString, (mkt, cd) -> {
                Optional<Double> last = dateRange
                        .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(mkt, cd, dr.start(), dr.end()))
                        .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(mkt, cd))
                        .stream()
                        .map(p -> rsi.apply(p.getClose().doubleValue()))
                        .filter(Optional::isPresent)
                        .reduce((a, b) -> b)
                        .flatMap(o -> o);
                return new CodeAware<>(mkt, cd, last.orElse(Double.NaN));
            });
        });
    }

    @Operation(summary = "Get Trend Velocity for a security", description = "Returns the latest Trend Velocity value: RoC(ema(period)) / atr(period).")
    @ApiResponse(responseCode = "200", description = "Trend Velocity value")
    @GetMapping("/tv/{code}/{period}")
    public Optional<TV> getTrendVelocity(
            @Parameter(description = "Security code (e.g. BHP or ASX:BHP)") @PathVariable("code") String marketCodeString,
            @Parameter(description = "Period for trend velocity calculation") @PathVariable("period") int period,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            Optional<DateRange> dateRange = DateRange.resolve(empty(), empty(), endAt);
            Function<Price, Optional<TrendVelocity.TrendVelocityRecord>> tv = TrendVelocity.trendVelocityStream(period)::apply;
            return u.resolveCode(marketCodeString, (mkt, cd) -> {
                Optional<TV> last = dateRange
                        .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(mkt, cd, dr.start(), dr.end()))
                        .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(mkt, cd))
                        .stream()
                        .flatMap(p -> tv.apply(p).map(d -> new TV(cd, p.getDate(), d)).stream())
                        .reduce(right());
                return last;
            });
        });
    }

    @Operation(summary = "Get prices for a security", description = "Returns OHLCV prices ordered by date ascending, optionally filtered by date range or tenor.")
    @ApiResponse(responseCode = "200", description = "List of prices")
    @GetMapping("/ema/{code}")
    public CodeAware<Map<Integer, EMA>> getEma(
            @Parameter(description = "Security code (e.g. BHP or ASX:BHP)") @PathVariable("code") String marketCodeString,
            @Parameter(description = "ema periods (e.g. 8,21,200)") @RequestParam("periods") Optional<String> optPeriods,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            Optional<DateRange> dateRange = DateRange.resolve(empty(), empty(), endAt);
            int[] periods = optPeriods.map(str -> asList(str.split(",")).stream().mapToInt(Integer::parseInt).toArray()).orElseGet(() -> EMA.DEFAULT_PERIODS);
            Function<Price, Optional<EMA>[]> f = EMA.createN(periods);
            return u.resolveCode(marketCodeString, (mkt, cd) -> dateRange
                            .map(dr -> priceRepository.findByMarketAndCodeAndDateBetweenOrderByDateAsc(mkt, cd, dr.start(), dr.end()))
                            .orElseGet(() -> priceRepository.findByMarketAndCodeOrderByDateAsc(mkt, cd))
                            .stream()
                            .map(f)
                            .reduce(new CodeAware<Map<Integer,EMA>>(mkt, cd, new HashMap<>()), (ca, o) -> {
                                for(int i=0; i<o.length; i++) {
                                    CodeAware<Map<Integer, EMA>> finalCa = ca;
                                    int finalI = i;
                                    o[i].ifPresent(ema -> finalCa.getPayload().put(periods[finalI], ema));
                                }
                                return ca;
                            }, oops()));
        });
    }

    public record TV(String code, LocalDate date, @JsonIgnore TrendVelocity.TrendVelocityRecord tvr) implements TrendVelocity {
        @Override
        public int getPeriod() {
            return tvr.period();
        }

        @Override
        public double getEma() {
            return tvr.ema();
        }

        @Override
        public double getRoc() {
            return tvr.roc();
        }

        @Override
        public double getAtr() {
            return tvr.atr();
        }

        @Override
        public double getTv() {
            return tvr.tv();
        }
    }
}
