package com.imc.intern.trading;

import com.imc.intern.exchange.client.ExchangeClient;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import com.imc.intern.exchange.client.RemoteExchangeView;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;
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
        if (exchangeView.getTrades() != null) {
            tacoTrader.setPosition(exchangeView.getTrades());
        }

        int limit = 100;

        exchangeView.subscribe(Symbol.of(TACO), tacoTrader);
        exchangeView.subscribe(Symbol.of(BEEF), tacoTrader);
        exchangeView.subscribe(Symbol.of(TORT), tacoTrader);
    }
}
