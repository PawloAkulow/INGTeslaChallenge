package com.example.atmservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class AtmServiceHandler implements HttpHandler {
    private ExecutorService executorService;

    public AtmServiceHandler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    // Restrictions from schema
    private static final int REGION_MIN = 1;
    private static final int REGION_MAX = 9999;
    private static final int ATM_ID_MIN = 1;
    private static final int ATM_ID_MAX = 9999;
    private static final int ATM_ID_BITMASK = 0x3FFF;
    private static final int ATM_REGION_BITMASK = 0x3FFF;
    private static final int REQUEST_TYPE_BITMASK = 0x3;
    private static final int ATM_ID_BIT_LENGTH = 14;
    private static final int REQUEST_TYPE_BIT_LENGTH = 2;

    private static final String INVALID_REGION_MESSAGE = "Invalid region value";
    private static final String INVALID_REQUEST_TYPE_MESSAGE = "Invalid requestType value";
    private static final String INVALID_ATM_ID_MESSAGE = "Invalid atmId value";
    private static final String INVALID_PROPERTY_KEY_MESSAGE = "Invalid property key";
    private static final String NON_INTEGER_VALUE_MESSAGE = "Non-integer value encountered";

    private static final Map<String, Integer> priorityMapping = Map.of(
        "FAILURE_RESTART", 0,
        "PRIORITY", 1,
        "SIGNAL_LOW", 2,
        "STANDARD", 3
    );

    private static final List<String> REQUEST_TYPES = new ArrayList<>(priorityMapping.keySet());

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

                List<Integer> tasks;
                try {
                    tasks = parseTasksFromJson(requestBody);
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

                String jsonResponse = createJsonResponse(calculateOrder(tasks));

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

private List<Integer> parseTasksFromJson(String json) throws IllegalArgumentException {
    List<Integer> tasks = new ArrayList<>();
     String[] jsonObjects = json.substring(1, json.length() - 1).split("},");
    for (String jsonObject : jsonObjects) {
        int region =0;
        int atmId = 0;
        int requestType = -1;
        String[] jsonProperties = jsonObject.replaceAll("[\\s{}]", "").split(",");
        for (String property : jsonProperties) {
            String[] keyValue = property.split(":");
            String key = keyValue[0].replaceAll("\"", "");
            String value = keyValue[1].replaceAll("\"", "");
            switch (key) {
                case "region":
                    try {
                        region = Integer.parseInt(value);
                        if (region < REGION_MIN || region > REGION_MAX) {
                            throw new IllegalArgumentException(INVALID_REGION_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE);
                    }
                    break;
                case "requestType":
                    if (!REQUEST_TYPES.contains(value)) {
                        throw new IllegalArgumentException(INVALID_REQUEST_TYPE_MESSAGE);
                    }
                    requestType = priorityMapping.get(value);
                    break;
                case "atmId":
                    try {
                        atmId = Integer.parseInt(value);
                        if (atmId < ATM_ID_MIN || atmId > ATM_ID_MAX) {
                            throw new IllegalArgumentException(INVALID_ATM_ID_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(NON_INTEGER_VALUE_MESSAGE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
            }
        }
        tasks.add(encodeTask(region, requestType, atmId));
     }
    return tasks;
}
    // it's region and atmId
    private static int getATMUnique(int taskInteger) {
        int region = (taskInteger >>> (ATM_ID_BIT_LENGTH + REQUEST_TYPE_BIT_LENGTH)) & ATM_ID_BITMASK;
        int atmId = taskInteger & ATM_ID_BITMASK;
        return (region << ATM_ID_BIT_LENGTH) | atmId;
}

    // it's region and request type
    private static int getPriority(int taskInteger) {
        int region = (taskInteger >>> (ATM_ID_BIT_LENGTH + REQUEST_TYPE_BIT_LENGTH)) & ATM_ID_BITMASK;
        int requestType = (taskInteger >>> ATM_ID_BIT_LENGTH) & REQUEST_TYPE_BITMASK;
        return (region << REQUEST_TYPE_BIT_LENGTH) | requestType;
}

    private static int encodeTask(int region, int requestType, int atmId) {
        return (region << (ATM_ID_BIT_LENGTH + REQUEST_TYPE_BIT_LENGTH)) | (requestType << ATM_ID_BIT_LENGTH) | atmId;
    }

    private String createJsonResponse(Map<Integer, List<Integer>> regionAndPriority) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
    
        for (Map.Entry<Integer, List<Integer>> entry : regionAndPriority.entrySet()) {
            int region = (entry.getKey() >>> REQUEST_TYPE_BIT_LENGTH) & ATM_REGION_BITMASK;
            List<Integer> atmIds = entry.getValue();
    
            for (Integer atmId : atmIds) {
                json.append("  {\n");
                json.append("    \"region\": ").append(region).append(",\n");
                json.append("    \"atmId\": ").append(atmId).append("\n");
                json.append("  },\n");
            }
        }
    
        // Remove the last comma and newline
        if (json.length() > 2) {
            json.setLength(json.length() - 2);
        }
    
        json.append("\n]");
    
        return json.toString();
    }    

    public Map<Integer, List<Integer>> calculateOrder(List<Integer> tasks) {
        Map<Integer, Integer> uniqueTasks = new HashMap<>();
        Map<Integer, List<Integer>> regionAndPriority = new TreeMap<>();
    
        int prevPriority = -1;
        int priority;
        List<Integer> priorityList = new LinkedList<Integer>();
        for (Integer taskInteger : tasks) {
            int atmUnique = getATMUnique(taskInteger);
            priority = getPriority(taskInteger);
            int atmId = taskInteger & ATM_ID_BITMASK;
            if (prevPriority != priority) {
                if (prevPriority != -1) {
                    regionAndPriority.put(prevPriority, priorityList);
                }
                priorityList = regionAndPriority.getOrDefault(priority, new LinkedList<Integer>());
                prevPriority = priority;
            }
    
            int oldPriority = uniqueTasks.getOrDefault(atmUnique, -1);
    
            if (oldPriority == -1 || (priority & REQUEST_TYPE_BITMASK) < (oldPriority & REQUEST_TYPE_BITMASK)) {
                if (oldPriority != -1) {
                    List<Integer> oldPriorityList = regionAndPriority.get(oldPriority);
                    oldPriorityList.remove((Integer) atmId);
                    if (oldPriorityList.isEmpty()) {
                        regionAndPriority.remove(oldPriority);
                    } else {
                        regionAndPriority.put(oldPriority, oldPriorityList);
                    }
                }
    
                priorityList.add(atmId);
                uniqueTasks.put(atmUnique, priority);
            }
        }
        // prevPriority is already synced to priority
        if (prevPriority != -1) {
            regionAndPriority.put(prevPriority, priorityList);
        }
    
        return regionAndPriority;
    }
    
}
