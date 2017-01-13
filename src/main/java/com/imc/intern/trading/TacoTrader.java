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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.lang.Math;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    private int secondLimit = 30;
    private int volumeLimit = 100;

    private TreeMap<Double, Order> tacoBids;
    private TreeMap<Double, Order> tacoAsks;
    private TreeMap<Double, Order> beefBids;
    private TreeMap<Double, Order> beefAsks;
    private TreeMap<Double, Order> tortBids;
    private TreeMap<Double, Order> tortAsks;

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

        orderCount = 0;
        GTCCount = 0;
        GTCs = true;

        tracker = new PositionTracker();
    }

    @Override
    public void handleRetailState(RetailState retailState) {
        System.out.println(retailState);
        /*
            cproctor: Instead of parsing the minutes and seconds, you could use the time as a long:
            long currentTime = System.currentTimeMillis();
            long milliTime30SecondsAgo = currentTime - TimeUnit.SECONDS.toMillis(30);
         */

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

        if (lastOrderTime < milliTime30SecondsAgo) {
            while (tentTort != tentBeef || tentBeef != -1*tentTaco || tentTort != -1*tentTaco) {
                if (tentTort > -1*tentTaco) {
                    if (tentTort >= -1*tentTaco + volumeLimit) {
                        sendOrder(TORT, lowestTortAsk, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentTort -= volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;

                    }
                    else {
                        sendOrder(TORT, lowestTortAsk, Math.abs(Math.abs(tentTort) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentTort -= Math.abs(Math.abs(tentTort) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }

                }
                else {
                    // buy tort
                    if (tentTort + volumeLimit <= -1*tentTaco) {
                        sendOrder(TORT, highestTortBid, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentTort += volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else {
                        sendOrder(TORT, highestTortBid, Math.abs(Math.abs(tentTort) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentTort += Math.abs(Math.abs(tentTort) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                }

                if (tentBeef > -1*tentTaco) {
                    // sell beef
                    if (tentBeef >= -1*tentTaco + volumeLimit) {
                        sendOrder(TORT, lowestTortAsk, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentBeef -= volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else {
                        sendOrder(TORT, lowestTortAsk, Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.SELL);
                        tentBeef -= Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco));

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                }
                else {
                    // buy beef
                    if (tentBeef + volumeLimit <= -1*tentTaco) {
                        sendOrder(TORT, highestTortBid, volumeLimit, OrderType.GOOD_TIL_CANCEL, Side.BUY);
                        tentBeef += volumeLimit;

                        lastOrderTime = System.currentTimeMillis();
                        GTCCount += 1;
                    }
                    else {
                        sendOrder(TORT, highestTortBid, Math.abs(Math.abs(tentBeef) - Math.abs(tentTaco)),
                                OrderType.GOOD_TIL_CANCEL, Side.BUY);
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

        //if (milliTime30SecondsAgo > lastOrderTime && orderCount == 0)
        if (milliTime30SecondsAgo > lastOrderTime && maxVol != 0)
        {
            LOGGER.info("FIRST LEVEL");
            if (highestTacoBid != 0 && lowestBeefAsk != 0 && lowestTortAsk != 0) {
                LOGGER.info("sell taco buy beef buy tort: ${}", highestTacoBid - lowestBeefAsk - lowestTortAsk);
                if (highestTacoBid > (lowestBeefAsk + lowestTortAsk) && maxVol > 0) {
                    System.out.println("ARBITRAGE1");
                    sendOrder(TACO, highestTacoBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                    sendOrder(BEEF, lowestBeefAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                    sendOrder(TORT, lowestTortAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);

                    lastOrderTime = currentTime;
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

                    lastOrderTime = currentTime;
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
            List<RetailState.Level> bids = retailState.getBids();

            /*
                cproctor: What is the highest level goes away? You get a retail state with volume 0, which is smaller
                than your volume limit. You can't hit a level with volume 0, you actually want the next level, if it
                exists. Additionally, the retail state only shows changed levels. You need to keep track of the book in
                order to see what is available.
             */
            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestTacoBid = firstBid.getPrice();
                bidVol = Math.min(firstBid.getVolume(), volumeLimit);
            }
            else {
                highestTacoBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestTacoAsk = asks.get(0).getPrice();
                askVol = Math.min(firstAsk.getVolume(), volumeLimit);
            }
            else {
                lowestTacoAsk = 0.0;
            }

        tacoVol = Math.min(askVol, bidVol);

        }
        else if (retailState.getBook().equals(BEEF)) {
            List<RetailState.Level> bids = retailState.getBids();

            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestBeefBid = firstBid.getPrice();
                bidVol = Math.min(firstBid.getVolume(), volumeLimit);
            }
            else {
                highestBeefBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestBeefAsk = asks.get(0).getPrice();
                askVol = Math.min(firstAsk.getVolume(), volumeLimit);
            }
            else {
                lowestBeefAsk = 0.0;
            }

            beefVol = Math.min(askVol, bidVol);

        }
        else {
            List<RetailState.Level> bids = retailState.getBids();

            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestTortBid = firstBid.getPrice();
                bidVol = Math.min(firstBid.getVolume(), volumeLimit);
            }
            else {
                highestTortBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestTortAsk = asks.get(0).getPrice();
                askVol = Math.min(firstAsk.getVolume(), volumeLimit);
            }
            else {
                lowestTortAsk = 0.0;
            }

            tortVol = Math.min(askVol, bidVol);

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
