package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.User;
import io.github.dfauth.trade.model.Watchlist;
import io.github.dfauth.trade.model.WatchlistItem;
import io.github.dfauth.trade.repository.UserRepository;
import io.github.dfauth.trade.repository.WatchlistItemRepository;
import io.github.dfauth.trade.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WatchlistControllerTest {

    private static final String MARKET = "ASX";
    private static final String GOOGLE_ID = "google-watchlist-test";
    private static final String EMAIL = "wl-test@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private WatchlistItemRepository watchlistItemRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .googleId(GOOGLE_ID).email(EMAIL).name("WL Test User").build());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req) {
        return req.with(oidcLogin().idToken(t -> t.subject(GOOGLE_ID).claim("email", EMAIL)));
    }

    private String itemsJson(String... codes) throws Exception {
        List<WatchlistItem> items = java.util.Arrays.stream(codes)
                .map(c -> WatchlistItem.builder().market(MARKET).code(c).build())
                .toList();
        return objectMapper.writeValueAsString(items);
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/watchlists")).andExpect(status().isUnauthorized());
    }

    // ── GET /api/watchlists ──────────────────────────────────────────────────

    @Test
    void getWatchlists_returnsEmptyList_whenNone() throws Exception {
        mockMvc.perform(auth(get("/api/watchlists")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getWatchlists_returnsOnlyCurrentUsersWatchlists() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-user").email("other@example.com").name("Other").build());
        watchlistRepository.save(Watchlist.builder().userId(testUser.getId()).name("Mine").build());
        watchlistRepository.save(Watchlist.builder().userId(other.getId()).name("Theirs").build());

        mockMvc.perform(auth(get("/api/watchlists")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Mine"));
    }

    // ── POST /api/watchlists ─────────────────────────────────────────────────

    @Test
    void createWatchlist_returnsCreatedWithEmptyItems() throws Exception {
        mockMvc.perform(auth(post("/api/watchlists"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tech\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tech"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());

        assertEquals(1, watchlistRepository.count());
    }

    // ── POST /api/watchlists/{name} (upsert) ─────────────────────────────────

    @Test
    void batchUpsert_createsNewWatchlistWithItems() throws Exception {
        mockMvc.perform(auth(post("/api/watchlists/Tech"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("WTC", "XRO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tech"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].market").value(MARKET))
                .andExpect(jsonPath("$.items[0].code").value("WTC"))
                .andExpect(jsonPath("$.items[1].code").value("XRO"));

        assertEquals(1, watchlistRepository.count());
        assertEquals(2, watchlistItemRepository.count());
    }

    @Test
    void batchUpsert_replacesItemsOnExistingWatchlist() throws Exception {
        mockMvc.perform(auth(post("/api/watchlists/Tech"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("WTC", "XRO", "AX1")))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(post("/api/watchlists/Tech"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("BHP", "XRO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].code").value("XRO"))
                .andExpect(jsonPath("$.items[1].code").value("BHP"));

        assertEquals(1, watchlistRepository.count());
        assertEquals(2, watchlistItemRepository.count());
    }

    @Test
    void batchUpsert_createsDistinctWatchlistsForDifferentNames() throws Exception {
        mockMvc.perform(auth(post("/api/watchlists/Tech"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("WTC")))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(post("/api/watchlists/Mining"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("BHP")))
                .andExpect(status().isCreated());

        assertEquals(2, watchlistRepository.count());
        assertEquals(2, watchlistItemRepository.count());
    }

    // ── PATCH /api/watchlists/{id} ───────────────────────────────────────────

    @Test
    void renameWatchlist_updatesName() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("Old Name").build());

        mockMvc.perform(auth(patch("/api/watchlists/" + wl.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));

        assertEquals("New Name", watchlistRepository.findById(wl.getId()).orElseThrow().getName());
    }

    @Test
    void renameWatchlist_returns404_forOtherUsersWatchlist() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-rename").email("other-rename@example.com").name("Other").build());
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(other.getId()).name("Theirs").build());

        mockMvc.perform(auth(patch("/api/watchlists/" + wl.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mine\"}"))
                .andExpect(status().isNotFound());

        assertEquals("Theirs", watchlistRepository.findById(wl.getId()).orElseThrow().getName());
    }

    // ── DELETE /api/watchlists/{id} ──────────────────────────────────────────

    @Test
    void deleteWatchlist_returns204_andRemovesWatchlist() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("To Delete").build());

        mockMvc.perform(auth(delete("/api/watchlists/" + wl.getId())))
                .andExpect(status().isNoContent());

        assertEquals(0, watchlistRepository.count());
    }

    @Test
    void deleteWatchlist_cascadesItems() throws Exception {
        mockMvc.perform(auth(post("/api/watchlists/Tech"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson("WTC", "XRO")))
                .andExpect(status().isCreated());

        Watchlist wl = watchlistRepository.findByUserId(testUser.getId()).getFirst();
        mockMvc.perform(auth(delete("/api/watchlists/" + wl.getId())))
                .andExpect(status().isNoContent());

        assertEquals(0, watchlistRepository.count());
        assertEquals(0, watchlistItemRepository.count());
    }

    @Test
    void deleteWatchlist_returns404_forOtherUsersWatchlist() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-delete").email("other-delete@example.com").name("Other").build());
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(other.getId()).name("Not Mine").build());

        mockMvc.perform(auth(delete("/api/watchlists/" + wl.getId())))
                .andExpect(status().isNotFound());

        assertEquals(1, watchlistRepository.count());
    }

    // ── POST /api/watchlists/{id}/items ──────────────────────────────────────

    @Test
    void addItem_returns201_andAddsToWatchlist() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("My List").build());

        mockMvc.perform(auth(post("/api/watchlists/" + wl.getId() + "/items"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"market\":\"ASX\",\"code\":\"BHP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.market").value("ASX"))
                .andExpect(jsonPath("$.code").value("BHP"));

        assertEquals(1, watchlistItemRepository.count());
    }

    @Test
    void addItem_returns409_whenDuplicate() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("My List").build());
        String item = "{\"market\":\"ASX\",\"code\":\"BHP\"}";

        mockMvc.perform(auth(post("/api/watchlists/" + wl.getId() + "/items"))
                        .contentType(MediaType.APPLICATION_JSON).content(item))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(post("/api/watchlists/" + wl.getId() + "/items"))
                        .contentType(MediaType.APPLICATION_JSON).content(item))
                .andExpect(status().isConflict());

        assertEquals(1, watchlistItemRepository.count());
    }

    @Test
    void addItem_returns404_forOtherUsersWatchlist() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-add").email("other-add@example.com").name("Other").build());
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(other.getId()).name("Not Mine").build());

        mockMvc.perform(auth(post("/api/watchlists/" + wl.getId() + "/items"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"market\":\"ASX\",\"code\":\"BHP\"}"))
                .andExpect(status().isNotFound());

        assertEquals(0, watchlistItemRepository.count());
    }

    // ── DELETE /api/watchlists/{id}/items/{itemId} ───────────────────────────

    @Test
    void removeItem_returns204_andDeletesItem() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("My List").build());
        mockMvc.perform(auth(post("/api/watchlists/" + wl.getId() + "/items"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"market\":\"ASX\",\"code\":\"BHP\"}"))
                .andExpect(status().isCreated());

        // id is @JsonIgnore so we fetch it from the repository
        Long itemId = watchlistItemRepository.findAll().getFirst().getId();

        mockMvc.perform(auth(delete("/api/watchlists/" + wl.getId() + "/items/" + itemId)))
                .andExpect(status().isNoContent());

        assertEquals(0, watchlistItemRepository.count());
    }

    @Test
    void removeItem_returns404_whenItemNotInWatchlist() throws Exception {
        Watchlist wl = watchlistRepository.save(
                Watchlist.builder().userId(testUser.getId()).name("My List").build());

        mockMvc.perform(auth(delete("/api/watchlists/" + wl.getId() + "/items/9999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_returns404_forOtherUsersWatchlist() throws Exception {
        User other = userRepository.save(User.builder()
                .googleId("other-remove").email("other-remove@example.com").name("Other").build());
        Watchlist otherWl = watchlistRepository.save(
                Watchlist.builder().userId(other.getId()).name("Not Mine").build());

        // add an item to the other user's watchlist directly
        WatchlistItem item = watchlistItemRepository.save(
                WatchlistItem.builder().watchlist(otherWl).market(MARKET).code("BHP").build());

        mockMvc.perform(auth(delete("/api/watchlists/" + otherWl.getId() + "/items/" + item.getId())))
                .andExpect(status().isNotFound());

        assertEquals(1, watchlistItemRepository.count());
    }
}
