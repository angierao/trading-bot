package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

public class Main
{
    private static final String EXCHANGE_URL = "tcp://wintern.imc.com:61616";
    private static final String USERNAME = "arao";
    private static final String PASSWORD = "string peace kids drawn";
    private static final String BOOK = "ARA1";
    private static final String TACO = "ARA.TACO";
    private static final String BEEF = "ARA.BEEF";
    private static final String TORT = "ARA.TORT";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception
    {
        ExchangeClient client = ExchangeClient.create(EXCHANGE_URL, Account.of(USERNAME), PASSWORD);
        double price = 20.0;
        int volume = 10;
        double highestBeefBid = 0.;
        double lowestBeefAsk = 0.;
        double highestTortBid = 0.;
        double lowestTortAsk = 0.;

        client.start();

        PositionTracker positionTracker = new PositionTracker();
        RemoteExchangeView exchangeView = client.getExchangeView();
        exchangeView.massCancel(Symbol.of(BOOK));

        TacoTrader tacoTrader = new TacoTrader(exchangeView);
        exchangeView.subscribe(Symbol.of(TACO), new OrderBookHandler() {
            public void handleRetailState(RetailState retailState) {
                System.out.println(retailState);

                tacoTrader.parseTacoRetailState(retailState);
                tacoTrader.arbitrage();
            }

            public void handleOwnTrade(OwnTrade trade) {
                positionTracker.changePosition(trade);
                System.out.println(positionTracker.getPosition());
            }

            public void handleTrade(Trade trade) {
                //System.out.println("HANDLE TRADE");
                System.out.println(trade);
            }

            public void handleError(Error error){
                System.out.println("ERROR");
                System.out.println(error);
            }

            @Override
            public void handleExposures(ExposureUpdate exposures) {
                System.out.println(exposures);
                for (Exposure exposure: exposures.getExposures()) {
                    long orderId = exposure.getOrderId();
                }
            }

        });

        exchangeView.subscribe(Symbol.of(BEEF), new OrderBookHandler() {
            public void handleRetailState(RetailState retailState) {
                System.out.println(retailState);

                tacoTrader.parseBeefRetailState(retailState);
                tacoTrader.arbitrage();
            }

            public void handleOwnTrade(OwnTrade trade) {
                positionTracker.changePosition(trade);
                System.out.println(positionTracker.getPosition());
            }

            public void handleTrade(Trade trade) {
                //System.out.println("HANDLE TRADE");
                System.out.println(trade);
            }

            public void handleError(Error error){
                System.out.println("ERROR");
                System.out.println(error);
            }

            @Override
            public void handleExposures(ExposureUpdate exposures) {
                System.out.println(exposures);
                for (Exposure exposure: exposures.getExposures()) {
                    long orderId = exposure.getOrderId();
                }
            }

        });

        exchangeView.subscribe(Symbol.of(TORT), new OrderBookHandler() {
            public void handleRetailState(RetailState retailState) {
                System.out.println(retailState);

                tacoTrader.parseTortRetailState(retailState);
                tacoTrader.arbitrage();
            }

            public void handleOwnTrade(OwnTrade trade) {
                positionTracker.changePosition(trade);
                System.out.println(positionTracker.getPosition());
            }

            public void handleTrade(Trade trade) {
                //System.out.println("HANDLE TRADE");
                System.out.println(trade);
            }

            public void handleError(Error error){
                System.out.println("ERROR");
                System.out.println(error);
            }

            @Override
            public void handleExposures(ExposureUpdate exposures) {
                System.out.println(exposures);
                for (Exposure exposure: exposures.getExposures()) {
                    long orderId = exposure.getOrderId();
                    /*
                    if (positionTracker.myOrders.get(orderId) != null) {
                        positionTracker.myOrders.remove(orderId);
                        positionTracker.myOrders.put(orderId,
                                new OwnTrade(Symbol.of(BOOK), Account.of(USERNAME), orderId, exposure.getPrice(),
                                        exposure.getRemaining(), exposure.getSide(), exposure.getTimestamp()));
                    }*/

                }
            }

        });

        // msanders: the new OrderBookHandler() code should probably be moved to a separate class :)
        /*
        exchangeView.subscribe(Symbol.of(BOOK), new OrderBookHandler() {
            public void handleRetailState(RetailState retailState) {
                System.out.println(retailState);

                Trader.checkRestingOrders(Symbol.of(BOOK), exchangeView, positionTracker, Side.BUY, price, retailState.getBids());
                Trader.checkRestingOrders(Symbol.of(BOOK), exchangeView, positionTracker, Side.SELL, price, retailState.getAsks());

                Trader.diming(Symbol.of(BOOK), exchangeView, positionTracker, Side.BUY, price, retailState.getBids());
                Trader.diming(Symbol.of(BOOK), exchangeView, positionTracker, Side.BUY, price, retailState.getAsks());

                Trader.handlePosition(positionTracker);
            }

            public void handleOwnTrade(OwnTrade trade) {
                positionTracker.changePosition(trade);
                System.out.println(positionTracker.getPosition());
            }

            public void handleTrade(Trade trade) {
                //System.out.println("HANDLE TRADE");
                System.out.println(trade);
            }

            public void handleError(Error error){
                System.out.println("ERROR");
                System.out.println(error);
            }

            // msanders: I noticed you added a logger above -- let's switch stuff over to actually use the logger
            //           instead of println statements
            @Override
            public void handleExposures(ExposureUpdate exposures) {
                System.out.println(exposures);
                for (Exposure exposure: exposures.getExposures()) {
                    long orderId = exposure.getOrderId();

                    if (positionTracker.myOrders.get(orderId) != null) {
                        positionTracker.myOrders.remove(orderId);
                        positionTracker.myOrders.put(orderId,
                                new OwnTrade(Symbol.of(BOOK), Account.of(USERNAME), orderId, exposure.getPrice(),
                                        exposure.getRemaining(), exposure.getSide(), exposure.getTimestamp()));
                    }

                }
            }

        });*/
    }
}
