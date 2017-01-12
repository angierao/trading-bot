package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.*;
import com.imc.intern.exchange.datamodel.api.Error;
import com.imc.intern.exchange.datamodel.jms.ExposureUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.List;
import java.lang.Math;

public class TacoTrader implements OrderBookHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TacoTrader.class);
    private final Symbol TACO = Symbol.of("TACO");
    private final Symbol BEEF = Symbol.of("BEEF");
    private final Symbol TORT = Symbol.of("TORT");

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

    private int positionThreshold;

    private PositionTracker tracker;

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

        tracker = new PositionTracker();
    }

    @Override
    public void handleRetailState(RetailState retailState) {
        updateStateForRetailState(retailState);
        arbitrage();
    }

    @Override
    public void handleOwnTrade(OwnTrade trade) {
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

        if (highestTacoBid != 0 && lowestBeefAsk != 0 && lowestTortAsk != 0) {
            LOGGER.info("sell taco buy beef buy tort: ${}", highestTacoBid - lowestBeefAsk - lowestTortAsk);
            if (highestTacoBid > (lowestBeefAsk + lowestTortAsk) && maxVol > 0) {
                sendOrder(TACO, highestTacoBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                sendOrder(BEEF, lowestBeefAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                sendOrder(TORT, lowestTortAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
            }
        }
        else if (lowestTacoAsk != 0 && highestBeefBid != 0 && highestTortBid != 0) {
            LOGGER.info("buy taco sell beef sell tort: ${}", -lowestTacoAsk + highestBeefBid + highestTortBid);
            if (lowestTacoAsk < (highestBeefBid + highestTortBid) && maxVol > 0) {
                sendOrder(TACO, lowestTacoAsk, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.BUY);
                sendOrder(BEEF, highestBeefBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
                sendOrder(TORT, highestTortBid, maxVol, OrderType.IMMEDIATE_OR_CANCEL, Side.SELL);
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


    private void updateStateForRetailState(RetailState retailState) {

        int bidVol = 0;
        int askVol = 0;

        if (retailState.getBook().equals(TACO)) {
            List<RetailState.Level> bids = retailState.getBids();

            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestTacoBid = firstBid.getPrice();
                bidVol = firstBid.getVolume();
            }
            else {
                highestTacoBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestTacoAsk = asks.get(0).getPrice();
                askVol = firstAsk.getVolume();
            }
            else {
                lowestTacoAsk = 0.0;
            }

            if (askVol != 0 && bidVol != 0) {
                tacoVol = Math.min(askVol, bidVol);
            }
        }
        else if (retailState.getBook().equals(BEEF)) {
            List<RetailState.Level> bids = retailState.getBids();

            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestBeefBid = firstBid.getPrice();
                bidVol = firstBid.getVolume();
            }
            else {
                highestBeefBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestBeefAsk = asks.get(0).getPrice();
                askVol = firstAsk.getVolume();
            }
            else {
                lowestBeefAsk = 0.0;
            }

            if (askVol != 0 && bidVol != 0) {
                beefVol = Math.min(askVol, bidVol);
            }
        }
        else {
            List<RetailState.Level> bids = retailState.getBids();

            if (bids.size() > 0) {
                RetailState.Level firstBid = bids.get(0);
                highestTortBid = firstBid.getPrice();
                bidVol = firstBid.getVolume();
            }
            else {
                highestTortBid = 0.0;
            }

            List<RetailState.Level> asks = retailState.getAsks();
            if (asks.size() > 0) {
                RetailState.Level firstAsk = asks.get(0);
                lowestTortAsk = asks.get(0).getPrice();
                askVol = firstAsk.getVolume();
            }
            else {
                lowestTortAsk = 0.0;
            }

            if (askVol != 0 && bidVol != 0) {
                tortVol = Math.min(askVol, bidVol);
            }
        }

    }

    public void parseBeefRetailState(RetailState retailState) {
        List<RetailState.Level> bids = retailState.getBids();
        int bidVol = 0;
        int askVol = 0;

        if (bids.size() > 0) {
            RetailState.Level firstBid = bids.get(0);
            highestBeefBid = firstBid.getPrice();
            bidVol = firstBid.getVolume();
        }

        List<RetailState.Level> asks = retailState.getAsks();
        if (asks.size() > 0) {
            RetailState.Level firstAsk = asks.get(0);
            lowestBeefAsk = asks.get(0).getPrice();
            askVol = firstAsk.getVolume();
        }

        if (askVol != 0 && bidVol != 0) {
            beefVol = Math.min(askVol, bidVol);
        }
    }

    public void parseTortRetailState(RetailState retailState) {
        List<RetailState.Level> bids = retailState.getBids();
        int bidVol = 0;
        int askVol = 0;

        if (bids.size() > 0) {
            RetailState.Level firstBid = bids.get(0);
            highestTortBid = firstBid.getPrice();
            bidVol = firstBid.getVolume();
        }

        List<RetailState.Level> asks = retailState.getAsks();
        if (asks.size() > 0) {
            RetailState.Level firstAsk = asks.get(0);
            lowestTortAsk = asks.get(0).getPrice();
            askVol = firstAsk.getVolume();
        }

        if (askVol != 0 && bidVol != 0) {
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
