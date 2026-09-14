package com.vocaquiz.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 영상 메타데이터 일괄 수집(P1-3-3, D-060) 같은 {@code @Scheduled} 배치를 켠다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
