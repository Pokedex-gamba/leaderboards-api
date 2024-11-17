package com.github.martmatix.leaderboardsapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.martmatix.leaderboardsapi.constants.ErrorCodes;
import com.github.martmatix.leaderboardsapi.models.UserStatsModel;
import com.github.martmatix.leaderboardsapi.services.KeyLoaderService;
import com.github.martmatix.leaderboardsapi.services.LeaderboardsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.PublicKey;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class LeaderboardsController {

    private KeyLoaderService keyLoaderService;

    private LeaderboardsService leaderboardsService;

    @GetMapping("/pokemon/leaderboards/getLeaderboards")
    public ResponseEntity<?> getLeaderboards(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader, @RequestParam(value = "count", required = false) String count) {
        String userId = getUserIdFromToken(authHeader);
        if (userId.equals(ErrorCodes.TOKEN_EXTRACTION_ERROR.getCode())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Unable To Process Request: " + ErrorCodes.TOKEN_EXTRACTION_ERROR.getCode() + "\"}");
        }
        if (userId.equals(ErrorCodes.PUBLIC_NOT_FOUND.getCode())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Unable To Process Request: " + ErrorCodes.PUBLIC_NOT_FOUND.getCode() + "\"}");
        }

        String responseSpec = leaderboardsService.retrieveUserStats(authHeader).bodyToMono(String.class).block();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Integer> userStats;
        try {
            userStats = objectMapper.readValue(responseSpec, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<UserStatsModel> list = userStats.entrySet().stream()
                .map(entry -> {
                    UserStatsModel model = new UserStatsModel();
                    model.setName(entry.getKey());
                    model.setScore(entry.getValue());
                    return model;
                })
                .sorted(Comparator.comparing(UserStatsModel::getScore))
                .toList();

        if (count == null || Integer.parseInt(count) >= list.size()) {
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.ok(list.subList(0, Integer.parseInt(count)));
    }

    private String getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer", "").trim();

        PublicKey publicKey;
        try {
            String path = LeaderboardsController.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File publicKeyFile = new File(path, "decoding_key");
            if (!publicKeyFile.exists()) {
                return ErrorCodes.PUBLIC_NOT_FOUND.getCode();
            }
            BufferedReader reader = new BufferedReader(new FileReader(publicKeyFile));
            String publicKeyContent = reader.lines().collect(Collectors.joining("\n"));
            reader.close();
            publicKey = keyLoaderService.getPublicKey(publicKeyContent);
        } catch (Exception e) {
            return ErrorCodes.TOKEN_EXTRACTION_ERROR.getCode();
        }

        Claims claims = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();

        String userId = claims.get("user_id", String.class);
        if (userId == null) {
            return ErrorCodes.TOKEN_EXTRACTION_ERROR.getCode();
        }

        return userId;
    }

    @Autowired
    public void setKeyLoaderService(KeyLoaderService keyLoaderService) {
        this.keyLoaderService = keyLoaderService;
    }

    @Autowired
    public void setLeaderboardsService(LeaderboardsService leaderboardsService) {
        this.leaderboardsService = leaderboardsService;
    }
}
