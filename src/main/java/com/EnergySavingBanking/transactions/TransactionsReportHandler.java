package com.EnergySavingBanking.transactions;
import com.EnergySavingBanking.AbstractHandler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TransactionsReportHandler extends AbstractHandler {
    private AtomicInteger pendingTasks = new AtomicInteger(0);

    private static final String INVALID_DEBIT_ACCOUNT_MESSAGE = "Invalid debit account number.";
    private static final String INVALID_CREDIT_ACCOUNT_MESSAGE = "Invalid credit account number.";
    private static final String INVALID_AMOUNT_MESSAGE = "Invalid amount value.";
    private static final String INVALID_PROPERTY_KEY_MESSAGE = "Invalid property key.";
    private static final int ACCOUNT_NUMBER_LENGTH = 26;
    private static final int CHUNK_SIZE = 10_000;

    public TransactionsReportHandler(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    protected void processRequestData(String requestBody, HttpExchange exchange) throws IOException, IllegalArgumentException, InterruptedException {
        Map<String, AccountData> accountDataMap = new ConcurrentSkipListMap<>();
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
        while (pendingTasks.get() > 0) {
            Thread.sleep(10);
        }

        Collection<AccountData> accountData = accountDataMap.values();

        String jsonResponse = createJsonResponse(accountData);

        sendJsonResponse(exchange, jsonResponse);
    }

    private void parseTransactionsFromJson(String json, Consumer<List<Transaction>> chunkProcessor) throws IllegalArgumentException {
        List<Transaction> transactions = new ArrayList<>();
        String[] jsonObjects = json.substring(1, json.length() - 1).split("},");

        for (int i = 0; i < jsonObjects.length; i++) {
            String jsonObject = jsonObjects[i];
            String debitAccount = "";
            String creditAccount = "";
            double amount = 0;

            String[] jsonProperties = jsonObject.replaceAll("[\\s{}]", "").split(",");
            for (String property : jsonProperties) {
                String[] keyValue = property.split(":");
                String key = keyValue[0].replaceAll("\"", "");
                String value = keyValue[1].replaceAll("\"", "");

                switch (key) {
                    case "debitAccount":
                        if (value.length() != ACCOUNT_NUMBER_LENGTH) {
                            throw new IllegalArgumentException(INVALID_DEBIT_ACCOUNT_MESSAGE);
                        }
                        debitAccount = value;
                        break;
                    case "creditAccount":
                        if (value.length() != ACCOUNT_NUMBER_LENGTH) {
                            throw new IllegalArgumentException(INVALID_CREDIT_ACCOUNT_MESSAGE);
                        }
                        creditAccount = value;
                        break;
                    case "amount":
                        try {
                            amount = Double.parseDouble(value);
                            if (amount <= 0) {
                                throw new IllegalArgumentException(INVALID_AMOUNT_MESSAGE);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(INVALID_AMOUNT_MESSAGE);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(INVALID_PROPERTY_KEY_MESSAGE);
                }
            }

            transactions.add(new Transaction(debitAccount, creditAccount, amount));

            if (transactions.size() >= CHUNK_SIZE || i == jsonObjects.length - 1) {
                chunkProcessor.accept(transactions);
                transactions = new ArrayList<>();
            }
        }
    }

    private void processChunkAsync(Map<String, AccountData> accountDataMap, List<Transaction> chunk) {
        pendingTasks.incrementAndGet();
        executorService.submit(() -> {
            try {
                processChunk(accountDataMap, chunk);
            } finally {
                pendingTasks.decrementAndGet();
            }
        });
    }

    private void processChunk(Map<String, AccountData> accountDataMap, List<Transaction> chunk) {
        for (Transaction transaction : chunk) {
            String debitAccount = transaction.getDebitAccount();
            String creditAccount = transaction.getCreditAccount();
            double amount = transaction.getAmount();
    
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

        // Create a DecimalFormat with custom DecimalFormatSymbols
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("0.00", symbols);


        StringBuilder jsonResponseBuilder = new StringBuilder();
        jsonResponseBuilder.append("[\n");
    
        boolean isFirst = true;
        for (AccountData accountData : accountDataCollection) {
            if (!isFirst) {
                jsonResponseBuilder.append(",\n");
            } else {
                isFirst = false;
            }
    
            jsonResponseBuilder.append("  {\n");
            jsonResponseBuilder.append("    \"account\": \"").append(accountData.getAccountNumber()).append("\",\n");
            jsonResponseBuilder.append("    \"debitCount\": ").append(accountData.getDebitCount()).append(",\n");
            jsonResponseBuilder.append("    \"creditCount\": ").append(accountData.getCreditCount()).append(",\n");
            jsonResponseBuilder.append("    \"balance\": ").append(decimalFormat.format(accountData.getBalance())).append("\n");
            jsonResponseBuilder.append("  }");
        }
    
        jsonResponseBuilder.append("\n]");
        return jsonResponseBuilder.toString();
    }    
}

