package com.example.onlinegame;

public class Clan {
    private int numberOfPlayers;
    private int points;

    public Clan(int numberOfPlayers, int points) {
        this.numberOfPlayers = numberOfPlayers;
        this.points = points;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public int getPoints() {
        return points;
    }
}
