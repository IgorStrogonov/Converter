package com.example.converter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.analytics.FirebaseAnalytics;

public class DonateActivity extends AppCompatActivity {

    // TODO: заменить на свою реальную ссылку для донатов перед публикацией/релизом.
    private static final String DONATE_URL_BASE = "https://example.com/donate";

    private Button btnDonate100, btnDonate500, btnDonate1000, btnCustomDonate;
    private Button btnRateApp, btnShareApp, btnBack;
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        initViews();
        setupClickListeners();

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private void initViews() {
        btnDonate100 = findViewById(R.id.btnDonate100);
        btnDonate500 = findViewById(R.id.btnDonate500);
        btnDonate1000 = findViewById(R.id.btnDonate1000);
        btnCustomDonate = findViewById(R.id.btnCustomDonate);
        btnRateApp = findViewById(R.id.btnRateApp);
        btnShareApp = findViewById(R.id.btnShareApp);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupClickListeners() {
        btnDonate100.setOnClickListener(v -> processDonation(100));
        btnDonate500.setOnClickListener(v -> processDonation(500));
        btnDonate1000.setOnClickListener(v -> processDonation(1000));
        btnCustomDonate.setOnClickListener(v -> showCustomDonationDialog());
        btnRateApp.setOnClickListener(v -> rateApp());
        btnShareApp.setOnClickListener(v -> shareApp());
        btnBack.setOnClickListener(v -> finish());
    }

    private void processDonation(int amount) {
        // Здесь можно интегрировать платежную систему
        // Покажем просто сообщение и откроем ссылку для доната
        Toast.makeText(this, "Спасибо за донат " + amount + " рублей!", Toast.LENGTH_LONG).show();

        // Открываем ссылку для доната
        String donateUrl = DONATE_URL_BASE + "?amount=" + amount;
        openUrlInBrowser(donateUrl);

        // Логируем донат в Analytics
        logDonationEvent(amount);
    }

    private void showCustomDonationDialog() {
        // Можно реализовать диалог для ввода произвольной суммы
        Toast.makeText(this, "Введите сумму доната в приложении банка", Toast.LENGTH_LONG).show();

        openUrlInBrowser(DONATE_URL_BASE);

        logDonationEvent(0); // 0 для кастомной суммы
    }

    private void rateApp() {
        // Открываем страницу приложения в Play Market (замените на ваш package name)
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.example.converter"));
            startActivity(intent);
        } catch (Exception e) {
            // Если Play Market не установлен, открываем в браузере
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.example.converter"));
            startActivity(intent);
        }

        Toast.makeText(this, "Спасибо за вашу оценку!", Toast.LENGTH_SHORT).show();
        logEvent("app_rated", "Пользователь оценил приложение");
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Конвертер валют");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Попробуйте это удобное приложение для конвертации валют! " +
                        "Скачать: https://play.google.com/store/apps/details?id=com.example.converter");

        startActivity(Intent.createChooser(shareIntent, "Поделиться приложением"));

        logEvent("app_shared", "Пользователь поделился приложением");
    }

    private void openUrlInBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void logDonationEvent(int amount) {
        Bundle bundle = new Bundle();
        bundle.putInt("donation_amount", amount);
        bundle.putString("donation_currency", "RUB");
        firebaseAnalytics.logEvent("donation_made", bundle);
    }

    private void logEvent(String eventName, String eventDescription) {
        Bundle bundle = new Bundle();
        bundle.putString("event_description", eventDescription);
        firebaseAnalytics.logEvent(eventName, bundle);
    }
}