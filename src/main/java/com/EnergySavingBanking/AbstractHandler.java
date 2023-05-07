package com.EnergySavingBanking;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

/**
 * AbstractHandler is an abstract class that provides common functionalities
 * for handling HTTP requests in a web application. It implements the
 * {@link HttpHandler} interface and provides methods for handling POST
 * requests, sending error and JSON responses, and processing request data.
 * <p>
 * Subclasses of AbstractHandler should override the
 * {@link #processRequestData(String, HttpExchange)} method to implement their
 * own custom request processing logic.
 */
public abstract class AbstractHandler implements HttpHandler {

    protected ExecutorService executorService;

    private static final String REQUEST_PROCESSING_INTERRUPTED_MESSAGE = "Request processing interrupted";
    private static final String ERROR_HEADER_MESSAGE = "Error: ";


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
                sendErrorResponse(exchange, REQUEST_PROCESSING_INTERRUPTED_MESSAGE);
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
        String response = ERROR_HEADER_MESSAGE + errorMessage;
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
