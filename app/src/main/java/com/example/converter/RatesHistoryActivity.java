package com.example.converter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RatesHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnBack;
    private ExchangeRateRepository ratesRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates_history);

        Log.d("RatesHistory", "Activity created");

        initViews();
        setupClickListeners();

        ratesRepository = new ExchangeRateRepository(getApplication());
        loadRatesHistory();
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        btnBack = findViewById(R.id.btnBack);

        if (listView == null) {
            Log.e("RatesHistory", "listView is null!");
        }
        if (btnBack == null) {
            Log.e("RatesHistory", "btnBack is null!");
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d("RatesHistory", "Back button clicked");
                finish();
            });
        }
    }

    private void loadRatesHistory() {
        Log.d("RatesHistory", "Loading rates history...");

        new Thread(() -> {
            try {
                List<ExchangeRateEntity> history = ratesRepository.getRatesHistory();

                runOnUiThread(() -> {
                    if (history != null && !history.isEmpty()) {
                        Log.d("RatesHistory", "Found " + history.size() + " records");

                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                        String[] items = new String[history.size()];

                        for (int i = 0; i < history.size(); i++) {
                            ExchangeRateEntity rate = history.get(i);
                            String date = dateFormat.format(rate.timestamp);
                            items[i] = String.format("%s\nEUR: %.4f | USD: %.4f | CNY: %.4f | BYN: %.4f",
                                    date, rate.eurRate, rate.usdRate, rate.cnyRate, rate.bynRate);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_list_item_1, items);
                        listView.setAdapter(adapter);

                        Toast.makeText(this, "Загружено записей: " + history.size(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("RatesHistory", "History is empty");
                        Toast.makeText(this, "История курсов пуста", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("RatesHistory", "Error loading history: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ratesRepository != null) {
            ratesRepository.shutdown();
        }
    }
}