package com.easywiki.repository;

import com.easywiki.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndFcmToken(Long userId, String fcmToken);

    List<UserDevice> findByUserId(Long userId);
}
