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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
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

    private org.springframework.test.web.servlet.ResultActions postDepth(String marketCode) throws Exception {
        return mockMvc.perform(post("/api/depth/" + marketCode)
                .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDepth())));
    }

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
}
