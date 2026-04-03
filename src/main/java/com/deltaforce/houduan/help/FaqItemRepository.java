package com.deltaforce.houduan.help;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqItemRepository extends JpaRepository<FaqItemEntity, Long> {
    List<FaqItemEntity> findByCategory(String category);
}
