package com.deltaforce.houduan.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
    List<SupportTicketEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
