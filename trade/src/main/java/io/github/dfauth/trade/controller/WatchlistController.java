package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.Watchlist;
import io.github.dfauth.trade.model.WatchlistItem;
import io.github.dfauth.trade.repository.WatchlistItemRepository;
import io.github.dfauth.trade.repository.WatchlistRepository;
import io.github.dfauth.trade.service.UserService;
import io.github.dfauth.trade.service.WatchlistService;
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
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/watchlists")
@Tag(name = "Watchlists", description = "Manage user watchlists")
public class WatchlistController extends BaseController {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final WatchlistService watchlistService;

    public WatchlistController(UserService userService, WatchlistRepository watchlistRepository,
                               WatchlistItemRepository watchlistItemRepository, WatchlistService watchlistService) {
        super(userService);
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.watchlistService = watchlistService;
    }

    @Operation(summary = "Get all watchlists for the current user")
    @ApiResponse(responseCode = "200", description = "List of watchlists with their items")
    @GetMapping
    public List<Watchlist> getWatchlists() {
        return authorize(u -> watchlistRepository.findByUserId(u.getId()));
    }

    @Operation(summary = "Get a named watchlist for the current user")
    @ApiResponse(responseCode = "200", description = "the named watchlist items")
    @GetMapping("/{name}")
    public Optional<Watchlist> getWatchlist(@PathVariable("name") String name) {
        return authorize(u -> watchlistRepository.findByUserIdAndName(u.getId(), name));
    }

    @Operation(summary = "upsert watchlist", description = "Creates or replaces a watchlist by name. Existing items are replaced.")
    @ApiResponse(responseCode = "201", description = "Watchlist upserted")
    @PostMapping("/{name}")
    public ResponseEntity<Watchlist> batchUpsert(@PathVariable("name") String name, @RequestBody List<WatchlistItem> batches) {
        return authorize(u -> {
            Watchlist saved = watchlistService.batchUpsert(u.getId(), name, batches);
            log.info("Batch upserted watchlist {} for user {}", name, u.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @Operation(summary = "Create a watchlist")
    @ApiResponse(responseCode = "201", description = "Watchlist created")
    @PostMapping
    public ResponseEntity<Watchlist> createWatchlist(@RequestBody Map<String, String> body) {
        return authorize(u -> {
            Watchlist wl = Watchlist.builder().userId(u.getId()).name(body.get("name")).build();
            Watchlist saved = watchlistRepository.save(wl);
            log.info("Created watchlist '{}' for user {}", saved.getName(), u.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @Operation(summary = "Rename a watchlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Watchlist renamed"),
            @ApiResponse(responseCode = "404", description = "Watchlist not found or not owned by user")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Watchlist> renameWatchlist(
            @Parameter(description = "Watchlist ID") @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return authorize(u -> watchlistRepository.findById(id)
                .filter(wl -> wl.getUserId().equals(u.getId()))
                .map(wl -> {
                    wl.setName(body.get("name"));
                    return ResponseEntity.ok(watchlistRepository.save(wl));
                })
                .orElse(ResponseEntity.<Watchlist>notFound().build()));
    }

    @Operation(summary = "Delete a watchlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Watchlist deleted"),
            @ApiResponse(responseCode = "404", description = "Watchlist not found or not owned by user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchlist(
            @Parameter(description = "Watchlist ID") @PathVariable("id") Long id) {
        return authorize(u -> watchlistRepository.findById(id)
                .filter(wl -> wl.getUserId().equals(u.getId()))
                .<ResponseEntity<Void>>map(wl -> {
                    watchlistRepository.delete(wl);
                    return ResponseEntity.<Void>noContent().build();
                })
                .orElse(ResponseEntity.<Void>notFound().build()));
    }

    @Operation(summary = "Add a security to a watchlist")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added"),
            @ApiResponse(responseCode = "404", description = "Watchlist not found or not owned by user"),
            @ApiResponse(responseCode = "409", description = "Security already in watchlist")
    })
    @PostMapping("/{id}/items")
    public ResponseEntity<WatchlistItem> addItem(
            @Parameter(description = "Watchlist ID") @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return authorize(u -> watchlistRepository.findById(id)
                .filter(wl -> wl.getUserId().equals(u.getId()))
                .<ResponseEntity<WatchlistItem>>map(wl -> {
                    String market = body.get("market");
                    String code = body.get("code");
                    boolean exists = wl.getItems().stream()
                            .anyMatch(i -> i.getMarket().equals(market) && i.getCode().equals(code));
                    if (exists) return ResponseEntity.<WatchlistItem>status(HttpStatus.CONFLICT).build();
                    WatchlistItem item = WatchlistItem.builder().watchlist(wl).market(market).code(code).build();
                    return ResponseEntity.status(HttpStatus.CREATED).<WatchlistItem>body(watchlistItemRepository.save(item));
                })
                .orElse(ResponseEntity.<WatchlistItem>notFound().build()));
    }

    @Operation(summary = "Remove a security from a watchlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Item not found or not in user's watchlist")
    })
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @Parameter(description = "Watchlist ID") @PathVariable("id") Long id,
            @Parameter(description = "Item ID") @PathVariable("itemId") Long itemId) {
        return authorize(u -> watchlistRepository.findById(id)
                .filter(wl -> wl.getUserId().equals(u.getId()))
                .<ResponseEntity<Void>>map(wl -> {
                    int deleted = watchlistItemRepository.deleteByWatchlistIdAndItemId(id, itemId);
                    return deleted > 0
                            ? ResponseEntity.<Void>noContent().build()
                            : ResponseEntity.<Void>notFound().build();
                })
                .orElse(ResponseEntity.<Void>notFound().build()));
    }
}
