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
    int myPosition;
    boolean changed = false;
    HashMap<Long, Order> myOrders;

    public PositionTracker() {
        myPosition = 0;
        myOrders = new HashMap<Long, Order>();
    }

    public long addOrder(Long orderId, Order order) {
        myOrders.put(orderId, order);
        return orderId;
    }

    public int changePosition(int change) {
        myPosition += change;
        return myPosition;
    }

    public int getPosition() {
        return myPosition;
    }
}
