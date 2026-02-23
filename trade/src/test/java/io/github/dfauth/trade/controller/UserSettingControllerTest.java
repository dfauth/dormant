package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.repository.UserRepository;
import io.github.dfauth.trade.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserSettingControllerTest {

    private static final String GOOGLE_ID = "google-settings-123";
    private static final String OTHER_GOOGLE_ID = "google-settings-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingRepository userSettingRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userSettingRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .googleId(GOOGLE_ID)
                .email("settings@example.com")
                .name("Settings Tester")
                .build());
        userRepository.save(User.builder()
                .googleId(OTHER_GOOGLE_ID)
                .email("other@example.com")
                .name("Other User")
                .build());
    }

    // --- authentication ---

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/settings (all) ---

    @Test
    void getAllSettings_empty_returnsEmptyMap() throws Exception {
        mockMvc.perform(get("/api/settings")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void getAllSettings_returnsOnlyCurrentUserSettings() throws Exception {
        // save a setting for the test user
        mockMvc.perform(post("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"dark\""))
                .andExpect(status().isOk());

        // save a setting for the other user
        mockMvc.perform(post("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(OTHER_GOOGLE_ID).claim("email", "other@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"light\""))
                .andExpect(status().isOk());

        // the test user should only see their own setting
        mockMvc.perform(get("/api/settings")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    @Test
    void getAllSettings_multipleKeys_returnsAll() throws Exception {
        String payload = "{\"a\":1,\"b\":2}";
        mockMvc.perform(post("/api/settings/dashboard")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"dark\""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/settings")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.dashboard.a").value(1))
                .andExpect(jsonPath("$.dashboard.b").value(2));
    }

    // --- GET /api/settings/{key} ---

    @Test
    void getSetting_existing_returnsValue() throws Exception {
        String payload = "{\"color\":\"blue\",\"size\":42}";
        mockMvc.perform(post("/api/settings/prefs")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/settings/prefs")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("blue"))
                .andExpect(jsonPath("$.size").value(42));
    }

    @Test
    void getSetting_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/settings/missing")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/settings/{key} ---

    @Test
    void upsertSetting_create_returnsValue() throws Exception {
        mockMvc.perform(post("/api/settings/locale")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"en-AU\""))
                .andExpect(status().isOk())
                .andExpect(content().json("\"en-AU\""));
    }

    @Test
    void upsertSetting_update_overwritesPreviousValue() throws Exception {
        mockMvc.perform(post("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"dark\""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"light\""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/settings/theme")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().json("\"light\""));
    }

    @Test
    void upsertSetting_complexJson_roundtrips() throws Exception {
        String payload = "{\"columns\":[\"date\",\"code\",\"price\"],\"pageSize\":50,\"filters\":{\"market\":\"ASX\"}}";
        mockMvc.perform(post("/api/settings/table-config")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/settings/table-config")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.filters.market").value("ASX"))
                .andExpect(jsonPath("$.columns", hasSize(3)));
    }

    // --- DELETE /api/settings/{key} ---

    @Test
    void deleteSetting_existing_returns204() throws Exception {
        mockMvc.perform(post("/api/settings/toDelete")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/settings/toDelete")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/settings/toDelete")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSetting_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/settings/nonexistent")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSetting_doesNotAffectOtherUsersSettings() throws Exception {
        // other user saves a setting
        mockMvc.perform(post("/api/settings/shared-key")
                        .with(oidcLogin().idToken(t -> t.subject(OTHER_GOOGLE_ID).claim("email", "other@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"other-value\""))
                .andExpect(status().isOk());

        // test user tries to delete — should 404 (they have no such key)
        mockMvc.perform(delete("/api/settings/shared-key")
                        .with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", "settings@example.com"))))
                .andExpect(status().isNotFound());

        // other user's setting should still be there
        mockMvc.perform(get("/api/settings/shared-key")
                        .with(oidcLogin().idToken(t -> t.subject(OTHER_GOOGLE_ID).claim("email", "other@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().json("\"other-value\""));
    }
}
