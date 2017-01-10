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
public class ExchangeTracker {
    HashMap<Long, Order> exchange;

    public ExchangeTracker() {
        exchange = new HashMap<Long, Order>();
    }

    public long addOrder(Long orderId, Order order) {
        exchange.put(orderId, order);
        return orderId;
    }
}
