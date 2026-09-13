package com.vocaquiz.common.config;

import com.vocaquiz.user.service.GoogleOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** /admin/** 은 role=ADMIN 만 (D-032). 나머지는 익명 허용 — 게임 플레이 때문이다. */
    private static final String[] ADMIN_PATHS = {"/admin", "/admin/**", "/api/v1/admin/**"};

    /** 사람이 아니라 프론트의 fetch가 부르는 경로. 여기서는 리다이렉트가 쓸모없다 (D-065). */
    private static final RequestMatcher API_PATHS = PathPatternRequestMatcher.pathPattern("/api/**");

    /** API를 뺀 나머지 전부. 사람이 브라우저로 여는 화면이다. 순서상 API 다음에 본다. */
    private static final RequestMatcher SCREEN_PATHS = PathPatternRequestMatcher.pathPattern("/**");

    /**
     * 구글 인가 요청이 시작되는 경로.
     *
     * <p>제공자가 하나뿐이라 Spring도 이 값을 쓴다. <b>제공자가 둘 이상이 되면 이 줄을 고쳐야 한다.</b>
     * Spring은 등록이 하나일 때만 제공자로 바로 보내고, 둘 이상이면 선택 페이지("/login")로 보낸다.
     * 아래 폴백이 목록에서 먼저 걸리므로, 여기를 안 고치면 컴파일도 테스트도 통과한 채
     * 두 번째 제공자만 조용히 못 쓰게 된다. 그때가 오면 필터 체인을 /api/**와 화면용으로
     * 나누는 쪽이 낫다. 그러면 이 상수 자체가 필요 없어진다.
     */
    private static final String LOGIN_URL = "/oauth2/authorization/google";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleOidcUserService userService) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")
                .anyRequest().permitAll())
            // 세션 쿠키로 인증하므로 CSRF를 끄지 않는다. spa()가 XSRF-TOKEN 쿠키와
            // X-XSRF-TOKEN 헤더 조합을 켠다. 프론트는 src/api/ 한 곳에서 헤더를 붙인다 (D-065).
            .csrf(csrf -> csrf.spa())
            // 로그인이 없을 때 무엇을 돌려줄지 (D-065). 등록한 순서대로 매처를 보고
            // 먼저 걸리는 쪽이 응답한다.
            //
            // 두 줄을 다 적는 이유: 아무 줄도 안 걸리면 Spring은 "맨 처음 등록된 것"을
            // 폴백으로 쓴다. /api/** 한 줄만 두면 그 401이 폴백이 되어 화면 경로까지 401이 된다.
            // (oauth2Login이 자동으로 등록하는 줄은 Accept 헤더가 html일 때만 걸려서
            //  폴백 역할을 못 한다.) 아래 두 줄이 모든 요청을 덮으므로 폴백까지 가지 않는다.
            .exceptionHandling(ex -> ex
                // fetch는 리다이렉트를 따라 구글까지 갔다가 CORS로 깨진다. 프론트가
                // "로그인이 필요함"과 "요청이 실패함"을 구분하려면 상태 코드로 와야 한다.
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_PATHS)
                // 사람이 브라우저로 연 화면은 로그인으로 보낸다.
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(LOGIN_URL), SCREEN_PATHS))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(userService)))
            .build();
    }
}
