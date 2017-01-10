package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.lang.Math;

/**
 * Created by imc on 10/01/2017.
 */
public class Trader {

    static RemoteExchangeView exchangeView;
    static Symbol book;
    static int positionThreshold = 50;
    static double offset = .05;
    static double adjustment = 0.0;

    private Trader() {
    }

    public static void checkRestingOrders(Symbol symb, RemoteExchangeView view, PositionTracker tracker, Side side,
                                          double fairValue, List<RetailState.Level> restingOrders) {
        exchangeView = view;
        book = symb;
        for (RetailState.Level order: restingOrders) {
            //System.out.println("HELLO1");
            if (order.getVolume() > 0) {
                //System.out.println("HELLO2");
                //System.out.println(fairValue);
                //System.out.println(offset);
                //System.out.println(adjustment);
                //System.out.println(fairValue - offset + adjustment);
                if (side.equals(Side.BUY) && order.getPrice() >= fairValue + offset + adjustment ||
                        side.equals(Side.SELL) && Math.round(order.getPrice()*100.0)/100.0 <= fairValue - offset + adjustment) {

                    //System.out.println("TAKING ACTION");
                    Side actionSide = Side.BUY;
                    if (side.equals(Side.BUY)) {
                        actionSide = Side.SELL;
                    }

                    sendOrder(book, order.getPrice(), order.getVolume(), OrderType.GOOD_TIL_CANCEL, actionSide);
                    /*
                    int positionChange = order.getVolume();
                    if (side.equals(Side.SELL)) {
                        positionChange *= -1;
                    }
                    tracker.changePosition(positionChange);*/
                }
            }
        }
    }

    public static void sendOrder(Symbol symbol, double price, int vol, OrderType type, Side side) {
        exchangeView.createOrder(symbol, price, vol, type, side);
    }

    public static void handlePosition(PositionTracker tracker) {
        int currentPosition = tracker.getPosition();

        //System.out.println("POSITION");
        System.out.println(adjustment);
        // We need to sell
        if (currentPosition > positionThreshold) {
            System.out.println("LOWERING ADJUSTMENT");
            adjustment += -.05;
        }
        else if (currentPosition < -1*positionThreshold) {
            System.out.println("INCREASING ADJUSTMENT");
            adjustment += .05;
        }
        else {
            adjustment = 0.0;
        }
    }

    public static void diming() {
        /*
                if (retailState.getBids().size() > 0) {
                    double bestBidPrice = retailState.getBids().get(0).getPrice();
                    if (bestBidPrice < price) {
                        client.getExchangeView().createOrder(retailState.getBook(),
                                bestBidPrice + 0.1, volume, OrderType.GOOD_TIL_CANCEL, Side.BUY);
                    }
                }
                if (retailState.getAsks().size() > 0) {
                    double bestAskPrice = retailState.getAsks().get(0).getPrice();
                    if (bestAskPrice > price) {
                        client.getExchangeView().createOrder(retailState.getBook(),
                                bestAskPrice - 0.1, volume, OrderType.GOOD_TIL_CANCEL, Side.SELL);
                    }
                }*/
    }
}
