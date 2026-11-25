package io.github.dfauth.dormant.model.repository;

import io.github.dfauth.dormant.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, UserSettingId> {

    List<UserSetting> findByIdUserId(Long userId);
}
