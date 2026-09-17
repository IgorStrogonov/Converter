package com.example.converter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface ExchangeRateDao {

    // Сохранить курсы валют
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRates(ExchangeRateEntity rates);

    // Получить последние курсы
    @Query("SELECT * FROM exchange_rates ORDER BY timestamp DESC LIMIT 1")
    ExchangeRateEntity getLatestRates();

    // Получить историю курсов (последние 30 записей)
    @Query("SELECT * FROM exchange_rates ORDER BY timestamp DESC LIMIT 30")
    List<ExchangeRateEntity> getRatesHistory();

    // Получить курсы за определенную дату
    @Query("SELECT * FROM exchange_rates WHERE date = :date LIMIT 1")
    ExchangeRateEntity getRatesByDate(String date);

    // Удалить старые записи (оставить последние 50)
    @Query("DELETE FROM exchange_rates WHERE id NOT IN (SELECT id FROM exchange_rates ORDER BY timestamp DESC LIMIT 50)")
    void deleteOldRates();
}