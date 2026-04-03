package com.deltaforce.houduan.map;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "map_pois")
public class MapPoiEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer x;
    private Integer y;
    private String floor;
    private String type;
    private String modes;
    private String securityLevels;
}
