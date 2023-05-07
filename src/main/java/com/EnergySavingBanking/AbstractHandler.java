package com.EnergySavingBanking;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

public abstract class AbstractHandler implements HttpHandler {

    protected ExecutorService executorService;

    public AbstractHandler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                handlePostRequest(exchange);
            } catch (InterruptedException e) {
                // Handle InterruptedException, e.g., log the error or send an error response
                e.printStackTrace();
                sendErrorResponse(exchange, "Request processing interrupted");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    protected void handlePostRequest(HttpExchange exchange) throws IOException, InterruptedException {
        String requestBody;
        try (InputStream input = exchange.getRequestBody();
             Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {
            requestBody = scanner.useDelimiter("\\A").next();
        }

        try {
            processRequestData(requestBody, exchange);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, e.getMessage());
        }
    }

    protected abstract void processRequestData(String requestBody, HttpExchange exchange) throws IOException, IllegalArgumentException,InterruptedException;

    protected void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        String response = "Error: " + errorMessage;
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(400, responseBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
            output.flush();
        }
    }

    protected void sendJsonResponse(HttpExchange exchange, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
            output.flush();
        }
    }
}