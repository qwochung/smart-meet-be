package com.example.smartmeetbe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "transcript.merge")
public class MergeConfig {

    private long debounceMs = 200;
    private long gapThresholdMs = 2000;
    private long outOfOrderTimeoutMs = 3000;
    private long snapshotIntervalMs = 30000;
    private int snapshotEveryNVersions = 10;
    private int stateExpireMinutes = 30;
}
