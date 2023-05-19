package com.EnergySavingBanking.onlinegame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game {
    // we need 10 bits to store up to 1000 Players in lower bits
    public static final int POINTS_SHIFT = 10;
    public static final int MIN_NUMBER_OF_PLAYERS = 1;
    public static final int MAX_NUMBER_OF_PLAYERS = 1000;
    // it's 21 upper bits
    public static final int MAX_POINTS = 1_000_000;
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
        int firstNonFilledGroup = 0;

        List<List<Integer>> orderedGroups = new ArrayList<>();
        List<Integer> remainingSpaces = new ArrayList<>();

        for (Integer encodedClan : encodedClans) {
            int numberOfPlayers = decodeNumberOfPlayers(encodedClan);
            int bestGroupIndex = -1;
            int bestRemainingSpace = groupCount + 1;

            boolean fullGroupsSequence = true;
            for (int i = firstNonFilledGroup; i < orderedGroups.size(); i++) {
                int remainingSpace = remainingSpaces.get(i);
                if (remainingSpace != 0) {
                    fullGroupsSequence = false;
                }
                if (fullGroupsSequence) {
                    firstNonFilledGroup = i; 
                }
                remainingSpace -= numberOfPlayers;
                if (remainingSpace >= 0) {
                    bestRemainingSpace = remainingSpace;
                    bestGroupIndex = i;
                    break;
                }
            }

            if (bestGroupIndex != -1) {
                orderedGroups.get(bestGroupIndex).add(encodedClan);
                remainingSpaces.set(bestGroupIndex, bestRemainingSpace);
            } else {
                List<Integer> newGroup = new ArrayList<>();
                newGroup.add(encodedClan);
                orderedGroups.add(newGroup);
                remainingSpaces.add(groupCount - numberOfPlayers);
            }
        }

        return orderedGroups;
    }

}
