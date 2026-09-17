package com.example.converter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "exchange_rates")
public class ExchangeRateEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String baseCurrency;
    public double eurRate;
    public double usdRate;
    public double cnyRate;
    public double bynRate;
    public String date;
    public long timestamp;

    public ExchangeRateEntity(String baseCurrency, double eurRate, double usdRate,
                              double cnyRate, double bynRate, String date) {
        this.baseCurrency = baseCurrency;
        this.eurRate = eurRate;
        this.usdRate = usdRate;
        this.cnyRate = cnyRate;
        this.bynRate = bynRate;
        this.date = date;
        this.timestamp = new Date().getTime();
    }
}