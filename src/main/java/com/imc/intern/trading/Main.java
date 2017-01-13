package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.*;

public class Main
{
    private static final String EXCHANGE_URL = "tcp://54.227.125.23:61616";
    private static final String USERNAME = "arao";
    private static final String PASSWORD = "string peace kids drawn";
    private static final String BOOK = "ARA1";
    private static final String TACO = "TACO";
    private static final String BEEF = "BEEF";
    private static final String TORT = "TORT";


    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception
    {
        ExchangeClient client = ExchangeClient.create(EXCHANGE_URL, Account.of(USERNAME), PASSWORD);

        client.start();

        PositionTracker positionTracker = new PositionTracker();
        RemoteExchangeView exchangeView = client.getExchangeView();
        exchangeView.massCancel(Symbol.of(TACO));
        exchangeView.massCancel(Symbol.of(BEEF));
        exchangeView.massCancel(Symbol.of(TORT));

        TacoTrader tacoTrader = new TacoTrader(exchangeView);
        exchangeView.createOrder(Symbol.of(BEEF), 26.05, 100, OrderType.GOOD_TIL_CANCEL, Side.SELL);
        exchangeView.createOrder(Symbol.of(BEEF), 25.95, 100, OrderType.GOOD_TIL_CANCEL, Side.BUY);
        exchangeView.createOrder(Symbol.of(TORT), 15.35, 100, OrderType.GOOD_TIL_CANCEL, Side.BUY);
        exchangeView.createOrder(Symbol.of(TORT), 15.4, 100, OrderType.GOOD_TIL_CANCEL, Side.SELL);

        exchangeView.createOrder(Symbol.of(TACO), 40.65, 100, OrderType.GOOD_TIL_CANCEL, Side.SELL);
        exchangeView.createOrder(Symbol.of(TACO), 40.5, 100, OrderType.GOOD_TIL_CANCEL, Side.BUY);

        /*
        exchangeView.createOrder(Symbol.of(BEEF), 26.05, 10, OrderType.GOOD_TIL_CANCEL, Side.SELL);
        exchangeView.createOrder(Symbol.of(BEEF), 25.95, 10, OrderType.GOOD_TIL_CANCEL, Side.BUY);
        exchangeView.createOrder(Symbol.of(TORT), 15.35, 10, OrderType.GOOD_TIL_CANCEL, Side.BUY);
        exchangeView.createOrder(Symbol.of(TORT), 15.4, 10, OrderType.GOOD_TIL_CANCEL, Side.SELL);

        exchangeView.createOrder(Symbol.of(TACO), 40.65, 10, OrderType.GOOD_TIL_CANCEL, Side.SELL);
        exchangeView.createOrder(Symbol.of(TACO), 40.5, 10, OrderType.GOOD_TIL_CANCEL, Side.BUY);*/

        //exchangeView.createOrder(Symbol.of(TORT), 15.35, 4, OrderType.GOOD_TIL_CANCEL, Side.BUY);

        exchangeView.subscribe(Symbol.of(TACO), tacoTrader);
        exchangeView.subscribe(Symbol.of(BEEF), tacoTrader);
        exchangeView.subscribe(Symbol.of(TORT), tacoTrader);
    }
}
