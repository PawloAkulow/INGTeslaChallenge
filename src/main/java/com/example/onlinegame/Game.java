package com.example.onlinegame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game {
    public static final int POINTS_SHIFT = 20;
    public static final int MIN_NUMBER_OF_PLAYERS = 1;
    public static final int MAX_NUMBER_OF_PLAYERS = 1000;
    public static final int MAX_POINTS = 100_000;
    public static final int MIN_POINTS = 1;

    private int groupCount;
    private List<Integer> encodedClans;

    public Game(int groupCount, List<Integer> encodedClans) {
        this.groupCount = groupCount;
        this.encodedClans = encodedClans;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public List<Integer> getEncodedClans() {
        return encodedClans;
    }

    public static int encodeClan(int numberOfPlayers, int points) {
        int shiftedPoints = points << POINTS_SHIFT;
        int invertedNumberOfPlayers = MAX_NUMBER_OF_PLAYERS - numberOfPlayers;
        return shiftedPoints | invertedNumberOfPlayers;
    }

    public static int decodeNumberOfPlayers(int encodedClan) {
        return MAX_NUMBER_OF_PLAYERS - (encodedClan & ((1 << POINTS_SHIFT) - 1));
    }

    public static int decodePoints(int encodedClan) {
        return encodedClan >>> POINTS_SHIFT;
    }

    public List<List<Integer>> calculateGroups() {
        encodedClans.sort(Comparator.reverseOrder());

        List<List<Integer>> orderedGroups = new ArrayList<>();
        int currentGroupSize = 0;
        List<Integer> currentGroup = new ArrayList<>();

        for (Integer encodedClan : encodedClans) {
            int numberOfPlayers = decodeNumberOfPlayers(encodedClan);
            if (currentGroupSize + numberOfPlayers <= groupCount) {
                currentGroupSize += numberOfPlayers;
                currentGroup.add(encodedClan);
            } else {
                orderedGroups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(encodedClan);
                currentGroupSize = numberOfPlayers;
            }
        }

        if (!currentGroup.isEmpty()) {
            orderedGroups.add(currentGroup);
        }

        return orderedGroups;
    }

}
