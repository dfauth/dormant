package io.github.dfauth.dormant.model.repository;

import io.github.dfauth.dormant.model.UserSetting;
import io.github.dfauth.dormant.model.UserSettingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSettingRepository extends JpaRepository<UserSetting, UserSettingId> {

    List<UserSetting> findByIdUserId(Long userId);
}
