package com.example.onlinegame;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public class OnlineGameCalculateHandler implements HttpHandler {
    private ExecutorService executorService;

    public OnlineGameCalculateHandler(ExecutorService executorService) {
        this.executorService = executorService;
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
    public void handle(HttpExchange exchange) throws IOException {
        executorService.submit(() -> {
            try {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String requestBody;
                    try (InputStream input = exchange.getRequestBody();
                            Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {
                        requestBody = scanner.useDelimiter("\\A").next();
                    }

                    Game game;
                    try {
                        game = parseGameFromJson(requestBody);
                    } catch (IllegalArgumentException e) {
                        String response = "Error: " + e.getMessage();
                        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(400, responseBytes.length);
                        try (OutputStream output = exchange.getResponseBody()) {
                            output.write(responseBytes);
                            output.flush();
                        }
                        return;
                    }

                    List<List<Integer>> orderedGroups = game.calculateGroups();
                    String jsonResponse = createJsonResponse(orderedGroups);

                    byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(responseBytes);
                        output.flush();
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (IOException e) {
                // Handle IOException here, e.g., log the error or send an error response
                e.printStackTrace();
            }
        });
    }

    private Game parseGameFromJson(String json) throws IllegalArgumentException {
        int groupCount = 0;
        List<Integer> encodedClans = new ArrayList<>();

        String[] jsonObjects = json.substring(1, json.length() - 1).split("},");
        for (String jsonObject : jsonObjects) {
            String[] jsonProperties = jsonObject.replaceAll("[\\s{}]", "").split(",");
            int numberOfPlayers = 0;
            int points = 0;
            boolean isGroupCount = false;
            for (String property : jsonProperties) {
                String[] keyValue = property.split(":");
                String key            = keyValue[0].replaceAll("\"", "");
                String value = keyValue[1].replaceAll("\"", "");
                switch (key) {
                    case "groupCount":
                        try {
                            groupCount = Integer.parseInt(value);
                            if (groupCount < GROUP_COUNT_MIN || groupCount > GROUP_COUNT_MAX) {
                                throw new IllegalArgumentException(INVALID_GROUP_COUNT_MESSAGE);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " groupCount");
                        }
                        isGroupCount = true;
                        break;
                    case "numberOfPlayers":
                        try {
                            numberOfPlayers = Integer.parseInt(value);
                            if (numberOfPlayers < NUMBER_OF_PLAYERS_MIN || numberOfPlayers > NUMBER_OF_PLAYERS_MAX) {
                                throw new IllegalArgumentException(INVALID_NUMBER_OF_PLAYERS_MESSAGE);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " numberOfPlayers");
                        }
                        break;
                    case "points":
                        try {
                            points = Integer.parseInt(value);
                            if (points < POINTS_MIN || points > POINTS_MAX) {
                                throw new IllegalArgumentException(INVALID_POINTS_MESSAGE);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE + " points");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
                }
            }
            if (!isGroupCount) {
                encodedClans.add(Game.encodeClan(numberOfPlayers, points));
            }
        }
    
        return new Game(groupCount, encodedClans);
    }
    
    private String createJsonResponse(List<List<Integer>> orderedGroups) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
    
        for (List<Integer> group : orderedGroups) {
            json.append("  [\n");
            for (Integer encodedClan : group) {
                int numberOfPlayers = Game.decodeNumberOfPlayers(encodedClan);
                int points = Game.decodePoints(encodedClan);
                json.append("    {\n");
                json.append("      \"numberOfPlayers\": ").append(numberOfPlayers).append(",\n");
                json.append("      \"points\": ").append(points).append("\n");
                json.append("    },\n");
            }
    
            // Remove the last comma and newline
            if (json.length() > 4) {
                json.setLength(json.length() - 2);
            }
    
            json.append("\n  ],\n");
        }
    
        // Remove the last comma and newline
        if (json.length() > 2) {
            json.setLength(json.length() - 2);
        }
    
        json.append("\n]");
    
        return json.toString();
    }
    
}
    