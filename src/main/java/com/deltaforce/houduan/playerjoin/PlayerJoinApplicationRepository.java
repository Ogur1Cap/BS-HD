package com.deltaforce.houduan.playerjoin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerJoinApplicationRepository extends JpaRepository<PlayerJoinApplicationEntity, Long> {

    boolean existsByUserIdAndStatus(Long userId, JoinApplicationStatus status);

    List<PlayerJoinApplicationEntity> findByStatusOrderByCreatedAtDesc(JoinApplicationStatus status);

    long countByStatus(JoinApplicationStatus status);

    Optional<PlayerJoinApplicationEntity> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
