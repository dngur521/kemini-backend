package com.opensource.kemini_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // Spring이 다른 서버와 HTTP 통신을 할 때 사용할 RestTemplate Bean
    @Bean
    public RestTemplate restTemplate() {
        // 기본 RestTemplate 대신 팩토리를 사용
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 타임아웃 설정 (단위: 밀리초)
        // Nginx가 300초(300,000ms)이므로, 동일하게 맞춥니다.
        
        // 연결(Connection) 타임아웃: 10초
        factory.setConnectTimeout(10 * 1000);
        
        // 응답 읽기(Read) 타임아웃: 300초 (이것이 핵심)
        factory.setReadTimeout(300 * 1000); 

        // 타임아웃이 설정된 팩토리로 RestTemplate 생성
        return new RestTemplate(factory);
    }
}