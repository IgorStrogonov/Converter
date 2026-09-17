package com.example.converter;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeRateService {

    public interface ExchangeRateCallback {
        void onSuccess(ExchangeRateResponse response);
        void onError(String errorMessage);
    }

    // AsyncTask deprecated с API 30 - используем свой ExecutorService для сетевого запроса
    // и Handler на главном Looper'е для колбэка, как и остальной фоновый код в проекте
    // (см. ExchangeRateRepository).
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static void getExchangeRates(ExchangeRateCallback callback) {
        EXECUTOR.execute(() -> {
            String result = null;
            String errorMessage = null;
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("https://open.er-api.com/v6/latest/RUB");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP error code: " + responseCode;
                } else {
                    InputStream inputStream = connection.getInputStream();
                    StringBuilder buffer = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line).append("\n");
                    }
                    result = buffer.toString();
                }
            } catch (IOException e) {
                errorMessage = e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            String finalResult = result;
            String finalErrorMessage = errorMessage;
            MAIN_HANDLER.post(() -> {
                if (finalResult != null) {
                    try {
                        ExchangeRateResponse response = ExchangeRateResponse.fromJson(finalResult);
                        callback.onSuccess(response);
                    } catch (JSONException e) {
                        callback.onError("Ошибка парсинга JSON: " + e.getMessage());
                    }
                } else {
                    callback.onError("Ошибка загрузки: " + finalErrorMessage);
                }
            });
        });
    }
}
