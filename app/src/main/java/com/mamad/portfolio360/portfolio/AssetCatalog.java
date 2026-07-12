package com.mamad.portfolio360.portfolio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * فهرست اولیه دارایی‌های محبوب برای انتخاب کاربر، دسته‌بندی‌شده.
 * نمادها مطابق قرارداد نام‌گذاری Yahoo Finance هستند (برای اتصال داده تاریخی در جلسه بعد).
 */
public class AssetCatalog {

    public static class Asset {
        public final String symbol;
        public final String name;

        public Asset(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }
    }

    public static final String CAT_STOCKS = "stocks";
    public static final String CAT_FUNDS = "funds";
    public static final String CAT_CRYPTO = "crypto";
    public static final String CAT_COMMODITIES = "commodities";

    public static Map<String, List<Asset>> all() {
        Map<String, List<Asset>> map = new LinkedHashMap<>();

        List<Asset> stocks = new ArrayList<>();
        stocks.add(new Asset("AAPL", "Apple"));
        stocks.add(new Asset("MSFT", "Microsoft"));
        stocks.add(new Asset("GOOGL", "Alphabet"));
        stocks.add(new Asset("AMZN", "Amazon"));
        stocks.add(new Asset("NVDA", "NVIDIA"));
        stocks.add(new Asset("TSLA", "Tesla"));
        stocks.add(new Asset("META", "Meta Platforms"));
        stocks.add(new Asset("JPM", "JPMorgan Chase"));
        stocks.add(new Asset("V", "Visa"));
        stocks.add(new Asset("JNJ", "Johnson & Johnson"));
        map.put(CAT_STOCKS, stocks);

        List<Asset> funds = new ArrayList<>();
        funds.add(new Asset("SPY", "S&P 500 ETF"));
        funds.add(new Asset("QQQ", "Nasdaq 100 ETF"));
        funds.add(new Asset("VTI", "Total US Market ETF"));
        funds.add(new Asset("VOO", "Vanguard S&P 500"));
        funds.add(new Asset("IWM", "Russell 2000 ETF"));
        funds.add(new Asset("DIA", "Dow Jones ETF"));
        funds.add(new Asset("ARKK", "ARK Innovation ETF"));
        funds.add(new Asset("TLT", "20+ Year Treasury ETF"));
        map.put(CAT_FUNDS, funds);

        List<Asset> crypto = new ArrayList<>();
        crypto.add(new Asset("BTC-USD", "Bitcoin"));
        crypto.add(new Asset("ETH-USD", "Ethereum"));
        crypto.add(new Asset("SOL-USD", "Solana"));
        crypto.add(new Asset("BNB-USD", "BNB"));
        crypto.add(new Asset("XRP-USD", "XRP"));
        crypto.add(new Asset("ADA-USD", "Cardano"));
        crypto.add(new Asset("DOGE-USD", "Dogecoin"));
        map.put(CAT_CRYPTO, crypto);

        List<Asset> commodities = new ArrayList<>();
        commodities.add(new Asset("GC=F", "طلا (Gold Futures)"));
        commodities.add(new Asset("SI=F", "نقره (Silver Futures)"));
        commodities.add(new Asset("CL=F", "نفت خام (Crude Oil)"));
        commodities.add(new Asset("NG=F", "گاز طبیعی (Natural Gas)"));
        commodities.add(new Asset("HG=F", "مس (Copper)"));
        commodities.add(new Asset("ZC=F", "ذرت (Corn)"));
        map.put(CAT_COMMODITIES, commodities);

        return map;
    }
          }
