package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    // 标记
    public static final String TAG = "WeatherActivity";

    // 定义一个访问和风天气所必须携带的个人key
    public static final String KEY = "7ae3b712e9134be29d5acc7a4ebd697b";

    // 访问和风天气的地址
    public static final String URL = "http://guolin.tech/api/weather";

    // 这个是获取必应上的图片URL地址返回的结果，存储在现在的服务器上的地址
    public static final String PIC_URL = "http://guolin.tech/api/bing_pic";

    // 滚动布局
    private ScrollView weatherLayout;

    // 标题显示城市名称
    private TextView mTitleCity;

    // 标题显示更新时间
    private TextView mTitleUpdateTime;

    // 当前天气的温度
    private TextView mDegreeText;

    // 当前天气的信息状态
    private TextView mWeatherInfoText;

    // 未来天气，这里是线性布局，因为里面的视图是动态进行加载的
    private LinearLayout forecastLayout;

    // 空气质量之aqi信息
    private TextView aqiText;

    // 空气质量之pm信息
    private TextView pm25Text;

    // 空气质量之qlty信息
    private TextView qltyText;

    // 舒适度建议
    private TextView comfortText;

    // 洗车建议
    private TextView carWashText;

    // 运动建议
    private TextView sportText;

    // 背景图片
    private ImageView mImageView;

    // 刷新页面
    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 完成背景图片和状态栏融合在一起
        if (Build.VERSION.SDK_INT >= 21) {
            // 获取DecorView对象(Decor 装饰)
            View decorView = getWindow().getDecorView();
            // setSystemUiVisibility（）:方法来改变系统UI的显示 View.SYSTEM_UI_FLAG_FULLSCREEN和View.SYSTEM_UI_FLAG_LAYOUT_STABLE这两个值
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // 将状态栏设置成透明色（transparent）
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        weatherLayout = findViewById(R.id.weather_layout);
        mTitleCity = findViewById(R.id.title_city);
        mTitleUpdateTime = findViewById(R.id.title_update_time);
        mDegreeText = findViewById(R.id.degree_text);
        mWeatherInfoText = findViewById(R.id.degree_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        qltyText = findViewById(R.id.qlty_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        mImageView = findViewById(R.id.bing_pic_img);
        mRefreshLayout = findViewById(R.id.swipe_refresh);
        // 设置下拉刷新进度条的颜色，这里我们就使用主题中的colorPrimary作为进度条的颜色了。
        mRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        // 从缓存中获取数据
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        // 这里定义一个区的id
        final String weatherId;
        if (weather != null) {
            // 有缓存时直接解析天气数据
            Weather weather1 = Utility.handleWeatherResponse(weather);
            weatherId = weather1.basic.weatherId;
            showWeatherInfo(weather1);
        } else {
            // 缓存没有区服务器查询天气
             weatherId = getIntent().getStringExtra("weather_id");
            // 如果缓存没有，需要将其ScrollView进行隐藏掉，不然空数据的界面看上去会很奇怪
            weatherLayout.setVisibility(View.INVISIBLE);
            // 从网络请求数据，并显示出来
            requestWeather(weatherId);
        }

        // 从缓存中
        String bingPic = preferences.getString("bing_pic", null);
        if (bingPic != null) {
            // 加载图片到imageview中，从网路中获取的是地址，放入到ImageView中
            Glide.with(this).load(bingPic).into(mImageView);
        } else {
            loadBingPic();
        }

        // 刷新,就是给这个组件上添加刷新的按钮，给刷新事件添加一个监听器
        mRefreshLayout.setOnRefreshListener(()-> requestWeather(weatherId));
    }

    private void loadBingPic() {
        HttpUtil.sendOkHttpRequest(PIC_URL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: reason:" + e.getMessage());
                Toast.makeText(WeatherActivity.this,"获取图片地址失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String url = response.body().string();
                // 将其保存到缓存中
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic", url);
                edit.apply();
                // 更改页面上UI内容
                runOnUiThread(()->{
                    // 进行修改
                    // 加载图片到imageview中，从网路中获取的是地址
                    Glide.with(WeatherActivity.this).load(url).into(mImageView);
                });
            }
        });
    }

    private void requestWeather(String weatherId) {
        // 拼凑天气访问的地址
        String weatherUrl = URL + "?cityid=" + weatherId + "&key=" + KEY;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 如果失败就给出提示
                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                // 作用就是刷新完毕之后，需要关闭刷新进度条，就是隐藏掉
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 从网路中获取天气
              String responseContent = response.body().string();
              // 进行JSON解析,拿到返回的数据
                Weather weather = Utility.handleWeatherResponse(responseContent);
                // 现在需要修改UI页面上的布局信息
                runOnUiThread(() -> {
                    // 处理数据
                    if (weather != null && "ok".equals(weather.status)) {
                        // 获取缓存管理器编辑器
                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                        // 将其添加到缓存中
                        edit.putString("weather", responseContent);
                        // 将修改的数据提交到内存，而后异步真正的提交到硬件磁盘，编辑完需要提交
                        edit.apply();
                        // 将数据给展示出来
                        showWeatherInfo(weather);
                    } else {
                        // 给出提示 表示错误
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                    // 作用就是刷新完毕之后，需要关闭刷新进度条，就是隐藏掉
                    mRefreshLayout.setRefreshing(false);
                });
            }
        });
        // 这里的目的是希望每次更新天气的时候，能够背景图片也刷新一下
        loadBingPic();
    }

    private void showWeatherInfo(Weather weather) {
        // 拿到基础数据,城市和更新时间
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime;
        // 将其修改添加进去
        mTitleCity.setText(cityName);
        mTitleUpdateTime.setText(updateTime);


        // 当前天气设置 温度和天气状况
        String temperature = weather.now.temperature + "℃";
        String info = weather.now.mMore.info;
        // 添加到ui中
        mWeatherInfoText.setText(info);
        mDegreeText.setText(temperature);

        // 未来天气情况
        // 移除所有视图，主要是为了更新其他城市或者过了今天，当前是明天时间，那么未来天气就要跟着变，需要提前将其清空下面的视图
        forecastLayout.removeAllViews();
        // 变量添加到容器中
        for (Forecast forecast : weather.mForecasts) {
            // 第一个参数是：需要根据那个布局来构建视图的，第二个参数是这个布局所在的父视图，第三个参数
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            // 日期
            TextView data = view.findViewById(R.id.data_text);
            // 天气状况
            TextView futureInfo = view.findViewById(R.id.info_text);
            // 最高气温
            TextView maxTemperature = view.findViewById(R.id.max_text);
            // 最低气温
            TextView minTemperature = view.findViewById(R.id.min_text);
            // 进行添加到ui中
            data.setText(forecast.date);
            futureInfo.setText(forecast.mMore.info);
            maxTemperature.setText(forecast.mTemperature.max + "℃");
            minTemperature.setText(forecast.mTemperature.min + "℃");
            // 设置成功，然后视图添加到另一个布局下
            forecastLayout.addView(view);
        }

        // 空气质量设置
        String aqi = weather.aqi.city.aqi;
        String pm25 = weather.aqi.city.pm25;
        String qlty = weather.aqi.city.qlty;
        aqiText.setText(aqi);
        pm25Text.setText(pm25);
        qltyText.setText(qlty);

        // 建议设置
        // 洗车指数
        String car = "洗车建议：" + weather.mSuggestion.cartWash.info;
        // 舒适度指数
        String comfort = "舒适度建议："  + weather.mSuggestion.mComfort.info;
        // 运动指数
        String sport = "运动建议：" + weather.mSuggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(car);
        sportText.setText(sport);
       // 设置可见
        weatherLayout.setVisibility(View.VISIBLE);
    }


}