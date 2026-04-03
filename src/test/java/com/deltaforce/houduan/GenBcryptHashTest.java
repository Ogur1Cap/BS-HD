package com.deltaforce.houduan;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 仅用于本地生成 Flyway 种子密码哈希；需要时去掉 @Disabled 后运行：
 * mvn test -Dtest=GenBcryptHashTest#printHashForSeed
 */
class GenBcryptHashTest {

    @Test
    @Disabled("手动生成 BCrypt 时临时启用")
    void printHashForSeed() {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
