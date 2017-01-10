package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.Symbol;
/**
 * Created by imc on 10/01/2017.
 */
public class Order {
    //retailState.getBook(),
    //bid.getPrice(), bid.getVolume(), OrderType.IMMEDIATE_OR_CANCEL, Side.SELL

    int volume;
    double price;
    OrderType orderType;
    Side side;

    public Order(Symbol symbol, double p, int vol, OrderType type, Side s) {
        volume = vol;
        price = p;
        orderType = type;
        side = s;
    }
}
