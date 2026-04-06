package com.deltaforce.houduan.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMapMarkerRepository extends JpaRepository<UserMapMarkerEntity, Long> {
    List<UserMapMarkerEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserMapMarkerEntity> findByIdAndUserId(Long id, Long userId);
}
