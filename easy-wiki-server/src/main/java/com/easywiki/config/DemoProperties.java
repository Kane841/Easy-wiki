package com.easywiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "easywiki.demo")
public class DemoProperties {

    /** 启动时是否清空并写入演示数据 */
    private boolean seed = false;

    /** 演示账号统一密码 */
    private String password = "Demo@2026";

    public boolean isSeed() {
        return seed;
    }

    public void setSeed(boolean seed) {
        this.seed = seed;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
