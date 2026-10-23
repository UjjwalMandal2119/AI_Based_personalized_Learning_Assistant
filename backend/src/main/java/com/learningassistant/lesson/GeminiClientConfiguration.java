package com.learningassistant.lesson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GeminiClientConfiguration {
    @Bean
    RestClient geminiRestClient(RestClient.Builder builder,
                                @Value("${gemini.connect-timeout:5s}") Duration connectTimeout,
                                @Value("${gemini.response-timeout:60s}") Duration responseTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(responseTimeout);
        return builder.baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(requestFactory)
                .build();
    }
}
