package com.deltaforce.houduan.violation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<ViolationEntity, Long> {
    List<ViolationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ViolationEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<ViolationEntity> findByOrderByCreatedAtDesc();
}