package com.example.converter;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeRateRepository {
    private ExchangeRateDao exchangeRateDao;
    private ExecutorService executorService;

    public ExchangeRateRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        exchangeRateDao = database.exchangeRateDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    // Сохранить курсы в базу данных
    public void saveRates(ExchangeRateResponse response) {
        executorService.execute(() -> {
            Rates rates = response.getRates();
            ExchangeRateEntity entity = new ExchangeRateEntity(
                    response.getBase(),
                    rates.getEUR(),
                    rates.getUSD(),
                    rates.getCNY(),
                    rates.getBYN(),
                    response.getDate()
            );
            exchangeRateDao.insertRates(entity);
            exchangeRateDao.deleteOldRates(); // Очищаем старые записи
        });
    }

    // Получить последние курсы из базы данных
    public ExchangeRateEntity getLatestRates() {
        try {
            return executorService.submit(() -> exchangeRateDao.getLatestRates()).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Получить историю курсов
    public List<ExchangeRateEntity> getRatesHistory() {
        try {
            return executorService.submit(() -> exchangeRateDao.getRatesHistory()).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Проверить, есть ли свежие курсы (менее 1 часа назад)
    public boolean hasFreshRates() {
        ExchangeRateEntity latestRates = getLatestRates();
        if (latestRates == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000; // 1 час в миллисекундах

        return (currentTime - latestRates.timestamp) < oneHour;
    }

    // Останавливает фоновый executor. Обязательно вызывать в onDestroy() у Activity,
    // которая создала этот repository, иначе поток остаётся висеть после закрытия экрана.
    public void shutdown() {
        executorService.shutdown();
    }
}
