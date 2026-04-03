package com.deltaforce.houduan.order;

public enum OrderStatus {
    PENDING,
    IN_PROGRESS,
    /** 打手已提交完成申请，待 BOSS 审核 */
    COMPLETION_PENDING,
    COMPLETED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED
}
