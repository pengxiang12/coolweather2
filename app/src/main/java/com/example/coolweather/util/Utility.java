package com.example.coolweather.util;

import android.content.pm.FeatureGroupInfo;
import android.text.TextUtils;

import com.example.coolweather.db.City;
import com.example.coolweather.db.Country;
import com.example.coolweather.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// 网络返回的json数据，处理数据工具类
public class Utility {
    /**
     *  解析和处理服务器返回的省级数据
     */
    // 参数：表示返回的数据
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    // 将返回的ID,作为库中的代号
                    province.setProvinceCode(provinceObject.getInt("id"));
                    // 保存到数据库中
                    province.save();
                }
                return true;
            } catch (JSONException e) {
              e.printStackTrace();
            }
        }
        return false;
    }

    /**
     *  解析和处理服务器返回的市级数据
     *  返回结果使用boolean 表示处理是否成功
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            // 将response转换为jsonArray
            try {
                JSONArray allCity = new JSONArray(response);
                for (int i = 0; i < allCity.length(); i++) {
                    JSONObject jsonObject = allCity.getJSONObject(i);
                    City city = new City();
                    city.setCityCode(jsonObject.getInt("id"));
                    city.setCityName(jsonObject.getString("name"));
                    // 保存省
                    city.setProvinceId(provinceId);
                    // 保存
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     * 返回结果使用boolean 表示处理是否成功， 同时将其放入到数据库中
     */

    public static boolean handleCountryResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCountry = new JSONArray(response);
                for(int i = 0; i < allCountry.length(); i++) {
                    JSONObject jsonObject = allCountry.getJSONObject(i);
                    Country country = new Country();
                    country.setCountName(jsonObject.getString("name"));
                    country.setCityId(cityId);
                    country.setWeatherId(jsonObject.getString("weather_id"));
                    country.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
