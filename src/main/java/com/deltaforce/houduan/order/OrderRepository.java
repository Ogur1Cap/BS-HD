package com.deltaforce.houduan.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    List<OrderEntity> findByPlayerIdAndStatusOrderByCreatedAtDesc(String playerId, OrderStatus status);

    List<OrderEntity> findByPlayerIdAndStatusInOrderByCreatedAtDesc(String playerId, Collection<OrderStatus> statuses);

    List<OrderEntity> findByPlayerIdOrderByCreatedAtDesc(String playerId);

    List<OrderEntity> findByStatusOrderByUpdatedAtDesc(OrderStatus status);

    List<OrderEntity> findByStatusInOrderByUpdatedAtDesc(Collection<OrderStatus> statuses);
}
