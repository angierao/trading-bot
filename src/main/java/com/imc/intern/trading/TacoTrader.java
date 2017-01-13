package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import sun.jvm.hotspot.utilities.IntegerEnum;
import sun.rmi.runtime.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.Math;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

public class TacoTrader implements OrderBookHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TacoTrader.class);
    private final Symbol TACO = Symbol.of("TACO");
    private final Symbol BEEF = Symbol.of("BEEF");
    private final Symbol TORT = Symbol.of("TORT");

    /*
    cproctor: There is a lot of duplication in this class. A better structure would be to create some sort of Book that
    keeps track of the highest bid and ask, volume, adjustment, ..etc. It would clan up this class nicely.
     */
    private RemoteExchangeView exchangeView;
    private double highestTacoBid;
    private double lowestTacoAsk;
    private double highestBeefBid;
    private double lowestBeefAsk;
    private double highestTortBid;
    private double lowestTortAsk;

    private int tacoVol;
    private int beefVol;
    private int tortVol;

    private double adjustment;
    private double tacoAdjustment;
    private double beefAdjustment;
    private double tortAdjustment;

    private double offset;
    private double tacoOffset;
    private double beefOffset;
    private double tortOffset;

    private double tacoFairValue;
    private double beefFairValue;
    private double tortFairValue;

    private int orderCount;
    private int GTCCount;

    //private Date timeOfOrder;
    private long lastOrderTime;

    private int positionThreshold;

    private PositionTracker tracker;

    private boolean getEven = false;

    private boolean GTCs;

    private int secondLimit = 10;
    private int volumeLimit = 100;

    private TreeMap<Double, RetailState.Level> tacoBids;
    private TreeMap<Double, RetailState.Level> tacoAsks;
    private TreeMap<Double, RetailState.Level> beefBids;
    private TreeMap<Double, RetailState.Level> beefAsks;
    private TreeMap<Double, RetailState.Level> tortBids;
    private TreeMap<Double, RetailState.Level> tortAsks;

    DateFormat df;

    public TacoTrader(RemoteExchangeView view) {
        exchangeView = view;
        highestTacoBid = 0.0;
        highestBeefBid = 0.0;
        highestTortBid = 0.0;
        lowestTacoAsk = 0.0;
        lowestBeefAsk = 0.0;
        lowestTortAsk = 0.0;

        tacoVol = 0;
        beefVol = 0;
        tortVol = 0;

        tacoAdjustment = 0.0;
        beefAdjustment = 0.0;
        tortAdjustment = 0.0;

        adjustment = 0.0;
        offset = 0.05;
        positionThreshold = 50;

        df = new SimpleDateFormat("mm:ss");
        lastOrderTime = System.currentTimeMillis();

        tacoBids = new TreeMap<>();
        beefBids = new TreeMap<>();
        tortBids = new TreeMap<>();

        tacoAsks = new TreeMap<>();
        beefAsks = new TreeMap<>();
        tortAsks = new TreeMap<>();

        orderCount = 0;
        GTCCount = 0;
        GTCs = true;

        tracker = new PositionTracker();
    }

    public void setPosition(Map<Symbol, List<OwnTrade>> myTrades) {
        List<OwnTrade> tacoTrades = myTrades.getOrDefault(TACO, Collections.emptyList());

        for (OwnTrade trade: tacoTrades) {
            if (trade.getSide().equals(Side.BUY)) {
                tracker.changeTacoPosition(trade.getVolume());
            }
            else {
                tracker.changeTacoPosition(-1*trade.getVolume());
            }
        }

        List<OwnTrade> beefTrades = myTrades.getOrDefault(BEEF, Collections.emptyList());

        for (OwnTrade trade: beefTrades) {
            if (trade.getSide().equals(Side.BUY)) {
                tracker.changeBeefPosition(trade.getVolume());
            }
            else {
                tracker.changeBeefPosition(-1*trade.getVolume());
            }
        }

        List<OwnTrade> tortTrades = myTrades.getOrDefault(TORT, Collections.emptyList());;

        for (OwnTrade trade: tortTrades) {
            if (trade.getSide().equals(Side.BUY)) {
                tracker.changeTortPosition(trade.getVolume());
            }
            else {
                tracker.changeTortPosition(-1*trade.getVolume());
            }
        }




    }
    @Override
    public void handleRetailState(RetailState retailState) {
        System.out.println(retailState);
        long currentTime = System.currentTimeMillis();
        long milliTime30SecondsAgo = currentTime - TimeUnit.SECONDS.toMillis(secondLimit);
        /*
            cproctor: Why do you check to see if the times are equal?

        if (prevMin*60+prevSec != currMin*60+currSec) {
            orderCount = 0;
        }*/


        if (!getEven) {
            updateStateForRetailState(retailState);
            arbitrage();
        }
        else {
            getEven(tracker);
        }
    }

    @Override
    public void handleOwnTrade(OwnTrade trade) {
        System.out.println("MY TRADE");
        tracker.changePosition(trade);
        handlePosition(tracker, trade);

        System.out.println(trade);
    }

    @Override
    public void handleTrade(Trade trade) {
        System.out.println(trade);
    }

    @Override
    public void handleError(Error error) {
        System.out.println(error);
    }

    @Override
    public void handleExposures(ExposureUpdate exposures) {
        System.out.println(exposures);
    }

    public void checkRestingOrders(RetailState retailState) {

    }

    public void getEven(PositionTracker tracker) {
        int tacoPosition = tracker.getTacoPosition();
        int beefPosition = tracker.getBeefPosition();
        int tortPosition = tracker.getTortPosition();

        int tentTaco = tacoPosition;
        int tentBeef = beefPosition;
        int tentTort = tortPosition;

        long currentTime = System.currentTimeMillis();
        long milliTime30SecondsAgo = currentTime - TimeUnit.SECONDS.toMillis(secondLimit);

        if (lastOrderTime > milliTime30SecondsAgo) {
            while (tentTort != tentBeef || tentBeef != -1*tentTaco || tentTort != -1*tentTaco) {
                if (tentTort > -1*tentTaco) {
                    if (tentTort >= -1*tentTaco + volumeLimit && lowestTortAsk != 0) {
                        sendOrder(TORT, lowestTortAsk, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentTort -= volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;

                    }
                    else if (lowestTortAsk != 0) {
                        sendOrder(TORT, lowestTortAsk, Math.abs(Math.abs(tentTort) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentTort -= Math.abs(Math.abs(tentTort) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }

                }
                else {
                    // buy tort
                    if (tentTort + volumeLimit <= -1*tentTaco && highestTortBid != 0) {
                        sendOrder(TORT, highestTortBid, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentTort += volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else if (highestTortBid != 0) {
                        sendOrder(TORT, highestTortBid, Math.abs(Math.abs(tentTort) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentTort += Math.abs(Math.abs(tentTort) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                }

                if (tentBeef > -1*tentTaco) {
                    // sell beef
                    if (tentBeef >= -1*tentTaco + volumeLimit && lowestTortAsk != 0) {
                        sendOrder(TORT, lowestTortAsk, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentBeef -= volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else if (lowestTortAsk != 0) {
                        sendOrder(TORT, lowestTortAsk, Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentBeef -= Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                }
                else {
                    // buy beef
                    if (tentBeef + volumeLimit <= -1*tentTaco && highestTortBid != 0) {
                        //sendOrder(TORT, highestTortBid, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentBeef += volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else if (highestTortBid != 0) {
                        //sendOrder(TORT, highestTortBid, Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco)),
                                //OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentBeef += Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                }
            }
        }
    }

    public void handlePosition(PositionTracker tracker, OwnTrade trade) {

        tracker.changePosition(trade);

        int ingredientPosition;
        Symbol ingredientSymbol;

        int tacoPosition = tracker.getTacoPosition();
        int beefPosition = tracker.getBeefPosition();
        int tortPosition = tracker.getTortPosition();

        int absBeef = Math.abs(beefPosition);
        int absTort = Math.abs(tortPosition);

        if (tacoPosition > 0 && beefPosition < 0 && tortPosition < 0) {

            if (Math.abs(tacoPosition - absBeef) < Math.abs(tacoPosition - absTort)) {

                if (Math.abs(tacoPosition - absBeef) > positionThreshold) {
                    // move tort toward beef
                    if (tortPosition > beefPosition) {
                        tortAdjustment -= .05;
                    }
                    else {
                        tortAdjustment += .05;
                    }
                }
            }
            else {
                if (Math.abs(tacoPosition - absTort) > positionThreshold) {
                    // move beef toward tort
                    if (tortPosition > beefPosition) {
                        beefAdjustment += .05;
                    }
                    else {
                        beefAdjustment -= .05;
                    }
                }
            }
        }
        else if (tacoPosition < 0 && beefPosition > 0 && tortPosition > 0) {
            if (Math.abs(Math.abs(tacoPosition) - beefPosition) < Math.abs(Math.abs(tacoPosition) - tortPosition)) {
                if (Math.abs(Math.abs(tacoPosition) - beefPosition) > positionThreshold) {
                    if (tortPosition > beefPosition) {
                        tortAdjustment -= .05;
                    }
                    else {
                        tortAdjustment += .05;
                    }
                }
            }
            else {
                if (tortPosition > beefPosition) {
                    beefAdjustment += .05;
                }
                else {
                    beefAdjustment -= .05;
                }
            }
        }
    }

    public void arbitrage() {
        int maxVol = Math.min(tacoVol, Math.min(beefVol, tortVol));

        if (maxVol > volumeLimit) {
            maxVol = volumeLimit;
        }
        /*
        cproctor: There's a lot of duplication here
         */

        long currentTime = System.currentTimeMillis();
        long milliTime30SecondsAgo = currentTime - TimeUnit.SECONDS.toMillis(secondLimit);
        LOGGER.info("HELLO {} {} {} {}", maxVol, tacoVol, beefVol, tortVol);
        LOGGER.info("{} {}", milliTime30SecondsAgo, lastOrderTime);

        //if (milliTime30SecondsAgo > lastOrderTime && orderCount == 0)
        if (milliTime30SecondsAgo < lastOrderTime && maxVol != 0)
        {
            LOGGER.info("FIRST LEVEL");
            if (highestTacoBid != 0 && lowestBeefAsk != 0 && lowestTortAsk != 0) {
                LOGGER.info("sell taco buy beef buy tort: ${}", highestTacoBid - lowestBeefAsk - lowestTortAsk);
                if (highestTacoBid > (lowestBeefAsk + lowestTortAsk) && maxVol > 0) {
                    System.out.println("ARBITRAGE1");

                    sendOrder(TACO, highestTacoBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                    sendOrder(BEEF, lowestBeefAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                    sendOrder(TORT, lowestTortAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);

                    lastOrderTime = System.currentTimeMillis();
                    //orderCount += 3;
                }
            }
            else if (lowestTacoAsk != 0 && highestBeefBid != 0 && highestTortBid != 0) {
                LOGGER.info("buy taco sell beef sell tort: ${}", -lowestTacoAsk + highestBeefBid + highestTortBid);
                if (lowestTacoAsk < (highestBeefBid + highestTortBid) && maxVol > 0) {
                    System.out.println("ARBITRAGE2");

                    sendOrder(TACO, lowestTacoAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                    sendOrder(BEEF, highestBeefBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                    sendOrder(TORT, highestTortBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);

                    lastOrderTime = System.currentTimeMillis();
                    //orderCount += 3;
                }
            }
        }

    }

    public void handleTacoPosition(PositionTracker tracker) {
        int currentPosition = tracker.getTacoPosition();

        if (currentPosition > positionThreshold) {
            System.out.println("LOWERING ADJUSTMENT");
            tacoAdjustment -= .05;
        } else if (currentPosition < (-1 * positionThreshold)) {
            System.out.println("INCREASING ADJUSTMENT");
            tacoAdjustment += .05;
        } else {
            tacoAdjustment = 0.0;
        }
    }

    public void handleBeefPosition(PositionTracker tracker) {
        int currentPosition = tracker.getBeefPosition();

        if (currentPosition > positionThreshold) {
            System.out.println("LOWERING ADJUSTMENT");
            beefAdjustment -= .05;
        } else if (currentPosition < (-1 * positionThreshold)) {
            System.out.println("INCREASING ADJUSTMENT");
            beefAdjustment += .05;
        } else {
            beefAdjustment = 0.0;
        }
    }

    public void handleTortPosition(PositionTracker tracker) {
        int currentPosition = tracker.getTortPosition();

        if (currentPosition > positionThreshold) {
            System.out.println("LOWERING ADJUSTMENT");
            tortAdjustment -= .05;
        } else if (currentPosition < (-1 * positionThreshold)) {
            System.out.println("INCREASING ADJUSTMENT");
            tortAdjustment += .05;
        } else {
            tortAdjustment = 0.0;
        }
    }

    /*
        cproctor: If you pull out a Book per symbol, all of this filtering and duplication goes away. At minimum, pull
        out helper methods.
     */
    private void updateStateForRetailState(RetailState retailState) {

        int bidVol = 0;
        int askVol = 0;

        if (retailState.getBook().equals(TACO)) {

            /*
                cproctor: What is the highest level goes away? You get a retail state with volume 0, which is smaller
                than your volume limit. You can't hit a level with volume 0, you actually want the next level, if it
                exists. Additionally, the retail state only shows changed levels. You need to keep track of the book in
                order to see what is available.
             */
            List<RetailState.Level> bids = retailState.getBids();

            for (RetailState.Level bid: bids) {
                tacoBids.put(bid.getPrice(), bid);

            }

            if (tacoBids.size() > 0) {
                /*
                LOGGER.info("TACO BIDS");
                System.out.println(tacoBids);
                LOGGER.info(Double.toString(tacoBids.lastKey()));
                LOGGER.info(Double.toString(tacoBids.firstKey()));*/

                highestTacoBid = tacoBids.lastKey();
                bidVol = tacoBids.get(highestTacoBid).getVolume();

            }

            List<RetailState.Level> asks = retailState.getAsks();

            for (RetailState.Level ask: asks) {
                tacoAsks.put(ask.getPrice(), ask);
            }

            if (tacoAsks.size() > 0) {

                lowestTacoAsk = tacoAsks.firstKey();
                askVol = tacoAsks.get(lowestTacoAsk).getVolume();

                //LOGGER.info(Double.toString(tacoBids.lastKey()));
                //LOGGER.info(Double.toString(tacoBids.firstKey()));

            }

            tacoVol = Math.min(askVol, bidVol);

            for(Map.Entry<Double, RetailState.Level> entry : tacoBids.entrySet()) {
                RetailState.Level bid = entry.getValue();
                if (tacoBids.get(bid.getPrice()).getVolume() == 0) {
                    tacoBids.remove(bid.getPrice());
                }
            }

            LOGGER.info("TACO ASKS");
            System.out.println(tacoAsks.entrySet());

            // Get a set of the entries
            Set<Map.Entry<Double, RetailState.Level>> set = tacoAsks.entrySet();

            // Get an iterator
            Iterator<Map.Entry<Double, RetailState.Level>> it = set.iterator();

            // Display elements
            while(it.hasNext()) {
                Map.Entry<Double, RetailState.Level> me = (Map.Entry<Double, RetailState.Level>)it.next();

                RetailState.Level ask = me.getValue();
                if (tacoAsks.get(ask.getPrice()).getVolume() == 0) {
                    tacoAsks.remove(ask.getPrice());
                }

            }

        }
        else if (retailState.getBook().equals(BEEF)) {
            List<RetailState.Level> bids = retailState.getBids();

            for (RetailState.Level bid: bids) {
                beefBids.put(bid.getPrice(), bid);
            }


            if (beefBids.size() > 0) {
                /*
                LOGGER.info("BEEF BIDS");
                System.out.println(beefBids);
                LOGGER.info(Double.toString(beefBids.lastKey()));
                LOGGER.info(Double.toString(beefBids.firstKey()));*/

                highestBeefBid = beefBids.lastKey();
                bidVol = beefBids.get(highestBeefBid).getVolume();

            }

            List<RetailState.Level> asks = retailState.getAsks();

            for (RetailState.Level ask: asks) {
                beefAsks.put(ask.getPrice(), ask);
            }

            if (beefAsks.size() > 0) {

                lowestBeefAsk = beefAsks.firstKey();
                askVol = beefAsks.get(lowestBeefAsk).getVolume();

            }
            else {
                LOGGER.info("NO TACO ASK WHAT");
                lowestBeefAsk = 0.0;
            }
            beefVol = Math.min(askVol, bidVol);

            for(Map.Entry<Double, RetailState.Level> entry : beefBids.entrySet()) {
                RetailState.Level bid = entry.getValue();
                if (beefBids.get(bid.getPrice()).getVolume() == 0) {
                    beefBids.remove(bid.getPrice());
                }
            }

            LOGGER.info("BEEF ASKS");
            System.out.println(beefAsks.entrySet());

            // Get a set of the entries
            Set<Map.Entry<Double, RetailState.Level>> set = beefAsks.entrySet();

            // Get an iterator
            Iterator<Map.Entry<Double, RetailState.Level>> it = set.iterator();

            // Display elements
            while(it.hasNext()) {
                Map.Entry<Double, RetailState.Level> me = (Map.Entry<Double, RetailState.Level>)it.next();

                RetailState.Level ask = me.getValue();
                if (beefAsks.get(ask.getPrice()).getVolume() == 0) {
                    beefAsks.remove(ask.getPrice());
                }

            }

        }
        else {

            List<RetailState.Level> bids = retailState.getBids();

            for (RetailState.Level bid: bids) {
                tortBids.put(bid.getPrice(), bid);
            }

            if (tortBids.size() > 0) {
                //highestTortBid = tortBids.lastKey();

                //LOGGER.info(Double.toString(tortBids.lastKey()));
                //LOGGER.info(Double.toString(tortBids.firstKey()));

                //bidVol = tortBids.get(highestTortBid).getVolume();
                /*
                LOGGER.info("TORT BIDS");
                System.out.println(tortBids);
                LOGGER.info(Double.toString(tortBids.lastKey()));
                LOGGER.info(Double.toString(tortBids.firstKey()));*/

                highestTortBid = tortBids.lastKey();
                bidVol = tortBids.get(highestTortBid).getVolume();

            }
            else {
                LOGGER.info("NO TACO BID WHAT");
                highestTortBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();

            for (RetailState.Level ask: asks) {
                tortAsks.put(ask.getPrice(), ask);
            }

            if (tortAsks.size() > 0) {
                lowestTortAsk = tortAsks.firstKey();
                askVol = tortAsks.get(lowestTortAsk).getVolume();

            }

            tortVol = Math.min(askVol, bidVol);

            for(Map.Entry<Double, RetailState.Level> entry : tortBids.entrySet()) {
                RetailState.Level bid = entry.getValue();
                if (tortBids.get(bid.getPrice()).getVolume() == 0) {
                    tortBids.remove(bid.getPrice());
                }
            }

            LOGGER.info("TORT ASKS");
            System.out.println(tortAsks.entrySet());

            // Get a set of the entries
            Set<Map.Entry<Double, RetailState.Level>> set = tortAsks.entrySet();

            // Get an iterator
            Iterator<Map.Entry<Double, RetailState.Level>> it = set.iterator();

            // Display elements
            while(it.hasNext()) {
                Map.Entry<Double, RetailState.Level> me = (Map.Entry<Double, RetailState.Level>) it.next();

                RetailState.Level ask = me.getValue();
                if (tortAsks.get(ask.getPrice()).getVolume() == 0) {
                    tortAsks.remove(ask.getPrice());
                }

            }

        }
    }

    public void sendOrder(Symbol symbol, double price, int vol, OrderType type, Side side) {
        exchangeView.createOrder(symbol, price, vol, type, side);
    }

    public double updateHighestTacoBid(double newBid) {
        highestTacoBid = newBid;
        return highestTacoBid;
    }

    public double updateHighestBeefBid(double newBid) {
        highestBeefBid = newBid;
        return highestBeefBid;
    }

    public double updateHighestTortBid(double newBid) {
        highestTortBid = newBid;
        return highestTortBid;
    }

    public double updateLowestTacoBid(double newBid) {
        lowestTacoAsk = newBid;
        return lowestTacoAsk;
    }

    public double updateLowestBeefBid(double newBid) {
        lowestBeefAsk = newBid;
        return lowestBeefAsk;
    }

    public double updateLowestTortBid(double newBid) {
        lowestTortAsk = newBid;
        return lowestTortAsk;
    }

}
