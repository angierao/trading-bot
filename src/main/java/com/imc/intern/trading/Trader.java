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

    RemoteExchangeView exchangeView;
    Symbol book;
    int positionThreshold = 50;
    double offset = .05;
    double adjustment = 0.0;

    public Trader(RemoteExchangeView view, Symbol symbol, int threshold, double off, double adj) {
        exchangeView = view;
        book = symbol;
        positionThreshold = 50;
        offset = off;
        adjustment = adj;
    }

    /*
    Would have been nice to reuse this instead of building a new taco trader
     */
    public void checkRestingOrders(Symbol symb, RemoteExchangeView view, PositionTracker tracker, Side side,
                                          double fairValue, List<RetailState.Level> restingOrders) {
        exchangeView = view;
        book = symb;
        for (RetailState.Level order: restingOrders) {
            if (order.getVolume() > 0) {
                if (side.equals(Side.BUY) && order.getPrice() >= fairValue + offset + adjustment ||
                        side.equals(Side.SELL) && Math.round(order.getPrice()*100.0)/100.0 <= fairValue - offset + adjustment) {

                    Side actionSide = Side.BUY;
                    if (side.equals(Side.BUY)) {
                        actionSide = Side.SELL;
                    }

                    sendOrder(book, order.getPrice(), order.getVolume(), OrderType.GOOD_TIL_CANCEL, actionSide);
                }
            }
        }
    }

    public void sendOrder(Symbol symbol, double price, int vol, OrderType type, Side side) {
        exchangeView.createOrder(symbol, price, vol, type, side);
    }

    public void handlePosition(PositionTracker tracker) {
        int currentPosition = tracker.getPosition();

        System.out.println(adjustment);

        // We need to sell
        if (currentPosition > positionThreshold) {
            System.out.println("LOWERING ADJUSTMENT");
            adjustment += -.05;
        }
        else if (currentPosition < (-1*positionThreshold)) {
            System.out.println("INCREASING ADJUSTMENT");
            adjustment += .05;
        }
        else {
            adjustment = 0.0;
        }
    }

    public void checkTacos() {
        /*
        if (highestTacoBid > (lowestBeefAsk + lowestTortAsk)) {
            buy beef and tort
            sell taco
        }
        else if (lowestTacoAsk < (highestBeefBid + highestTortBid)) {
        }
         */
    }
    public void diming(Symbol symb, RemoteExchangeView view, PositionTracker tracker, Side side,
                              double fairValue, List<RetailState.Level> restingOrders) {
        exchangeView = view;

        if (restingOrders.size() > 0) {
            RetailState.Level order = restingOrders.get(0);
            if (side.equals(Side.BUY) && order.getPrice() > fairValue + offset) {
                sendOrder(book, order.getPrice() + offset, order.getVolume(), OrderType.GOOD_TIL_CANCEL, side);
            }
            else if (side.equals(Side.SELL) && order.getPrice() < fairValue - offset) {
                sendOrder(book, order.getPrice() - offset, order.getVolume(), OrderType.GOOD_TIL_CANCEL, side);
            }
        }
        else {
            // figure out what to do here
        }
    }


}
