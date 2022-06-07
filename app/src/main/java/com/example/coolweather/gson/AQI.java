package com.example.coolweather.gson;

public class AQI {

    public AQICity city;

    public class AQICity {

        // aqi值 比如54
        public String aqi;

        // pm值
        public String pm25;

        // 天气状况，优，良，差等等
        public String qlty;
    }
}
