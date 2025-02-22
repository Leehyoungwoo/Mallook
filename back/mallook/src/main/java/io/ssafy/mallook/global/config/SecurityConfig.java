package io.ssafy.mallook.global.config;

import io.ssafy.mallook.global.security.filter.JwtAuthenticateFilter;
import io.ssafy.mallook.global.security.handler.CustomAccessDeniedHandler;
import io.ssafy.mallook.global.security.handler.CustomOAuth2FailHandler;
import io.ssafy.mallook.global.security.handler.CustomOAuth2SucceessHandler;
import io.ssafy.mallook.global.security.service.CustomOAuth2UserService;
import io.ssafy.mallook.global.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] URL_WHITE_LIST = {
            "/error", "/login", "/favicon.ico",
            "/actuator/**", "/actuator", "/api-docs/**", "/swagger-ui/**",
            "/swagger-resources/**", "/swagger-ui.html", "/api/token/**",
            "/api/auth/login/kakao"
    };
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SucceessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2FailHandler customOAuth2FailHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable)
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(
                        authorize -> authorize.requestMatchers(URL_WHITE_LIST).permitAll().anyRequest().authenticated())

                .oauth2Login(
                        oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                                .successHandler(customOAuth2SuccessHandler).failureHandler(customOAuth2FailHandler))
                .addFilterBefore(jwtAuthenticateFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handingConfigurer ->
                        handingConfigurer.accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticateFilter jwtAuthenticateFilter() {
        return new JwtAuthenticateFilter(jwtService, URL_WHITE_LIST);
    }

    // CORS 설정
    CorsConfigurationSource corsConfigurationSource() {
        final List<String> allowedHeaders = List.of("*");
        final List<String> allowedOriginPatterns = List.of(
                "http://localhost:8080",
                "http://localhost:3000",
                "https://j10a606.p.ssafy.io"
        );
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(allowedHeaders);
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(allowedOriginPatterns); // ⭐️ 허용할 origin
            config.setAllowCredentials(true);
            return config;
        };
    }
}
