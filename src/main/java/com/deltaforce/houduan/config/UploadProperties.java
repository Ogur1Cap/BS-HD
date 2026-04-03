package com.deltaforce.houduan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地上传目录（头像等），可通过 app.upload.dir 覆盖。
 */
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(String dir) {
    public String dirOrDefault() {
        return (dir == null || dir.isBlank()) ? "./houduan-uploads" : dir;
    }
}
