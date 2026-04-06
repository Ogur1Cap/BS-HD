package com.deltaforce.houduan.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    List<PlayerEntity> findByShowInHallIsTrueOrderByIdAsc();
}
