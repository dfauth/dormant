package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dfauth.trade.model.Side;
import io.github.dfauth.trade.model.Trade;
import io.github.dfauth.trade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TradeControllerTest {

    private static final String MARKET = "NYSE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TradeRepository tradeRepository;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
    }

    private Trade sampleTrade(String confirmationId) {
        return Trade.builder()
                .date(LocalDate.of(2024, 3, 15))
                .code("AAPL")
                .market("NYSE")
                .size(new BigDecimal("100"))
                .price(new BigDecimal("172.50"))
                .cost(new BigDecimal("17250.00"))
                .side(Side.BUY)
                .notes("Entry position")
                .confirmationId(confirmationId)
                .build();
    }

    @Test
    void createTrade() throws Exception {
        Trade trade = sampleTrade("CONF-001");

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trade)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmationId").value("CONF-001"))
                .andExpect(jsonPath("$.code").value("AAPL"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.market").value(MARKET));

        assertEquals(1, tradeRepository.count());
    }

    @Test
    void createTrade_duplicateConfirmationId_returnsConflict() throws Exception {
        Trade trade = sampleTrade("CONF-DUP");
        trade.setMarket(MARKET);
        tradeRepository.save(trade);

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTrade("CONF-DUP"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("CONF-DUP")));

        assertEquals(1, tradeRepository.count());
    }

    @Test
    void createBatch() throws Exception {
        List<Trade> trades = List.of(
                sampleTrade("BATCH-001"),
                sampleTrade("BATCH-002"),
                sampleTrade("BATCH-003")
        );

        mockMvc.perform(post("/api/trades/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trades)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].market").value(MARKET));

        assertEquals(3, tradeRepository.count());
    }

    @Test
    void createBatch_skipsDuplicates() throws Exception {
        Trade existing = sampleTrade("EXISTING");
        existing.setMarket(MARKET);
        tradeRepository.save(existing);

        List<Trade> trades = List.of(
                sampleTrade("EXISTING"),
                sampleTrade("NEW-001")
        );

        mockMvc.perform(post("/api/trades/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trades)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].confirmationId").value("NEW-001"));

        assertEquals(2, tradeRepository.count());
    }

    @Test
    void getTradesByMarket() throws Exception {
        Trade nyse = sampleTrade("MKT-001");
        nyse.setMarket("NYSE");
        Trade asx = sampleTrade("MKT-002");
        asx.setMarket("ASX");
        tradeRepository.save(nyse);
        tradeRepository.save(asx);

        mockMvc.perform(get("/api/trades/market/{market}", "NYSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].market").value("NYSE"));
    }

    @Test
    void getByConfirmationId() throws Exception {
        Trade trade = sampleTrade("LOOKUP-001");
        trade.setMarket(MARKET);
        tradeRepository.save(trade);

        mockMvc.perform(get("/api/trades/confirmationId/LOOKUP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AAPL"))
                .andExpect(jsonPath("$.confirmationId").value("LOOKUP-001"));
    }

    @Test
    void getByConfirmationId_notFound() throws Exception {
        mockMvc.perform(get("/api/trades/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
