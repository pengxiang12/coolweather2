package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.WeatherActivity;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public static final String TAG = "AutoUpdateService";
    //
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updatePic();
        // 定时来跟新天气情况
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // 定义更新的时间,8小时更新一次
        int anHour = 8 * 60 * 60 * 1000;
        // 获取哦那个当前系统开机的时间,加上当前时间表示让定时任务的触发时间从系统开机开始算起，但会唤醒 CPU
        long  triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0 ,i, 0);
        manager.cancel(pi);
        // 设置定时任务
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updatePic() {
        HttpUtil.sendOkHttpRequest(WeatherActivity.PIC_URL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: reason:" + e.getMessage());
                Toast.makeText(AutoUpdateService.this,"获取图片地址失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String url = response.body().string();
                // 将其保存到缓存中
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                edit.putString("bing_pic", url);
                edit.apply();
            }
        });

    }

    private void updateWeather() {
        // 从缓存中获取数据
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        if (weather != null) {
            // 有缓存时直接解析天气数据
            Weather weather1 = Utility.handleWeatherResponse(weather);
            String weatherId = weather1.basic.weatherId;
            String weatherUrl = WeatherActivity.URL + "?cityid=" + weatherId + "&key=" + WeatherActivity.KEY;
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // 如果失败就给出提示
                    Toast.makeText(AutoUpdateService.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // 从网路中获取天气
                    String responseContent = response.body().string();
                    // 进行JSON解析,拿到返回的数据
                    Weather weather = Utility.handleWeatherResponse(responseContent);
                    // 现在需要修改UI页面上的布局信息
                    // 处理数据
                    if (weather != null && "ok" .equals(weather.status)) {
                        // 获取缓存管理器编辑器
                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        // 将其添加到缓存中
                        edit.putString("weather", responseContent);
                        // 将修改的数据提交到内存，而后异步真正的提交到硬件磁盘，编辑完需要提交
                        edit.apply();
                    }
                }
            });
        }
    }
}