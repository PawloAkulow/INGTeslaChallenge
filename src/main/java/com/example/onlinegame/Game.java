package com.example.onlinegame;

import java.util.List;

public class Game {
    private int groupCount;
    private List<Clan> clans;

    public Game(int groupCount, List<Clan> clans) {
        this.groupCount = groupCount;
        this.clans = clans;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public List<Clan> getClans() {
        return clans;
    }
}
