package com.example.converter;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ConverterActivity extends AppCompatActivity {

    // Статическая информация по каждой валюте (имя + символ), одно место вместо трёх дублирующихся switch.
    private static final class CurrencyInfo {
        final String name;
        final String symbol;

        CurrencyInfo(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }
    }

    private static final Map<String, CurrencyInfo> CURRENCIES = new LinkedHashMap<>();

    static {
        CURRENCIES.put("EUR", new CurrencyInfo("Евро (€)", "€"));
        CURRENCIES.put("USD", new CurrencyInfo("Доллар ($)", "$"));
        CURRENCIES.put("CNY", new CurrencyInfo("Юань (¥)", "¥"));
        CURRENCIES.put("BYN", new CurrencyInfo("Белорусский рубль (Br)", "Br"));
    }

    private EditText etAmount;
    private Button btnEUR, btnUSD, btnCNY, btnBYN, btnRefresh, btnBack;
    private Map<String, Button> currencyButtons;
    private TextView tvCurrencyResult, tvDetails, tvError, tvHistoryCounter;
    private ProgressBar progressBar;
    private ExchangeRateResponse exchangeRates;
    private String selectedCurrency = "";
    private FirestoreService firestoreService;
    private FirebaseAnalytics firebaseAnalytics;
    private int historyCount = 0;
    private ExchangeRateRepository ratesRepository;
    private boolean useCachedRates = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);

        initViews();
        setupClickListeners();

        // Инициализация Firebase. История конвертаций привязана к uid текущего пользователя -
        // для анонимных (кнопка "Пропустить" на экране логина) FirestoreService работает в
        // no-op режиме, см. FirestoreService.isEnabled().
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;
        firestoreService = new FirestoreService(uid);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Инициализация локальной БД
        ratesRepository = new ExchangeRateRepository(getApplication());

        // Добавляем слушатель реального времени для истории
        setupRealtimeHistoryListener();

        // Загружаем курсы (сначала проверяем локальную БД)
        loadExchangeRates();

        // Логируем событие открытия конвертера
        logConverterOpenEvent();
    }

    private void setupRealtimeHistoryListener() {
        if (!firestoreService.isEnabled()) {
            // Аноним - истории конвертаций в облаке для него нет, объясняем это в счётчике,
            // а не показываем "Всего конвертаций: 0", будто их правда ноль.
            if (tvHistoryCounter != null) {
                tvHistoryCounter.setText("История доступна после входа в аккаунт");
            }
            return;
        }
        firestoreService.addRealtimeHistoryListener(new FirestoreService.RealtimeUpdateCallback() {
            @Override
            public void onHistoryUpdated(List<ConversionHistory> history) {
                runOnUiThread(() -> {
                    historyCount = history.size();
                    updateHistoryCounter();

                    // Логируем обновление истории в Analytics
                    logHistoryUpdateEvent(historyCount);

                    // Можно добавить уведомление или другую логику при обновлении истории
                    if (historyCount > 0) {
                        Log.d("RealtimeDB", "История конвертаций обновлена: " + historyCount + " записей");
                    }
                });
            }
        });
    }

    private void updateHistoryCounter() {
        // Обновляем интерфейс с количеством записей в истории
        if (tvHistoryCounter != null) {
            tvHistoryCounter.setText("Всего конвертаций: " + historyCount);
        }
        // Или просто логируем
        Log.d("History", "Текущее количество записей в истории: " + historyCount);
    }

    private void logHistoryUpdateEvent(int historySize) {
        Bundle bundle = new Bundle();
        bundle.putInt("history_size", historySize);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "history_update");
        firebaseAnalytics.logEvent("history_updated", bundle);
    }

    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        btnEUR = findViewById(R.id.btnEUR);
        btnUSD = findViewById(R.id.btnUSD);
        btnCNY = findViewById(R.id.btnCNY);
        btnBYN = findViewById(R.id.btnBYN);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);
        tvCurrencyResult = findViewById(R.id.tvCurrencyResult);
        tvDetails = findViewById(R.id.tvDetails);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        tvHistoryCounter = findViewById(R.id.tvHistoryCounter);

        currencyButtons = new LinkedHashMap<>();
        currencyButtons.put("EUR", btnEUR);
        currencyButtons.put("USD", btnUSD);
        currencyButtons.put("CNY", btnCNY);
        currencyButtons.put("BYN", btnBYN);
    }

    private void loadExchangeRates() {
        // Проверка свежести курсов в БД делает блокирующий запрос,
        // поэтому уводим её с главного потока, а не вызываем hasFreshRates() напрямую.
        showLoading(true);
        new Thread(() -> {
            boolean fresh = ratesRepository.hasFreshRates();
            runOnUiThread(() -> {
                if (fresh) {
                    useCachedRates = true;
                    loadRatesFromDatabase();
                } else {
                    useCachedRates = false;
                    loadRatesFromApi();
                }
            });
        }).start();
    }

    private void loadRatesFromApi() {
        showLoading(true);
        showError(false, null);

        ExchangeRateService.getExchangeRates(new ExchangeRateService.ExchangeRateCallback() {
            @Override
            public void onSuccess(ExchangeRateResponse response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    exchangeRates = response;
                    showError(false, null);

                    // Сохраняем курсы в локальную БД
                    ratesRepository.saveRates(response);

                    // Логируем обновление курсов
                    logRatesUpdateEvent(response.getDate());

                    Toast.makeText(ConverterActivity.this,
                            "Курсы обновлены из сети",
                            Toast.LENGTH_SHORT).show();

                    if (!selectedCurrency.isEmpty()) {
                        String currencyName = getCurrencyName(selectedCurrency);
                        showCurrencyRate(selectedCurrency, currencyName);
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // Если API не доступен, пробуем загрузить из БД
                    if (!useCachedRates) {
                        loadRatesFromDatabase();
                    } else {
                        showError(true, errorMessage);
                    }
                });
            }
        });
    }

    private void loadRatesFromDatabase() {
        showLoading(true);

        new Thread(() -> {
            ExchangeRateEntity cachedRates = ratesRepository.getLatestRates();

            runOnUiThread(() -> {
                showLoading(false);

                if (cachedRates != null) {
                    // Конвертируем данные из БД в нашу модель
                    Rates rates = new Rates(
                            cachedRates.eurRate,
                            cachedRates.usdRate,
                            cachedRates.cnyRate,
                            cachedRates.bynRate
                    );

                    exchangeRates = new ExchangeRateResponse(
                            cachedRates.baseCurrency,
                            cachedRates.date,
                            rates
                    );

                    showError(false, null);

                    String source = useCachedRates ? "кеша" : "базы данных";
                    Toast.makeText(ConverterActivity.this,
                            "Курсы загружены из " + source + " (" + cachedRates.date + ")",
                            Toast.LENGTH_SHORT).show();

                    if (!selectedCurrency.isEmpty()) {
                        String currencyName = getCurrencyName(selectedCurrency);
                        showCurrencyRate(selectedCurrency, currencyName);
                    }
                } else {
                    showError(true, "Нет данных о курсах");
                }
            });
        }).start();
    }

    private void setupClickListeners() {
        for (Map.Entry<String, Button> entry : currencyButtons.entrySet()) {
            String code = entry.getKey();
            entry.getValue().setOnClickListener(v -> selectCurrency(code, getCurrencyName(code)));
        }
        btnRefresh.setOnClickListener(v -> {
            useCachedRates = false; // Принудительно обновляем из API
            loadExchangeRates();
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void selectCurrency(String currencyCode, String currencyName) {
        selectedCurrency = currencyCode;
        resetButtonColors();

        // Логируем выбор валюты
        logCurrencySelectionEvent(currencyCode, currencyName);

        int selectedColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
        Button button = currencyButtons.get(currencyCode);
        if (button != null) {
            button.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
        }

        showCurrencyRate(currencyCode, currencyName);
    }

    private void resetButtonColors() {
        int defaultColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
        ColorStateList defaultTint = ColorStateList.valueOf(defaultColor);
        for (Button button : currencyButtons.values()) {
            button.setBackgroundTintList(defaultTint);
        }
    }

    private String formatDateForDisplay(String dateString) {
        try {
            // Если дата в формате "2024-01-08"
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = dateString.split("-");
                String year = parts[0];
                String month = parts[1];
                String day = parts[2];

                // Форматируем в "08.01.2024"
                return day + "." + month + "." + year;
            }
        } catch (Exception e) {
            // Если ошибка, возвращаем оригинальную строку
        }
        return dateString;
    }

    private void showCurrencyRate(String currencyCode, String currencyName) {
        if (exchangeRates == null) {
            tvCurrencyResult.setText("Курсы не загружены");
            tvDetails.setText("Нажмите 'Обновить курсы' для загрузки актуальных данных");
            return;
        }

        Rates rates = exchangeRates.getRates();
        double rate = getRateValue(rates, currencyCode);
        CurrencyInfo info = CURRENCIES.get(currencyCode);
        String symbol = info != null ? info.symbol : "";

        String amountText = etAmount.getText().toString();
        double amount = 0.0;
        boolean hasAmount = false;

        if (!amountText.isEmpty()) {
            try {
                amount = Double.parseDouble(amountText);
                if (amount > 0) {
                    hasAmount = true;
                    double convertedAmount = amount * rate;

                    // Логируем конвертацию в Analytics
                    logConversionEvent(amount, "RUB", currencyCode, convertedAmount);

                    // Сохраняем в историю
                    saveToHistory(amount, "RUB", currencyCode, rate, convertedAmount);
                }
            } catch (NumberFormatException e) {
                // Игнорируем ошибку - показываем только курс
            }
        }

        if (hasAmount) {
            double convertedAmount = amount * rate;
            tvCurrencyResult.setText(String.format("%.2f RUB = %.2f %s", amount, convertedAmount, symbol));
        } else {
            tvCurrencyResult.setText(String.format("1 RUB = %.4f %s", rate, symbol));
        }

        String formattedDate = formatDateForDisplay(exchangeRates.getDate());

        String details = String.format(
                "Валюта: %s\nКод: %s\nКурс: 1 RUB = %.6f %s\nОбновлено: %s",
                currencyName, currencyCode, rate, symbol, formattedDate
        );
        tvDetails.setText(details);
    }

    private void saveToHistory(double amount, String fromCurrency, String toCurrency,
                               double rate, double result) {
        ConversionHistory history = new ConversionHistory(amount, fromCurrency, toCurrency, rate, result);

        firestoreService.saveConversion(history, new FirestoreService.SaveCallback() {
            @Override
            public void onSuccess() {
                // Успешно сохранено в историю
                Log.d("Firebase", "Конвертация сохранена в историю: " +
                        amount + " " + fromCurrency + " → " + result + " " + toCurrency);
            }

            @Override
            public void onError(String errorMessage) {
                // Просто логируем ошибку, не показываем пользователю
                Log.e("Firebase", "Ошибка сохранения истории: " + errorMessage);
            }
        });
    }

    // Методы для Firebase Analytics
    private void logCurrencySelectionEvent(String currencyCode, String currencyName) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, currencyCode);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, currencyName);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "currency");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void logConversionEvent(double amount, String fromCurrency, String toCurrency, double result) {
        Bundle bundle = new Bundle();
        bundle.putString("from_currency", fromCurrency);
        bundle.putString("to_currency", toCurrency);
        bundle.putDouble("amount", amount);
        bundle.putDouble("converted_amount", result);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "conversion");
        firebaseAnalytics.logEvent("conversion", bundle);
    }

    private void logRatesUpdateEvent(String date) {
        Bundle bundle = new Bundle();
        bundle.putString("update_date", date);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "rates_update");
        firebaseAnalytics.logEvent("rates_updated", bundle);
    }

    private String getCurrencyName(String currencyCode) {
        CurrencyInfo info = CURRENCIES.get(currencyCode);
        return info != null ? info.name : currencyCode;
    }

    // Единственное оставшееся место, где нужен switch: у Rates типизированные геттеры
    // (getEUR()/getUSD()/...), а не доступ по строковому коду.
    private double getRateValue(Rates rates, String currencyCode) {
        switch (currencyCode) {
            case "EUR": return rates.getEUR();
            case "USD": return rates.getUSD();
            case "CNY": return rates.getCNY();
            case "BYN": return rates.getBYN();
            default: return 0.0;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!show);
        btnEUR.setEnabled(!show);
        btnUSD.setEnabled(!show);
        btnCNY.setEnabled(!show);
        btnBYN.setEnabled(!show);
    }

    private void showError(boolean show, String message) {
        tvError.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null) {
            tvError.setText(message);
        }
    }

    private void logConverterOpenEvent() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "ConverterActivity");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ConverterActivity");
        firebaseAnalytics.logEvent("converter_opened", bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Отписываемся от real-time listener'а и останавливаем фоновый executor репозитория,
        // иначе они продолжают жить после закрытия экрана (утечка при повороте/возврате назад).
        if (firestoreService != null) {
            firestoreService.removeRealtimeHistoryListener();
        }
        if (ratesRepository != null) {
            ratesRepository.shutdown();
        }
    }
}