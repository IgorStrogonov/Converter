package com.example.converter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnConverter, btnDonate, btnRatesHistory;
    private TextView tvUserInfo;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();

        mAuth = FirebaseAuth.getInstance();
        checkAuthStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAuthStatus();
    }

    private void initViews() {
        btnLogin = findViewById(R.id.btnLogin);
        btnConverter = findViewById(R.id.btnConverter);
        btnDonate = findViewById(R.id.btnDonate);
        btnRatesHistory = findViewById(R.id.btnRatesHistory);
        tvUserInfo = findViewById(R.id.tvUserInfo);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> openLoginActivity());
        btnConverter.setOnClickListener(v -> openConverterActivity());
        btnDonate.setOnClickListener(v -> openDonateActivity());
        btnRatesHistory.setOnClickListener(v -> openRatesHistoryActivity());
    }

    private void checkAuthStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Пользователь авторизован
            tvUserInfo.setText("Вы вошли как: " + currentUser.getEmail());
            tvUserInfo.setVisibility(View.VISIBLE);
            btnLogin.setText("Выйти");
        } else {
            // Пользователь не авторизован
            tvUserInfo.setVisibility(View.GONE);
            btnLogin.setText("Войти в аккаунт");
        }
    }

    private void openLoginActivity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Если пользователь авторизован - выход
            mAuth.signOut();
            checkAuthStatus();
            Toast.makeText(MainActivity.this, "Выход выполнен", Toast.LENGTH_SHORT).show();
        } else {
            // Если не авторизован - открываем экран логина
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void openConverterActivity() {
        startActivity(new Intent(this, ConverterActivity.class));
    }

    private void openDonateActivity() {
        startActivity(new Intent(this, DonateActivity.class));
    }

    private void openRatesHistoryActivity() {
        startActivity(new Intent(this, RatesHistoryActivity.class));
    }
}
