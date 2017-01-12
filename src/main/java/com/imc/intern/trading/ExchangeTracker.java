package com.imc.intern.trading;

import java.util.HashMap;

/**
 * Created by imc on 10/01/2017.
 */
public class ExchangeTracker {
    HashMap<Long, Order> exchange;

    public ExchangeTracker() {
        exchange = new HashMap<>();
    } // intellij is suggesting you can' use the diamond operator here: http://www.javaworld.com/article/2074080/core-java/jdk-7--the-diamond-operator.html

    public long addOrder(Long orderId, Order order) {
        exchange.put(orderId, order);
        return orderId;
    }
}
