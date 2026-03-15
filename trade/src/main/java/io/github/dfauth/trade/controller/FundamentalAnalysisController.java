package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Revenue;
import io.github.dfauth.trade.service.RevenueService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/fa")
@Tag(name = "fundamental", description = "fundamental data analysis")
public class FundamentalAnalysisController extends BaseController {

    private final RevenueService revenueService;

    public FundamentalAnalysisController(UserService userService, RevenueService revenueService) {
        super(userService);
        this.revenueService = revenueService;
    }

    @Operation(
            summary = "Record revenue snapshot",
            description = "Persists revenue data for the given security. " +
                    "The path parameter must be in the form MARKET:CODE (e.g. ASX:CBA).")
    @ApiResponse(responseCode = "201", description = "Snapshot recorded")
    @ApiResponse(responseCode = "400", description = "Invalid market:code format")
    @PostMapping("/{marketCode}")
    @CrossOrigin("https://sharetrading.westpac.com.au")
    public ResponseEntity<Integer> record(
            @Parameter(description = "Market and code separated by colon, e.g. ASX:CBA")
            @PathVariable("marketCode") String marketCode,
            @RequestBody FundamentalData data) {

        String[] parts = marketCode.split(":");
        if (parts.length != 2) {
            return ResponseEntity.badRequest().build();
        }
        String market = parts[0];
        String code = parts[1];

        List<Revenue> revenues = data.getRevenue().stream()
                .map(entry -> Revenue.builder()
                        .market(market)
                        .code(code)
                        .date(entry.getLocalDate())
                        .amount(BigDecimal.valueOf(entry.getAmount()))
                        .build())
                .toList();

        List<Revenue> saved = revenueService.upsertAll(revenues);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.size());
    }

    @Data
    @NoArgsConstructor
    public static class FundamentalData {
        private List<RevenueEntry> revenue;
    }

    @Data
    @NoArgsConstructor
    public static class RevenueEntry {
        private String periodEnd;
        private Double amount;

        public LocalDate getLocalDate() {
            return Optional.of(periodEnd.split("/")).filter(arr -> arr.length==2).map(arr -> LocalDate.of(Integer.valueOf(arr[0]), Integer.valueOf(arr[1])+1, 1).plusDays(-1)).orElseThrow();
        }
    }
}
