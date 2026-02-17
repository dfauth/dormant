package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Price;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.PriceRepository;
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
class PriceControllerTest {

    private static final String MARKET = "ASX";
    private static final String CODE = "BHP";
    private static final String GOOGLE_ID = "google-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        priceRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("test@example.com")
                .name("Test User")
                .build());
    }

    private Price samplePrice(LocalDate date) {
        return Price.builder()
                .market(MARKET)
                .code(CODE)
                .date(date)
                .open(new BigDecimal("42.100000"))
                .high(new BigDecimal("43.500000"))
                .low(new BigDecimal("41.800000"))
                .close(new BigDecimal("43.200000"))
                .volume(1_000_000)
                .build();
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/prices").param("market", MARKET).param("code", CODE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBatch() throws Exception {
        List<Price> prices = List.of(
                samplePrice(LocalDate.of(2024, 1, 2)),
                samplePrice(LocalDate.of(2024, 1, 3)),
                samplePrice(LocalDate.of(2024, 1, 4))
        );

        mockMvc.perform(post("/api/prices/batch")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prices)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.persisted").value(3))
                .andExpect(jsonPath("$.skipped").value(0));

        assertEquals(3, priceRepository.count());
    }

    @Test
    void createBatch_skipsDuplicates() throws Exception {
        priceRepository.save(samplePrice(LocalDate.of(2024, 1, 2)));

        List<Price> prices = List.of(
                samplePrice(LocalDate.of(2024, 1, 2)),
                samplePrice(LocalDate.of(2024, 1, 3))
        );

        mockMvc.perform(post("/api/prices/batch")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prices)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.persisted").value(1))
                .andExpect(jsonPath("$.skipped").value(1));

        assertEquals(2, priceRepository.count());
    }

    @Test
    void getPrices_byMarketAndCode() throws Exception {
        priceRepository.save(samplePrice(LocalDate.of(2024, 1, 3)));
        priceRepository.save(samplePrice(LocalDate.of(2024, 1, 2)));

        mockMvc.perform(get("/api/prices").param("market", MARKET).param("code", CODE)
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2024-01-02"))
                .andExpect(jsonPath("$[1].date").value("2024-01-03"));
    }

    @Test
    void getPrices_withDateRange() throws Exception {
        priceRepository.save(samplePrice(LocalDate.of(2024, 1, 2)));
        priceRepository.save(samplePrice(LocalDate.of(2024, 1, 15)));
        priceRepository.save(samplePrice(LocalDate.of(2024, 2, 1)));

        mockMvc.perform(get("/api/prices")
                        .param("market", MARKET)
                        .param("code", CODE)
                        .param("startFrom", "20240101")
                        .param("endAt", "20240120")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2024-01-02"))
                .andExpect(jsonPath("$[1].date").value("2024-01-15"));
    }

    @Test
    void getPrices_withTenor() throws Exception {
        priceRepository.save(samplePrice(LocalDate.now().minusMonths(1)));
        priceRepository.save(samplePrice(LocalDate.now().minusMonths(8)));

        mockMvc.perform(get("/api/prices")
                        .param("market", MARKET)
                        .param("code", CODE)
                        .param("tenor", "6M")
                        .with(oidcLogin().idToken(token -> token.subject(GOOGLE_ID).claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
