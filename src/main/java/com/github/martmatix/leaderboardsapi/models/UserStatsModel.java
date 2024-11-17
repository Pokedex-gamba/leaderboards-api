package com.github.martmatix.leaderboardsapi.models;

public class UserStatsModel {

    private String name;
    private Integer score;

    public UserStatsModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
