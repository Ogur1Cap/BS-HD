package com.deltaforce.houduan.player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "players")
public class PlayerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String avatar;
    private String rankName;
    private String skills;
    private BigDecimal winRate;
    private Integer completedOrders;
    private BigDecimal rating;
    private BigDecimal pricePerHour;
    private String intro;
    private String tags;

    /** 是否在打手大厅展示；解除打手资格时置为 false */
    @Column(name = "show_in_hall", nullable = false)
    private boolean showInHall = true;
}
