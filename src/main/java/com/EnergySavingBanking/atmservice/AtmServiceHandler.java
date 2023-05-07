package com.EnergySavingBanking.atmservice;

import com.EnergySavingBanking.AbstractHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * All ATM data encoded into one Integer with bitwise operations 
 * due to memory and CPU time optimalization
 * Usage of integers allows to use CPU cache more efficient
 * bitwise operations are quickier as well
 * No concurrency used because it wouldn't improve sorting  
 */
public class AtmServiceHandler extends AbstractHandler {

    public AtmServiceHandler(ExecutorService executorService) {
        super(executorService);
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

    private static final Map<String, Integer> priorityMapping = Map.of(
            "FAILURE_RESTART", 0,
            "PRIORITY", 1,
            "SIGNAL_LOW", 2,
            "STANDARD", 3);

    private static final List<String> REQUEST_TYPES = new ArrayList<>(priorityMapping.keySet());

    @Override
    protected void processRequestData(String requestBody, HttpExchange exchange)
            throws IOException, IllegalArgumentException, InterruptedException {
        List<Integer> tasks;
        try {
            tasks = parseTasksFromJson(requestBody);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, e.getMessage());
            return;
        }

        String jsonResponse = createJsonResponse(calculateOrder(tasks));
        sendJsonResponse(exchange, jsonResponse);
    }

    private List<Integer> parseTasksFromJson(String json) throws IllegalArgumentException {
        List<Integer> tasks = new ArrayList<>();

        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            int region = jsonObject.get("region").getAsInt();
            if (region < REGION_MIN || region > REGION_MAX) {
                throw new IllegalArgumentException(INVALID_REGION_MESSAGE);
            }

            int atmId = jsonObject.get("atmId").getAsInt();
            if (atmId < ATM_ID_MIN || atmId > ATM_ID_MAX) {
                throw new IllegalArgumentException(INVALID_ATM_ID_MESSAGE);
            }

            String requestTypeString = jsonObject.get("requestType").getAsString();
            if (!REQUEST_TYPES.contains(requestTypeString)) {
                throw new IllegalArgumentException(INVALID_REQUEST_TYPE_MESSAGE);
            }
            int requestType = priorityMapping.get(requestTypeString);

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
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonArray resultArray = new JsonArray();

        for (Map.Entry<Integer, List<Integer>> entry : regionAndPriority.entrySet()) {
            int region = (entry.getKey() >>> REQUEST_TYPE_BIT_LENGTH) & ATM_REGION_BITMASK;
            List<Integer> atmIds = entry.getValue();

            for (Integer atmId : atmIds) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("region", region);
                jsonObject.addProperty("atmId", atmId);
                resultArray.add(jsonObject);
            }
        }

        return gson.toJson(resultArray);
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
