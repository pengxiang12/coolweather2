package com.example.coolweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
// 网络工具类
public class HttpUtil {

    // 第一个参数：是地址访问网络的地址，第二个参数：回调函数，需要自己去写
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        // 创建请求网络客户端
        OkHttpClient client = new OkHttpClient();
        // 构建请求对象
        Request request = new Request.Builder().url(address).build();
        // 请求发送，然后将回调函数放入到等待队列中，如果有数据返回，则回来调用这个函数
        client.newCall(request).enqueue(callback);


    }
}
