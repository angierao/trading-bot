package com.imc.intern.trading;

import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.Account;
import com.imc.intern.exchange.datamodel.api.OwnTrade;
import com.imc.intern.exchange.datamodel.api.Symbol;
import org.junit.Test;

import static org.junit.Assert.*;


public class PositionTrackerTest {


    public static final Symbol BAM = Symbol.of("BAM");
    public static final Account ACCOUNT = Account.create();
    public static final long ORDER_ID = 1L;
    public static final double PRICE = 10.0;
    public static final int TIMESTAMP = 1000000;

    @Test
    public void testBuyPositionTracker() {
        PositionTracker tracker = new PositionTracker();

        int oldPosition = tracker.getPosition();

        int volume = 10;
        Side buy = Side.BUY;
        OwnTrade trade = new OwnTrade(BAM, ACCOUNT, ORDER_ID, PRICE, volume, buy, TIMESTAMP);
        //int newPosition = tracker.changePosition(trade);

        //assert oldPosition + volume == newPosition;
    }

    @Test
    public void testSellPositionTracker() {
        PositionTracker tracker = new PositionTracker();

        int oldPosition = tracker.getPosition();

        int volume = 5;
        Side sell = Side.SELL;
        OwnTrade trade = new OwnTrade(BAM, ACCOUNT, ORDER_ID, PRICE, volume, sell, TIMESTAMP);
        //int newPosition = tracker.changePosition(trade);

        //assert oldPosition - volume == newPosition;
    }
}