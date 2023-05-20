package com.EnergySavingBanking.transactions;

import com.EnergySavingBanking.AbstractHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

public class TransactionsReportHandler extends AbstractHandler {
    private static final String INVALID_DEBIT_ACCOUNT_MESSAGE = "Invalid debit account number.";
    private static final String INVALID_CREDIT_ACCOUNT_MESSAGE = "Invalid credit account number.";
    private static final String INVALID_AMOUNT_MESSAGE = "Invalid amount value.";
    private static final int ACCOUNT_NUMBER_LENGTH = 26;
    private static final int CHUNK_SIZE = 10_000;

    private List<CompletableFuture<Void>> futures = new ArrayList<>();

    public TransactionsReportHandler(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    protected void processRequestData(String requestBody, HttpExchange exchange)
            throws IOException, IllegalArgumentException, InterruptedException {
        Map<String, AccountData> accountDataMap = new ConcurrentSkipListMap<>(); // Change this line
        try {
            parseTransactionsFromJson(requestBody, chunk -> {
                if (chunk.size() < CHUNK_SIZE) {
                    processChunk(accountDataMap, chunk);
                } else {
                    processChunkAsync(accountDataMap, chunk);
                }
            });
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, e.getMessage());
            return;
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Collection<AccountData> accountData = accountDataMap.values();

        String jsonResponse = createJsonResponse(accountData);

        sendJsonResponse(exchange, jsonResponse);
    }

    private void parseTransactionsFromJson(String json, Consumer<List<Transaction>> chunkProcessor)
            throws IllegalArgumentException {
        List<Transaction> transactions = new ArrayList<>();
        Gson gson = new Gson();

        JsonArray jsonArray = gson.fromJson(json, JsonArray.class);

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            String debitAccount = jsonObject.get("debitAccount").getAsString();
            String creditAccount = jsonObject.get("creditAccount").getAsString();
            BigDecimal amount = jsonObject.get("amount").getAsBigDecimal();

            if (debitAccount.length() != ACCOUNT_NUMBER_LENGTH) {
                throw new IllegalArgumentException(INVALID_DEBIT_ACCOUNT_MESSAGE);
            }

            if (creditAccount.length() != ACCOUNT_NUMBER_LENGTH) {
                throw new IllegalArgumentException(INVALID_CREDIT_ACCOUNT_MESSAGE);
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(INVALID_AMOUNT_MESSAGE);
            }

            transactions.add(new Transaction(debitAccount, creditAccount, amount));

            if (transactions.size() >= CHUNK_SIZE || i == jsonArray.size() - 1) {
                chunkProcessor.accept(transactions);
                transactions = new ArrayList<>();
            }
        }
    }

    private void processChunkAsync(Map<String, AccountData> accountDataMap, List<Transaction> chunk) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processChunk(accountDataMap, chunk),
                executorService);
        futures.add(future);
    }

    private void processChunk(Map<String, AccountData> accountDataMap, List<Transaction> chunk) {
        for (Transaction transaction : chunk) {
            String debitAccount = transaction.getDebitAccount();
            String creditAccount = transaction.getCreditAccount();
            BigDecimal amount = transaction.getAmount();

            // Update debit account
            AccountData debitAccountData = accountDataMap.computeIfAbsent(debitAccount, k -> new AccountData(k));
            synchronized (debitAccountData) {
                debitAccountData.incrementDebitCount();
                debitAccountData.subtractFromBalance(amount);
            }

            // Update credit account
            AccountData creditAccountData = accountDataMap.computeIfAbsent(creditAccount, k -> new AccountData(k));
            synchronized (creditAccountData) {
                creditAccountData.incrementCreditCount();
                creditAccountData.addToBalance(amount);
            }
        }
    }

    private static String createJsonResponse(Collection<AccountData> accountDataCollection) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray jsonArray = new JsonArray();

        for (AccountData accountData : accountDataCollection) {
            JsonObject accountDataJson = new JsonObject();
            accountDataJson.addProperty("account", accountData.getAccountNumber());
            accountDataJson.addProperty("debitCount", accountData.getDebitCount());
            accountDataJson.addProperty("creditCount", accountData.getCreditCount());

            // add the balance as a JsonPrimitive of type number
            accountDataJson.add("balance", new JsonPrimitive(accountData.getBalance()));

            jsonArray.add(accountDataJson);
        }

        return gson.toJson(jsonArray);
    }

}