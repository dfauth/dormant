package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.CodeAware;
import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.EMA;
import io.github.dfauth.trade.model.Price;
import io.github.dfauth.trade.repository.PriceRepository;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;

import static io.github.dfauth.trycatch.Utils.oops;
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
}
