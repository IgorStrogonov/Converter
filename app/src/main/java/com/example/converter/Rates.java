package com.example.converter;

public class Rates {
    private double EUR;
    private double USD;
    private double CNY;
    private double BYN;

    public Rates(double EUR, double USD, double CNY, double BYN) {
        this.EUR = EUR;
        this.USD = USD;
        this.CNY = CNY;
        this.BYN = BYN;
    }

    public double getEUR() { return EUR; }
    public double getUSD() { return USD; }
    public double getCNY() { return CNY; }
    public double getBYN() { return BYN; }
}