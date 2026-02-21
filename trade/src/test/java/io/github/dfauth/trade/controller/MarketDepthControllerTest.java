package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.MarketDepth;
import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.MarketDepthRepository;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketDepthControllerTest {

    private static final String GOOGLE_ID = "google-depth-test";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MarketDepthRepository marketDepthRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        marketDepthRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("test@example.com")
                .name("Test User")
                .build());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MarketDepth sampleDepth() {
        return MarketDepth.builder()
                .buyers(120)
                .buyerShares(50000)
                .sellers(85)
                .sellerShares(32000)
                .price(new BigDecimal("45.230000"))
                .change(new BigDecimal("0.750000"))
                .volume(215000)
                .build();
    }

    private void saveDepth(String market, String code, LocalDateTime recordedAt) {
        marketDepthRepository.save(MarketDepth.builder()
                .market(market).code(code).recordedAt(recordedAt)
                .buyers(120).buyerShares(50000).sellers(85).sellerShares(32000)
                .price(new BigDecimal("45.230000")).change(new BigDecimal("0.750000")).volume(215000)
                .build());
    }

    private void saveDepth(String market, String code, LocalDateTime recordedAt,
                           int buyers, int buyerShares, int sellers, int sellerShares) {
        marketDepthRepository.save(MarketDepth.builder()
                .market(market).code(code).recordedAt(recordedAt)
                .buyers(buyers).buyerShares(buyerShares).sellers(sellers).sellerShares(sellerShares)
                .price(new BigDecimal("45.230000")).change(new BigDecimal("0.750000")).volume(215000)
                .build());
    }

    private org.springframework.test.web.servlet.ResultActions postDepth(String marketCode) throws Exception {
        return mockMvc.perform(post("/api/depth/" + marketCode)
                .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDepth())));
    }

    // ── POST /{marketCode} ───────────────────────────────────────────────────

    @Test
    void unauthenticatedRequest_isAccepted() throws Exception {
        // /api/depth/** is permitAll — it receives data from external systems without OIDC
        mockMvc.perform(post("/api/depth/ASX:CBA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDepth())))
                .andExpect(status().isCreated());

        assertEquals(1, marketDepthRepository.count());
    }

    @Test
    void record_savesSnapshotAndReturnsAllFields() throws Exception {
        postDepth("ASX:CBA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.market").value("ASX"))
                .andExpect(jsonPath("$.code").value("CBA"))
                .andExpect(jsonPath("$.buyers").value(120))
                .andExpect(jsonPath("$.buyerShares").value(50000))
                .andExpect(jsonPath("$.sellers").value(85))
                .andExpect(jsonPath("$.sellerShares").value(32000))
                .andExpect(jsonPath("$.price").value(45.23))
                .andExpect(jsonPath("$.change").value(0.75))
                .andExpect(jsonPath("$.volume").value(215000))
                .andExpect(jsonPath("$.recordedAt").isNotEmpty());

        assertEquals(1, marketDepthRepository.count());
    }

    @Test
    void record_marketAndCodeComeFromPath_notBody() throws Exception {
        // body has no market/code fields — they must be set from the path variable
        postDepth("ASX:CBA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.market").value("ASX"))
                .andExpect(jsonPath("$.code").value("CBA"));
    }

    @Test
    void record_upcasesMarketAndCode() throws Exception {
        postDepth("asx:cba")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.market").value("ASX"))
                .andExpect(jsonPath("$.code").value("CBA"));
    }

    @Test
    void record_multipleSnapshotsForSameSecurity_allPersisted() throws Exception {
        postDepth("ASX:CBA").andExpect(status().isCreated());
        postDepth("ASX:CBA").andExpect(status().isCreated());
        postDepth("ASX:CBA").andExpect(status().isCreated());

        assertEquals(3, marketDepthRepository.count());
    }

    @Test
    void record_snapshotsForDifferentSecurities_allPersisted() throws Exception {
        postDepth("ASX:CBA").andExpect(status().isCreated());
        postDepth("ASX:BHP").andExpect(status().isCreated());

        assertEquals(2, marketDepthRepository.count());
    }

    @Test
    void record_missingColon_returns400() throws Exception {
        postDepth("ASXCBA").andExpect(status().isBadRequest());

        assertEquals(0, marketDepthRepository.count());
    }

    @Test
    void record_emptyMarket_returns400() throws Exception {
        postDepth(":CBA").andExpect(status().isBadRequest());

        assertEquals(0, marketDepthRepository.count());
    }

    @Test
    void record_emptyCode_returns400() throws Exception {
        postDepth("ASX:").andExpect(status().isBadRequest());

        assertEquals(0, marketDepthRepository.count());
    }

    // ── GET /recent ──────────────────────────────────────────────────────────

    @Test
    void recent_returnsEmptyList_whenNoData() throws Exception {
        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recent_returnsEntryFromToday() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());

        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CBA"))
                .andExpect(jsonPath("$[0].buyers").value(120))
                .andExpect(jsonPath("$[0].buyerShares").value(50000))
                .andExpect(jsonPath("$[0].sellers").value(85))
                .andExpect(jsonPath("$[0].sellerShares").value(32000))
                .andExpect(jsonPath("$[0].date").isNotEmpty());
    }

    @Test
    void recent_excludesEntriesOlderThan3Days() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now().minusDays(4));

        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recent_averagesMultipleEntriesOnSameDay() throws Exception {
        // avg buyers=(100+60)/2=80, buyerShares=(60000+40000)/2=50000,
        //     sellers=(50+30)/2=40, sellerShares=(30000+20000)/2=25000
        saveDepth("ASX", "CBA", LocalDateTime.now(), 100, 60000, 50, 30000);
        saveDepth("ASX", "CBA", LocalDateTime.now(), 60, 40000, 30, 20000);

        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].buyers").value(80))
                .andExpect(jsonPath("$[0].buyerShares").value(50000))
                .andExpect(jsonPath("$[0].sellers").value(40))
                .andExpect(jsonPath("$[0].sellerShares").value(25000));
    }

    @Test
    void recent_separateRowsForDifferentCodes() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());
        saveDepth("ASX", "BHP", LocalDateTime.now());

        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void recent_sortedByDateDescThenCode() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());
        saveDepth("ASX", "BHP", LocalDateTime.now());
        saveDepth("ASX", "CBA", LocalDateTime.now().minusDays(1));

        // today: BHP, CBA (alphabetical); yesterday: CBA
        mockMvc.perform(get("/api/depth/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("BHP"))
                .andExpect(jsonPath("$[1].code").value("CBA"))
                .andExpect(jsonPath("$[2].code").value("CBA"));
    }

    // ── GET /{code} ──────────────────────────────────────────────────────────

    @Test
    void byCode_returnsAllHistory_notJust3Days() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now().minusDays(30));

        mockMvc.perform(get("/api/depth/CBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CBA"));
    }

    @Test
    void byCode_excludesOtherCodes() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());
        saveDepth("ASX", "BHP", LocalDateTime.now());

        mockMvc.perform(get("/api/depth/CBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CBA"));
    }

    @Test
    void byCode_unknownCode_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/depth/UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void byCode_caseInsensitive() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());

        mockMvc.perform(get("/api/depth/cba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void byCode_averagesMultipleEntriesOnSameDay() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now(), 100, 60000, 50, 30000);
        saveDepth("ASX", "CBA", LocalDateTime.now(), 60, 40000, 30, 20000);

        mockMvc.perform(get("/api/depth/CBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].buyers").value(80))
                .andExpect(jsonPath("$[0].buyerShares").value(50000))
                .andExpect(jsonPath("$[0].sellers").value(40))
                .andExpect(jsonPath("$[0].sellerShares").value(25000));
    }

    @Test
    void byCode_multiDayHistoryReturnedAsSeperateRows() throws Exception {
        saveDepth("ASX", "CBA", LocalDateTime.now());
        saveDepth("ASX", "CBA", LocalDateTime.now().minusDays(10));
        saveDepth("ASX", "CBA", LocalDateTime.now().minusDays(20));

        mockMvc.perform(get("/api/depth/CBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
