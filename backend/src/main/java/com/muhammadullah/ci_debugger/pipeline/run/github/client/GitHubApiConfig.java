package com.muhammadullah.ci_debugger.pipeline.run.github.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
public class GitHubApiConfig {

    @Value("${github.api.base-url}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String apiToken;

    @Value("${github.api.connect-timeout-ms}")
    private int connectionTimeoutMs;

    @Value("${github.api.read-timeout-ms}")
    private int readTimeoutMs;


    @Bean
    public RestClient gitHubRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectionTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeaders(
                        httpHeaders -> {
                        httpHeaders.set("Authorization", "Bearer " + apiToken);
                        httpHeaders.set("Accept", "application/vnd.github+json");
                        httpHeaders.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .requestFactory(requestFactory)
                    .build();


    }

    
}
