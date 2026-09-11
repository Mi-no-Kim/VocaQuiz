package com.vocaquiz.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 메인 클래스가 아니라 여기에 둔다. 메인에 붙이면 @WebMvcTest 같은 슬라이스
 * 테스트가 JPA 감사까지 끌고 들어오려다 실패한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
