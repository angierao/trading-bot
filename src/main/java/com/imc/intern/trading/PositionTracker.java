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

/**
 * Created by imc on 10/01/2017.
 */
public class PositionTracker {
    //HashMap<Long, ExposureUpdate> exchange;
    //int sharesBought = 0;
    //int sharesSold = 0;
    private int myPosition;
    private int myTacoPosition;
    private int myBeefPosition;
    private int myTortPosition;
    private boolean changed;
    HashMap<Long, Order> myOrders;

    public PositionTracker() {
        myPosition = 0;
        changed = false;
        myOrders = new HashMap<Long, Order>();
    }

    public long addOrder(Long orderId, Order order) {
        myOrders.put(orderId, order);
        return orderId;
    }

    public int changePosition(OwnTrade trade) {
        int vol = trade.getVolume();
        if (trade.getSide().equals(Side.SELL)) {
            vol *= -1;
        }

        myPosition += vol;
        changed = true;
        return myPosition;
    }

    public int getPosition() {
        return myPosition;
    }

    public int getTacoPosition() {
        return myTacoPosition;
    }
}
