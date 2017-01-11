package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.lang.Math; // msanders: let's try to get rid of these types of unused imports
import java.util.*;

public class Main
{
    private static final String EXCHANGE_URL = "tcp://wintern.imc.com:61616";
    private static final String USERNAME = "arao";
    private static final String PASSWORD = "string peace kids drawn";
    private static final String BOOK = "ARA1";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception
    {
        ExchangeClient client = ExchangeClient.create(EXCHANGE_URL, Account.of(USERNAME), PASSWORD);
        double price = 20.0;
        int volume = 10;
        int positionThreshold = 20;

        client.start();

        PositionTracker positionTracker = new PositionTracker();
        RemoteExchangeView exchangeView = client.getExchangeView();
        exchangeView.massCancel(Symbol.of(BOOK));

        // msanders: the new OrderBookHandler() code should probably be moved to a separate class :)
        exchangeView.subscribe(Symbol.of(BOOK), new OrderBookHandler() {
            public void handleRetailState(RetailState retailState) {
                System.out.println(retailState);

                Trader.checkRestingOrders(Symbol.of(BOOK), exchangeView, positionTracker, Side.BUY, price, retailState.getBids());
                Trader.checkRestingOrders(Symbol.of(BOOK), exchangeView, positionTracker, Side.SELL, price, retailState.getAsks());

                Trader.handlePosition(positionTracker);
            }

            public void handleOwnTrade(OwnTrade trade) {
                System.out.println("HANDLE OWN TRADE");
                System.out.println(trade);

                int vol = trade.getVolume();
                if (trade.getSide().equals(Side.SELL)) {
                    vol *= -1;
                }

                positionTracker.changePosition(vol);
                System.out.println(positionTracker.getPosition());
                positionTracker.changed = true;
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
    }
}
