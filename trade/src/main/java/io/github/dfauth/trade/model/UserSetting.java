package io.github.dfauth.trade.model;

import io.github.dfauth.trade.utils.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSetting {

    @EmbeddedId
    private UserSettingId id;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "setting_value", nullable = false, columnDefinition = "CLOB")
    private JsonNode value;
}
