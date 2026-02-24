package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Payment;
import io.github.dfauth.trade.model.TransactionType;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.PaymentRepository;
import io.github.dfauth.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    private static final String GOOGLE_ID = "google-payments-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("payments-test@example.com")
                .name("Payments Test User")
                .build());
    }

    private Payment samplePayment(TransactionType type, String code) {
        return Payment.builder()
                .transactionType(type)
                .date(LocalDate.of(2024, 6, 15))
                .detail("Test payment detail")
                .value(new BigDecimal("500.00"))
                .balance(new BigDecimal("10000.00"))
                .side("CREDIT")
                .code(code)
                .build();
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayment_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePayment(TransactionType.DIV, "BHP"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DIV"))
                .andExpect(jsonPath("$.code").value("BHP"))
                .andExpect(jsonPath("$.detail").value("Test payment detail"));

        assertEquals(1, paymentRepository.count());
    }

    @Test
    void createPayment_withOptionalFields() throws Exception {
        Payment payment = Payment.builder()
                .transactionType(TransactionType.DIV)
                .date(LocalDate.of(2024, 3, 20))
                .detail("Dividend with ex-date")
                .value(new BigDecimal("120.50"))
                .balance(new BigDecimal("8000.00"))
                .side("CREDIT")
                .code("ANZ")
                .contractNumber("CTR-2024-001")
                .exDividendDate(LocalDate.of(2024, 3, 1))
                .build();

        mockMvc.perform(post("/api/payments")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractNumber").value("CTR-2024-001"))
                .andExpect(jsonPath("$.exDividendDate").isNotEmpty());
    }

    @Test
    void createBatch_persistsAllPayments() throws Exception {
        List<Payment> payments = List.of(
                samplePayment(TransactionType.DIV, "BHP"),
                samplePayment(TransactionType.INT, null),
                samplePayment(TransactionType.PAYMENT, null)
        );

        mockMvc.perform(post("/api/payments/batch")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payments)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)));

        assertEquals(3, paymentRepository.count());
    }

    @Test
    void getPayments_returnsAllForUser() throws Exception {
        Payment p1 = samplePayment(TransactionType.DIV, "BHP");
        Payment p2 = samplePayment(TransactionType.INT, null);
        p1.setUserId(testUser.getId());
        p2.setUserId(testUser.getId());
        paymentRepository.saveAll(List.of(p1, p2));

        mockMvc.perform(get("/api/payments")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPayments_filteredByType() throws Exception {
        Payment div = samplePayment(TransactionType.DIV, "BHP");
        Payment fee = samplePayment(TransactionType.PAYMENT, null);
        div.setUserId(testUser.getId());
        fee.setUserId(testUser.getId());
        paymentRepository.saveAll(List.of(div, fee));

        mockMvc.perform(get("/api/payments").param("type", "DIV")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionType").value("DIV"));
    }

    @Test
    void getPayments_filteredByCode() throws Exception {
        Payment bhp = samplePayment(TransactionType.DIV, "BHP");
        Payment anz = samplePayment(TransactionType.DIV, "ANZ");
        bhp.setUserId(testUser.getId());
        anz.setUserId(testUser.getId());
        paymentRepository.saveAll(List.of(bhp, anz));

        mockMvc.perform(get("/api/payments").param("code", "ANZ")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("ANZ"));
    }

    @Test
    void getById_returnsPayment() throws Exception {
        Payment payment = samplePayment(TransactionType.INT, null);
        payment.setUserId(testUser.getId());
        Payment saved = paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/" + saved.getId())
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.transactionType").value("INT"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/payments/99999")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayments_doesNotReturnOtherUsersPayments() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-google-456")
                .email("other@example.com")
                .name("Other User")
                .build());
        Payment otherPayment = samplePayment(TransactionType.DIV, "CBA");
        otherPayment.setUserId(other.getId());
        paymentRepository.save(otherPayment);

        mockMvc.perform(get("/api/payments")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "payments-test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
