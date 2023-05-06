package com.EnergySavingBanking;

import com.EnergySavingBanking.atmservice.AtmServiceHandler;
import com.EnergySavingBanking.onlinegame.OnlineGameCalculateHandler;
import com.EnergySavingBanking.transactions.TransactionsReportHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class INGTeslaChallenge {

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            ExecutorService executorService = Executors.newCachedThreadPool();
            
            server.createContext("/atmservice/calculateOrder", new AtmServiceHandler(executorService));

            server.createContext("/transactions/report", new TransactionsReportHandler(executorService));

            server.createContext("/onlinegame/calculate", new OnlineGameCalculateHandler(executorService));

            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
