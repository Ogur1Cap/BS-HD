package com.deltaforce.houduan.notification;

import com.deltaforce.houduan.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    public NotificationService(NotificationRepository notificationRepository, NotificationReadRepository notificationReadRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationReadRepository = notificationReadRepository;
    }

    public List<Map<String, Object>> list(Long userId) {
        List<NotificationEntity> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<Long> readIds = readIdsFor(userId, list);
        return list.stream().map(n -> toDto(n, readIds.contains(n.getId()))).toList();
    }

    /** 未读条数（供顶栏角标轮询，避免传输完整列表） */
    public long countUnread(Long userId) {
        List<NotificationEntity> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (list.isEmpty()) {
            return 0;
        }
        Set<Long> readIds = readIdsFor(userId, list);
        return list.stream().filter(n -> !readIds.contains(n.getId())).count();
    }

    private Set<Long> readIdsFor(Long userId, List<NotificationEntity> notifications) {
        Set<Long> ids = notifications.stream().map(NotificationEntity::getId).collect(Collectors.toSet());
        return notificationReadRepository.findByUserId(userId).stream()
                .map(NotificationReadEntity::getNotificationId)
                .filter(ids::contains)
                .collect(Collectors.toSet());
    }

    @Transactional
    public List<Map<String, Object>> markRead(Long userId, Long notificationId) {
        NotificationEntity n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BizException(404, "通知不存在"));
        if (!notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId)) {
            NotificationReadEntity read = new NotificationReadEntity();
            read.setNotificationId(n.getId());
            read.setUserId(userId);
            read.setReadAt(LocalDateTime.now());
            notificationReadRepository.save(read);
        }
        return list(userId);
    }

    @Transactional
    public List<Map<String, Object>> markAllRead(Long userId) {
        List<NotificationEntity> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<Long> readIds = notificationReadRepository.findByUserId(userId).stream()
                .map(NotificationReadEntity::getNotificationId)
                .collect(Collectors.toSet());
        for (NotificationEntity n : list) {
            if (!readIds.contains(n.getId())) {
                NotificationReadEntity read = new NotificationReadEntity();
                read.setNotificationId(n.getId());
                read.setUserId(userId);
                read.setReadAt(LocalDateTime.now());
                notificationReadRepository.save(read);
            }
        }
        return list(userId);
    }

    @Transactional
    public List<Map<String, Object>> delete(Long userId, Long notificationId) {
        NotificationEntity n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BizException(404, "通知不存在"));
        notificationRepository.delete(n);
        return list(userId);
    }

    private Map<String, Object> toDto(NotificationEntity entity, boolean isRead) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", String.valueOf(entity.getId()));
        dto.put("title", entity.getTitle());
        dto.put("content", entity.getContent());
        dto.put("type", entity.getType());
        dto.put("isRead", isRead);
        dto.put("createdAt", entity.getCreatedAt().toString());
        dto.put("relatedId", entity.getRelatedId());
        return dto;
    }
}
