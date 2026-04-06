package com.deltaforce.houduan.security;

/**
 * JWT 解析后的当前用户；userLevel 0=顾客，1=打手。
 */
public record JwtUserPrincipal(Long userId, String username, int userLevel) {
}
