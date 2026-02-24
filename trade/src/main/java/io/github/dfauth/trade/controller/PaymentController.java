package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.DateRange;
import io.github.dfauth.trade.model.Payment;
import io.github.dfauth.trade.model.TransactionType;
import io.github.dfauth.trade.service.PaymentService;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Record and retrieve payment transactions")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    public PaymentController(UserService userService, PaymentService paymentService) {
        super(userService);
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a payment", description = "Persists a single payment transaction.")
    @ApiResponse(responseCode = "201", description = "Payment created")
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        return authorize(u -> {
            Payment saved = paymentService.create(payment, u.getId());
            log.info("Persisted payment: type={}, date={}, value={}", saved.getTransactionType(), saved.getDate(), saved.getValue());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @Operation(summary = "Create multiple payments", description = "Persists a batch of payment transactions.")
    @ApiResponse(responseCode = "201", description = "All payments created")
    @PostMapping("/batch")
    public ResponseEntity<List<Payment>> createPayments(@RequestBody List<Payment> payments) {
        return authorize(u -> {
            List<Payment> saved = paymentService.createBatch(payments, u.getId());
            log.info("Persisted {} payments", saved.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @Operation(summary = "Get payments", description = "Returns the authenticated user's payments, optionally filtered by code, transaction type, tenor, or date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of payments")
    })
    @GetMapping
    public List<Payment> getPayments(
            @Parameter(description = "Security code to filter by") @RequestParam("code") Optional<String> code,
            @Parameter(description = "Transaction type to filter by") @RequestParam("type") Optional<TransactionType> transactionType,
            @Parameter(description = "Tenor shorthand for date range (e.g. 6M, 1Y)") @RequestParam("tenor") Optional<String> tenor,
            @Parameter(description = "Start date in YYYYMMDD format") @RequestParam("startFrom") Optional<String> startFrom,
            @Parameter(description = "End date in YYYYMMDD format") @RequestParam("endAt") Optional<String> endAt) {
        return authorize(u -> {
            Optional<DateRange> dateRange = DateRange.resolve(tenor, startFrom, endAt);
            return paymentService.getPayments(u.getId(), code, transactionType, dateRange);
        });
    }

    @Operation(summary = "Get payment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getById(
            @Parameter(description = "Payment ID") @PathVariable("id") Long id) {
        return authorize(u -> paymentService.findById(id, u.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()));
    }
}
