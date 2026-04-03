package com.deltaforce.houduan.map;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_map_markers")
public class UserMapMarkerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Integer x;
    private Integer y;
    private String label;
    private String note;
    private LocalDateTime createdAt;
}
