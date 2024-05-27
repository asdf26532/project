package com.smartfarm.iot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView temperatureText;
    private TextView humidityText;
    private ImageView temperatureIcon;
    private ImageView humidityIcon;
    private WebView cctvWebView;
    private ImageButton refreshButton;
    private ProgressBar loadingIndicator; // 로딩 인디케이터
    private static final int DELAY_MILLISECONDS = 1000; // 딜레이 시간 (1초)
    private static final String WEATHER_API_KEY = "3fc6a07ac53d22cd1f01a7495f563451";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureText = findViewById(R.id.temperature_text);
        humidityText = findViewById(R.id.humidity_text);
        temperatureIcon = findViewById(R.id.temperature_icon);
        humidityIcon = findViewById(R.id.humidity_icon);
        cctvWebView = findViewById(R.id.cctv_webview);
        refreshButton = findViewById(R.id.refresh_button);
        loadingIndicator = findViewById(R.id.loading_indicator); // 로딩 인디케이터 초기화

        // Setup WebView for CCTV
        WebSettings webSettings = cctvWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        cctvWebView.setWebViewClient(new WebViewClient());
        cctvWebView.loadUrl("http://192.168.4.12");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocationAndWeather();
        }
        refreshButton.setOnClickListener(v -> {
            refreshButton.setVisibility(View.GONE); // 새로고침 버튼 숨김
            loadingIndicator.setVisibility(View.VISIBLE); // 로딩 인디케이터 표시
            new Handler().postDelayed(this::getLocationAndWeather, DELAY_MILLISECONDS);
        });
    }

    private void getLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            fetchWeather(latitude, longitude);
                        }
                    }
                });
    }

    private void fetchWeather(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getCurrentWeather(latitude, longitude,  WEATHER_API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    updateUI(weatherResponse);
                    refreshButton.setVisibility(View.VISIBLE); // 새로고침 버튼 숨김
                    loadingIndicator.setVisibility(View.GONE); // 로딩 인디케이터 표시

                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // Handle failure
            }
        });
    }

    private void updateUI(WeatherResponse weatherResponse) {
        // 온도를 섭씨로 변환
        int temperatureCelsius = Math.round(weatherResponse.main.temp - 273.15f);
        int humidity = weatherResponse.main.humidity;

        // UI 업데이트
        temperatureText.setText("온도 " + temperatureCelsius + "°C");
        humidityText.setText("습도 " + humidity + "%");

        temperatureIcon.setImageResource(R.drawable.temperature_icon);
        humidityIcon.setImageResource(R.drawable.humidity_icon);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndWeather();
            }
        }
    }

    interface WeatherService {
        @GET("data/2.5/weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("lat") double lat,
                @Query("lon") double lon,
                @Query("appid") String appid
        );
    }

    class WeatherResponse {
        Main main;

        class Main {
            float temp;
            int humidity;
        }
    }
}