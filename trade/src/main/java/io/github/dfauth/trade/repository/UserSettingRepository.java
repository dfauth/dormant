package io.github.dfauth.trade.repository;

import io.github.dfauth.trade.model.UserSetting;
import io.github.dfauth.trade.model.UserSettingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSettingRepository extends JpaRepository<UserSetting, UserSettingId> {

    List<UserSetting> findByIdUserId(Long userId);
}
