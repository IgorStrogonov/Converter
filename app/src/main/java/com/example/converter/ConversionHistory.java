package com.example.converter;

import java.util.Date;

public class ConversionHistory {
    private String id;
    private double amount;
    private String fromCurrency;
    private String toCurrency;
    private double rate;
    private double result;
    private long timestamp; // Измените Date на long для Firebase

    // Обязательный пустой конструктор для Firebase
    public ConversionHistory() {}

    public ConversionHistory(double amount, String fromCurrency, String toCurrency,
                             double rate, double result) {
        this.amount = amount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.result = result;
        this.timestamp = new Date().getTime(); // Сохраняем как timestamp
    }

    // Getters and Setters (должны быть публичными для Firebase)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public double getResult() { return result; }
    public void setResult(double result) { this.result = result; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Вспомогательный метод для получения даты
    public Date getDate() {
        return new Date(timestamp);
    }
}