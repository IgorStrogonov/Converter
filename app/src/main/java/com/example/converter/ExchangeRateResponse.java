package com.example.converter;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class ExchangeRateResponse {
    private String base;
    private String date;
    private Rates rates;
    private static final String TAG = "ExchangeRateResponse";

    public ExchangeRateResponse(String base, String date, Rates rates) {
        this.base = base;
        this.date = date;
        this.rates = rates;
    }

    public static ExchangeRateResponse fromJson(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String baseCurrency = jsonObject.getString("base_code");
        String fullDateString = jsonObject.getString("time_last_update_utc");

        String[] parts = fullDateString.split(" ");
        String date = parseDate(fullDateString);

        JSONObject ratesObject = jsonObject.getJSONObject("rates");

        Rates rates = new Rates(
                ratesObject.getDouble("EUR"),
                ratesObject.getDouble("USD"),
                ratesObject.getDouble("CNY"),
                ratesObject.getDouble("BYN")
        );

        return new ExchangeRateResponse(
                baseCurrency,
                date,
                rates
        );
    }

    private static String parseDate(String dateString) {
        try {
            // Разбиваем строку на части: "Mon, 08 Jan 2024 00:00:00 +0000"
            dateString = dateString.replace(",", "");
            String[] parts = dateString.split("\\s+");

            if (parts.length >= 4) {
                // parts[0] = "Mon," (день недели)
                // parts[1] = "08" (день)
                // parts[2] = "Jan" (месяц)
                // parts[3] = "2024" (год)

                String day = parts[1];
                String month = convertMonth(parts[2]);
                String year = parts[3];

                if (day.length() == 1) {
                    day = "0" + day;
                }

                // Форматируем в "2024-01-08"
                return year + "-" + month + "-" + day;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Возвращаем дату по умолчанию в случае ошибки
        return "2024-01-01";
    }

    private static String convertMonth(String month) {
        switch (month.toLowerCase()) {
            case "jan":
                return "01";
            case "feb":
                return "02";
            case "mar":
                return "03";
            case "apr":
                return "04";
            case "may":
                return "05";
            case "jun":
                return "06";
            case "jul":
                return "07";
            case "aug":
                return "08";
            case "sep":
                return "09";
            case "oct":
                return "10";
            case "nov":
                return "11";
            case "dec":
                return "12";
            default:
                return "01";
        }
    }

    public String getBase() {
        return base;
    }

    public String getDate() {
        return date;
    }

    public Rates getRates() {
        return rates;
    }
}