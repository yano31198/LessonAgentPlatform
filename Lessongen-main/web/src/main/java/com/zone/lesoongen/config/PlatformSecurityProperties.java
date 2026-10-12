package com.zone.lesoongen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record PlatformSecurityProperties(boolean enabled, boolean registrationEnabled) {}
