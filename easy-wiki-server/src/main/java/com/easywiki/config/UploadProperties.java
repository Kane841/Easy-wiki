package com.easywiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "easywiki.upload")
public class UploadProperties {

    private String path = "D:/easy-wiki/uploads/";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
