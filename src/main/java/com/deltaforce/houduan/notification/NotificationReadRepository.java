package com.deltaforce.houduan.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface NotificationReadRepository extends JpaRepository<NotificationReadEntity, Long> {
    List<NotificationReadEntity> findByUserId(Long userId);

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    Set<NotificationReadEntity> findByNotificationIdInAndUserId(Set<Long> notificationIds, Long userId);
}
