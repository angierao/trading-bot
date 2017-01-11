package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.awt.print.Book;
import java.util.*;

/**
 * Created by imc on 10/01/2017.
 */
// ONLY THINGS THAT HAVE ALREADY HAPPENED!!!!!
public class PositionTracker implements OrderBookHandler {
    private final Symbol TACO = Symbol.of("TACO");
    private final Symbol BEEF = Symbol.of("BEEF");
    private final Symbol TORT = Symbol.of("TORT");

    private int myPosition;
    private int myTacoPosition;
    private int myBeefPosition;
    private int myTortPosition;
    private boolean changed;
    //HashMap<Long, Order> myOrders;
    //private TreeMap<Long, Order> myBids;
    //private TreeMap<Long, Order> myAsks;

    public PositionTracker() {
        myTacoPosition = 0;
        myBeefPosition = 0;
        myTortPosition = 0;
        changed = false;
        //myBids = new TreeMap<Long, Order>();
        //myAsks = new TreeMap<Long, Order>();
    }

    @Override
    public void handleOwnTrade(OwnTrade trade) {
        changePosition(trade);
    }

    public long addBid(Long orderId, Order order) {
        //myBids.put(orderId, order);
        return orderId;
    }


    public void changePosition(OwnTrade trade) {
        int vol = trade.getVolume();

        if (trade.getSide().equals(Side.SELL)) {
            vol *= -1;
        }

        Symbol book = trade.getBook();
        if (book.equals(TACO)) {
            myTacoPosition += vol;
        }
        else if (book.equals(BEEF)) {
            myBeefPosition += vol;
        }
        else {
            myTortPosition += vol;
        }

        changed = true;
    }

    public int getPosition() {
        return myPosition;
    }

    public int getTacoPosition() {
        return myTacoPosition;
    }

    public int getBeefPosition() { return myBeefPosition; }

    public int getTortPosition() { return myTortPosition; }
}
