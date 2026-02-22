package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.TradeRepository;
import io.github.dfauth.trade.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PositionControllerTest {

    private static final String MARKET = "ASX";
    private static final String GOOGLE_ID = "google-pos-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("pos@example.com")
                .name("Position Tester")
                .defaultMarket(MARKET)
                .build());

        // BHP: open long position (buy only)
        tradeRepository.save(Trade.builder()
                .date(LocalDate.of(2024, 1, 15))
                .market(MARKET).code("BHP").side(Side.BUY)
                .size(100).price(new BigDecimal("30.00")).cost(new BigDecimal("3000.00"))
                .confirmationId("BHP-001").userId(testUser.getId()).build());

        // CBA: closed long position (buy then sell)
        tradeRepository.save(Trade.builder()
                .date(LocalDate.of(2024, 1, 10))
                .market(MARKET).code("CBA").side(Side.BUY)
                .size(100).price(new BigDecimal("80.00")).cost(new BigDecimal("8000.00"))
                .confirmationId("CBA-001").userId(testUser.getId()).build());

        tradeRepository.save(Trade.builder()
                .date(LocalDate.of(2024, 2, 10))
                .market(MARKET).code("CBA").side(Side.SELL)
                .size(100).price(new BigDecimal("90.00")).cost(new BigDecimal("9000.00"))
                .confirmationId("CBA-002").userId(testUser.getId()).build());
    }

    // --- authentication ---

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/positions"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/positions (all positions) ---

    @Test
    void getPositions_returnsAllPositions() throws Exception {
        mockMvc.perform(get("/api/positions")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPositions_includesOpenAndClosedPositions() throws Exception {
        mockMvc.perform(get("/api/positions")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'BHP' && @.open == true)]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CBA' && @.open == false)]").exists());
    }

    // --- GET /api/positions/closed ---

    @Test
    void getClosedPositions_returnsOnlyClosedPositions() throws Exception {
        mockMvc.perform(get("/api/positions/closed")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("CBA"))
                .andExpect(jsonPath("$[0].open").value(false));
    }

    @Test
    void getClosedPositions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/positions/closed"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/positions/market/{market} ---

    @Test
    void getPositionsByMarket_returnsAllPositionsInMarket() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET)
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPositionsByMarket_unknownMarket_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/positions/market/NYSE")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/positions/market/{market}/code/{code} ---

    @Test
    void getPositionsByCode_returnsAllPositionsForCode() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/code/CBA")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("CBA"))
                .andExpect(jsonPath("$[0].open").value(false));
    }

    @Test
    void getPositionsByCode_openPosition_hasTrades() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/code/BHP")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trades", hasSize(1)));
    }

    @Test
    void getPositionsByCode_closedPosition_hasBothTrades() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/code/CBA")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trades", hasSize(2)));
    }

    @Test
    void getPositionsByCode_unknownCode_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/code/UNKNOWN")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/positions/market/{market}/performance ---

    @Test
    void getPerformanceStats_returnsStatsForClosedPositions() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/performance")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClosedPositions").value(1))
                .andExpect(jsonPath("$.wins").value(1))
                .andExpect(jsonPath("$.losses").value(0))
                .andExpect(jsonPath("$.winRate").value(1.0));
    }

    @Test
    void getPerformanceStats_unknownMarket_returnsZeroStats() throws Exception {
        mockMvc.perform(get("/api/positions/market/NYSE/performance")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "pos@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClosedPositions").value(0))
                .andExpect(jsonPath("$.wins").value(0))
                .andExpect(jsonPath("$.winRate").value(0.0));
    }

    @Test
    void getPerformanceStats_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/positions/market/" + MARKET + "/performance"))
                .andExpect(status().isUnauthorized());
    }
}
