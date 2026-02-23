package io.github.dfauth.trade.controller;

import io.github.dfauth.trade.model.UserSetting;
import io.github.dfauth.trade.model.UserSettingId;
import io.github.dfauth.trade.repository.UserSettingRepository;
import io.github.dfauth.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@Tag(name = "User Settings", description = "Persist and retrieve user-scoped JSON key-value settings")
public class UserSettingController extends BaseController {

    private final UserSettingRepository userSettingRepository;

    public UserSettingController(UserService userService, UserSettingRepository userSettingRepository) {
        super(userService);
        this.userSettingRepository = userSettingRepository;
    }

    @Operation(summary = "Get all settings", description = "Returns all key-value settings for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Map of all settings")
    @GetMapping
    public Map<String, JsonNode> getAllSettings() {
        return authorize(u -> userSettingRepository.findByIdUserId(u.getId()).stream()
                .collect(Collectors.toMap(
                        s -> s.getId().getSettingKey(),
                        UserSetting::getValue
                )));
    }

    @Operation(summary = "Get a setting by key", description = "Returns the JSON value stored under the given key for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setting value"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    @GetMapping("/{key}")
    public ResponseEntity<JsonNode> getSetting(
            @Parameter(description = "Setting key") @PathVariable("key") String key) {
        return authorize(u -> userSettingRepository.findById(new UserSettingId(u.getId(), key))
                .map(s -> ResponseEntity.ok(s.getValue()))
                .orElse(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Create or update a setting", description = "Stores the request body JSON under the given key for the authenticated user. Creates or overwrites.")
    @ApiResponse(responseCode = "200", description = "Setting saved")
    @PostMapping("/{key}")
    public ResponseEntity<JsonNode> upsertSetting(
            @Parameter(description = "Setting key") @PathVariable("key") String key,
            @RequestBody JsonNode value) {
        return authorize(u -> {
            UserSetting setting = UserSetting.builder()
                    .id(new UserSettingId(u.getId(), key))
                    .value(value)
                    .build();
            userSettingRepository.save(setting);
            return ResponseEntity.ok(value);
        });
    }

    @Operation(summary = "Delete a setting", description = "Deletes the setting stored under the given key for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Setting deleted"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteSetting(
            @Parameter(description = "Setting key") @PathVariable("key") String key) {
        return authorize(u -> {
            UserSettingId id = new UserSettingId(u.getId(), key);
            if (!userSettingRepository.existsById(id)) {
                return ResponseEntity.<Void>notFound().build();
            }
            userSettingRepository.deleteById(id);
            return ResponseEntity.<Void>noContent().build();
        });
    }
}
