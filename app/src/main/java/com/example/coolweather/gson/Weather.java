package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    // 状态
    public String status;

    // 区的基础信息
    public Basic basic;

    //
    public AQI aqi;

    // 当前天气情况
    public Now now;

    // 天气给出建议
    @SerializedName("suggestion")
    public Suggestion mSuggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> mForecasts;
}
