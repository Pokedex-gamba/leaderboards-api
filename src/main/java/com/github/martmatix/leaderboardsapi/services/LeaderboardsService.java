package com.github.martmatix.leaderboardsapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class LeaderboardsService {

    private WebClient.Builder builder;

    @Value("${inventory.api.url}")
    private String inventoryHost;

    public WebClient.ResponseSpec retrieveUserStats(String authHeader) {
        WebClient webClient = builder.baseUrl(inventoryHost).build();

        return webClient.get()
                .uri("/pokemon/inventory/getUserTotal")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve();
    }

    @Autowired
    public void setBuilder(WebClient.Builder builder) {
        this.builder = builder;
    }
}
