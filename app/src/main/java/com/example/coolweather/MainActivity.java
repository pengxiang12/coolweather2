package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         *  如果用户已经点击了查看了天气的，就不需要在重新在选择城市和区了，直接查看
         */

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (defaultSharedPreferences.getString("weather", null) != null) {
            // 直接开启天气活动
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            // 关闭当前活动
            finish();
        }
    }
}