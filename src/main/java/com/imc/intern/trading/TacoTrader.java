package com.imc.intern.trading;

import com.imc.intern.exchange.client.RemoteExchangeView;
import com.imc.intern.exchange.datamodel.Side;
import com.imc.intern.exchange.datamodel.api.OrderType;
import com.imc.intern.exchange.datamodel.api.RetailState;
import com.imc.intern.exchange.datamodel.api.Symbol;

import java.util.List;
import java.lang.Math;

/**
 * Created by imc on 11/01/2017.
 */
public class TacoTrader {

    private final Symbol TACO = Symbol.of("ARA.TACO");

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
    }

    public void arbitrage() {
        int maxVol = Math.min(tacoVol, Math.min(beefVol, tortVol));

        if (highestTacoBid > (lowestBeefAsk + lowestTortAsk)) {
            //buy beef and tort
            //sell taco
            sendOrder(TACO, highestTacoBid, maxVol, OrderType.GOOD_TIL_CANCEL, Side.SELL);
        }
        else if (lowestTacoAsk < (highestBeefBid + highestTortBid)) {
            sendOrder(TACO, lowestTacoAsk, maxVol, OrderType.GOOD_TIL_CANCEL, Side.BUY);
        }
    }

    public void parseTacoRetailState(RetailState retailState) {
        List<RetailState.Level> bids = retailState.getBids();
        int bidVol = 0;
        int askVol = 0;

        if (bids.size() > 0) {
            RetailState.Level firstBid = bids.get(0);
            highestTacoBid = firstBid.getPrice();
            bidVol = firstBid.getVolume();
        }

        List<RetailState.Level> asks = retailState.getAsks();
        if (asks.size() > 0) {
            RetailState.Level firstAsk = asks.get(0);
            lowestTacoAsk = asks.get(0).getPrice();
            askVol = firstAsk.getVolume();
        }

        if (askVol != 0 && bidVol != 0) {
            tacoVol = Math.min(askVol, bidVol);
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
