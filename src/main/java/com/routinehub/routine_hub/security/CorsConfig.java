package com.routinehub.routine_hub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정을 Security 필터 단계에서 활성화
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                // H2 콘솔이 <frame> 안에서 열릴 수 있도록
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/price/**").permitAll()
                .requestMatchers(
                                "/", "/index.html", "/static/**", "/**/*.js", "/**/*.css"
                                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().denyAll()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.disable())
            .formLogin(login -> login.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 전역 CORS 정책 정의
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://43.201.99.85:8080", "http://43.201.99.85", "http://proteintracker.store/", "http://proteintracker.store:8080"));
        config.setAllowedMethods(List.of("GET","POST","OPTIONS","PUT","DELETE"));
        config.setAllowedHeaders(List.of("Content-Type","X-User-UUID"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        // source.registerCorsConfiguration("/api/price/**", config);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
