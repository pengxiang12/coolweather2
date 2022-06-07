package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 *  天气返回的数据一个实体映射类
 */
public class Basic {

    // 名称和要返回的数据参数名不一致，使用这个注解即可
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public String location;

    @SerializedName("parent_city")
    public String parentCity;

    @SerializedName("admin_area")
    public String adminArea;

    /*public String city;*/

    public Update update;

    public class Update {

        @SerializedName("loc")
        public String updateTime;


    }



}
