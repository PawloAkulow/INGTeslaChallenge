package com.EnergySavingBanking.onlinegame;

import com.EnergySavingBanking.AbstractHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OnlineGameCalculateHandler extends AbstractHandler {

    public OnlineGameCalculateHandler(ExecutorService executorService) {
        super(executorService);
    }

    private static final String INVALID_PROPERTY_KEY_MESSAGE = "Invalid property key";
    private static final String INVALID_GROUP_COUNT_MESSAGE = "Invalid groupCount value";
    private static final String INVALID_NUMBER_OF_PLAYERS_MESSAGE = "Invalid numberOfPlayers value";
    private static final String INVALID_POINTS_MESSAGE = "Invalid points value";
    private static final String NON_INTEGER_VALUE_MESSAGE = "Non-integer value for";

    private static final int GROUP_COUNT_MIN = 1;
    private static final int GROUP_COUNT_MAX = 1000;
    private static final int NUMBER_OF_PLAYERS_MIN = Game.MIN_NUMBER_OF_PLAYERS;
    private static final int NUMBER_OF_PLAYERS_MAX = Game.MAX_NUMBER_OF_PLAYERS;
    private static final int POINTS_MIN = Game.MIN_POINTS;
    private static final int POINTS_MAX = Game.MAX_POINTS;

    @Override
    protected void processRequestData(String requestBody, HttpExchange exchange) throws IOException, IllegalArgumentException {
        executorService.submit(() -> {
            List<List<Integer>> orderedGroups;
            try {
                Game game = parseGameFromJson(requestBody);
                orderedGroups = game.calculateGroups();
            } catch (IllegalArgumentException e) {
                try {
                    sendErrorResponse(exchange, e.getMessage());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                return;
            }
            String jsonResponse = createJsonResponse(orderedGroups);
            try {
                sendJsonResponse(exchange, jsonResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Game parseGameFromJson(String json) throws IllegalArgumentException {
        int groupCount;
        List<Integer> encodedClans = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        if (jsonObject.has("groupCount")) {
            try {
                groupCount = jsonObject.get("groupCount").getAsInt();
                if (groupCount < GROUP_COUNT_MIN || groupCount > GROUP_COUNT_MAX) {
                    throw new IllegalArgumentException(INVALID_GROUP_COUNT_MESSAGE);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " groupCount");
            }
        } else {
            throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
        }

        if (jsonObject.has("clans")) {
            JsonArray clansArray = jsonObject.getAsJsonArray("clans");
            for (int i = 0; i < clansArray.size(); i++) {
                JsonObject clanObject = clansArray.get(i).getAsJsonObject();
                int numberOfPlayers;
                int points;

                if (clanObject.has("numberOfPlayers")) {
                    try {
                        numberOfPlayers = clanObject.get("numberOfPlayers").getAsInt();
                        if (numberOfPlayers < NUMBER_OF_PLAYERS_MIN || numberOfPlayers > NUMBER_OF_PLAYERS_MAX) {
                            throw new IllegalArgumentException(INVALID_NUMBER_OF_PLAYERS_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " numberOfPlayers");
                    }
                } else {
                    throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
                }
                if (clanObject.has("points")) {
                    try {
                        points = clanObject.get("points").getAsInt();
                        if (points < POINTS_MIN || points > POINTS_MAX) {
                            throw new IllegalArgumentException(INVALID_POINTS_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " points");
                    }
                } else {
                    throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
                }
    
                encodedClans.add(Game.encodeClan(numberOfPlayers, points));
            }
        } else {
            throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
        }
    
        return new Game(groupCount, encodedClans);
    }
    
    private String createJsonResponse(List<List<Integer>> orderedGroups) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonArray resultArray = new JsonArray();
    
        for (List<Integer> group : orderedGroups) {
            JsonArray groupArray = new JsonArray();
            for (Integer encodedClan : group) {
                int numberOfPlayers = Game.decodeNumberOfPlayers(encodedClan);
                int points = Game.decodePoints(encodedClan);
                JsonObject clanObject = new JsonObject();
                clanObject.addProperty("numberOfPlayers", numberOfPlayers);
                clanObject.addProperty("points", points);
                groupArray.add(clanObject);
            }
            resultArray.add(groupArray);
        }
    
        return gson.toJson(resultArray);
    }
}    
