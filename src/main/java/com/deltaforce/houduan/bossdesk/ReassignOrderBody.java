package com.deltaforce.houduan.bossdesk;

import lombok.Data;

@Data
public class ReassignOrderBody {
    private Long targetPlayerId;
    private String remark;
}
