package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Consensus;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.model.Valuation;
import io.github.dfauth.trade.repository.UserRepository;
import io.github.dfauth.trade.repository.ValuationRepository;
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
class ValuationControllerTest {

    private static final String MARKET = "ASX";
    private static final String CODE = "CBA";
    private static final String GOOGLE_ID = "google-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValuationRepository valuationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        valuationRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("test@example.com")
                .name("Test User")
                .build());
    }

    private Valuation sampleValuation(LocalDate date) {
        return Valuation.builder()
                .market(MARKET)
                .code(CODE)
                .date(date)
                .consensus(Consensus.BUY)
                .buy(12)
                .hold(5)
                .sell(2)
                .target(new BigDecimal("105.500000"))
                .build();
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/valuations/" + CODE).param("market", MARKET))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createValuation() throws Exception {
        Valuation valuation = sampleValuation(LocalDate.of(2024, 3, 1));

        mockMvc.perform(post("/api/valuations")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valuation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.market").value(MARKET))
                .andExpect(jsonPath("$.code").value(CODE))
                .andExpect(jsonPath("$.consensus").value("BUY"))
                .andExpect(jsonPath("$.buy").value(12))
                .andExpect(jsonPath("$.hold").value(5))
                .andExpect(jsonPath("$.sell").value(2))
                .andExpect(jsonPath("$.target").value(105.5))
                .andExpect(jsonPath("$.date").value("2024-03-01"));

        assertEquals(1, valuationRepository.count());
    }

    @Test
    void createValuation_upsertUpdatesExistingRecord() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 3, 1)));

        Valuation updated = sampleValuation(LocalDate.of(2024, 3, 1));
        updated.setConsensus(Consensus.STRONG_BUY);
        updated.setBuy(18);
        updated.setTarget(new BigDecimal("112.000000"));

        mockMvc.perform(post("/api/valuations")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consensus").value("STRONG_BUY"))
                .andExpect(jsonPath("$.buy").value(18))
                .andExpect(jsonPath("$.target").value(112.0));

        assertEquals(1, valuationRepository.count());
    }

    @Test
    void createBatch() throws Exception {
        List<Valuation> valuations = List.of(
                sampleValuation(LocalDate.of(2024, 1, 1)),
                sampleValuation(LocalDate.of(2024, 2, 1)),
                sampleValuation(LocalDate.of(2024, 3, 1))
        );

        mockMvc.perform(post("/api/valuations/batch")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valuations)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(3));

        assertEquals(3, valuationRepository.count());
    }

    @Test
    void createBatch_skipsDuplicates() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 1, 1)));

        List<Valuation> valuations = List.of(
                sampleValuation(LocalDate.of(2024, 1, 1)),
                sampleValuation(LocalDate.of(2024, 2, 1))
        );

        mockMvc.perform(post("/api/valuations/batch")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valuations)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(1));

        assertEquals(2, valuationRepository.count());
    }

    @Test
    void getValuations_byMarketAndCode() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 3, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 1, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 2, 1)));

        mockMvc.perform(get("/api/valuations/" + CODE)
                        .param("market", MARKET)
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].date").value("2024-01-01"))
                .andExpect(jsonPath("$[1].date").value("2024-02-01"))
                .andExpect(jsonPath("$[2].date").value("2024-03-01"));
    }

    @Test
    void getValuations_withDateRange() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 1, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 2, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 3, 1)));

        mockMvc.perform(get("/api/valuations/" + CODE)
                        .param("market", MARKET)
                        .param("startFrom", "20240101")
                        .param("endAt", "20240215")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2024-01-01"))
                .andExpect(jsonPath("$[1].date").value("2024-02-01"));
    }

    @Test
    void getValuations_withTenor() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.now().minusMonths(2)));
        valuationRepository.save(sampleValuation(LocalDate.now().minusMonths(8)));

        mockMvc.perform(get("/api/valuations/" + CODE)
                        .param("market", MARKET)
                        .param("tenor", "6M")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getLatestValuation() throws Exception {
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 1, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 2, 1)));
        valuationRepository.save(sampleValuation(LocalDate.of(2024, 3, 1)));

        mockMvc.perform(get("/api/valuations/" + CODE + "/latest")
                        .param("market", MARKET)
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2024-03-01"));
    }

    @Test
    void getLatestValuation_notFound() throws Exception {
        mockMvc.perform(get("/api/valuations/" + CODE + "/latest")
                        .param("market", MARKET)
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isNotFound());
    }
}
