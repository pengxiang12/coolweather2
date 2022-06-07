package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coolweather.db.City;
import com.example.coolweather.db.Country;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    // 进度对话框
    private ProgressDialog mProgressDialog;

    // 文本组件
    private TextView titleText;

    // 后退按钮
    private Button backButton;

    // 列表组件
    private ListView mListView;

    // 数组适配器。这里和列表组件一起使用
    private ArrayAdapter<String> mAdapter;

    // 存放数据的 用于将给列表组件展示使用的数据
    private List<String> dataList = new ArrayList<>();

    // 省集合，用于存储省相关的信息数据，访问网络的时候，用于保存的数据，最后存储到数据库中
    private List<Province> mProvinceList;

    // 市列表， 用于存储市相关的信息数据，也是网络访问返回的数据，用于保存的数据
    private List<City> mCityList;

    // 县列表
    private List<Country> mCountryList;

    // 选中的省份，这里记录选中的省信息，后面查询省下的市用的
    private Province selectedProvince;

    // 选中的城市。这里记录选中的市信息，后面查询县相关的信息
    private City selectedCity;

    // 当前选中的级别，根据选择的级别，我可以知道下一步要做啥，比如点击省份的时候，我就要将市相关的信息都展示出来，如果点击市 我就将县信息都查询出来
    private int currentLevel;

    // 这个方法就是将碎片布局转换为View，因为不是活动，是碎片视图，所以这里需要使用inflater.inflate来创建视图，将布局中各个组件转换为对象
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 创建碎片的视图
        View view = inflater.inflate(R.layout.choose_area, container, false);
        // 获取各个组件信息
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        mListView = view.findViewById(R.id.list_view);
        // 将数据添加到适配器中，完成数据在listView组件中显示出来
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        // 适配和ListView进行绑定
        mListView.setAdapter(mAdapter);
        return view;
    }

    // 活动创建完毕的时候调用，就是给item添加事件，给button按钮添加 事件
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 这个在活动创建完毕之后，给list组件每个item添加点击事件
        mListView.setOnItemClickListener((adapterView, view, i, l) -> {
            // 在点击当个item的时候，判断它的当前等级，如果是省份
            if (currentLevel == LEVEL_PROVINCE) {
                // 将这个省信息，填写到到这个省份的变量当中，后面会用到
                selectedProvince = mProvinceList.get(i);
                // 查询城市
                queryCities();
            } else if(currentLevel == LEVEL_CITY) {
                // 将市信息复制给这里
                selectedCity = mCityList.get(i);
                // 查询县信息
                queryCounties();
            } else if (currentLevel == LEVEL_COUNTY) {
                // 从县的列表点击i位置的区信息
                String weatherId = mCountryList.get(i).getWeatherId();
                // 判断当前是不是在主活动中，如果是按照启动新的天气活动
                if (getActivity() instanceof MainActivity) {
                    // 如果当前是县就要将天气给显示出来

                    // 开启天气活动
                    Intent intent = new Intent(getContext(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    // 将当前活动给关闭了，因为开启了新的活动
                    getActivity().finish();
                } else if (getActivity() instanceof WeatherActivity) {
                    // 如果当前是在天气活动中，那么就不需要重新开启天气活动
                    WeatherActivity weatherActivity = (WeatherActivity)getActivity();
                    // 这里是来关闭滑动菜单，因为不需要切换到另一个省市中
                    weatherActivity.mDrawerLayout.closeDrawers();
                    // 显示下拉框进度条
                    weatherActivity.mRefreshLayout.setRefreshing(true);
                    // 重新获取区的天气
                    weatherActivity.requestWeather(weatherId);
                }
            }
        });

        // 给后退按钮添加点击事件
        backButton.setOnClickListener(v-> {
            // 如果当前是县等级，那么点击后退查询城市信息
            if (currentLevel == LEVEL_COUNTY) {
                queryCities();
            }
            // 如果当前是市等级，后退 查询城市信息
             else if (currentLevel == LEVEL_CITY) {
                queryProvinces();
            }
        });
        // 将省信息给显出出来
        queryProvinces();
    }

    // 查询全国所有的省，先从数据库中查询，如果没有在从服务器上查询，在将数据放入到数据库中，并且显示出来
    private void queryProvinces() {
        titleText.setText("中国");
        // 设置按钮是不可见的，且不暂地方没，因为查询省没有后退必要了，后面没了
        backButton.setVisibility(View.GONE);
        // 先从数据库中查询省份信息
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.size() > 0) {
            // 先将临时数据集合数据清除
            dataList.clear();
            for (Province province : mProvinceList) {
                // 将其省的名字添加进去
                dataList.add(province.getProvinceName());
            }
            // 适配器将最新的数据添加进去
            mAdapter.notifyDataSetChanged();
            // 这个作用将list组件放入到最新的末尾位置
            mListView.setSelection(0);
            // 同时将其当前位置设置为自己的省份等级
            currentLevel = LEVEL_PROVINCE;
        } else {
            // 从网上查询
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    //这个方法就是从网络上进行查询，然后保存到数据库中，同时修改页面上信息
    private void queryFromServer(String address, final String type) {
      // 查询网络时，需要给出进度对话框，因为网络暂用时间
        showProgressDialog();
        // 使用工具类来去调用
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            // 返回的失败之后需要处理的信息
            @Override
            public void onFailure(Call call, IOException e) {
               // 获取活动，通过runOnUiThread 方法回到主线程处理逻辑
                getActivity().runOnUiThread(() -> {
                    // 关闭进度对话框
                    closeProgressDialog();
                    Log.d(TAG, "onFailure: reason:" + e.getMessage());
                    // 错误处理，给出提示从网络查询失败
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                });
            }

            // 返回成功的信息
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                // 定义一个变量，标记从网络上查询后的数据保存数据库是否执行成功了,默认是失败的
                boolean result = false;
                if ("province".equals(type)) {
                    // 工具类中就是将数据拿到，保存到数据库中
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)){
                    // 从选中的省中选择
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if("county".equals(type)) {
                    result = Utility.handleCountryResponse(responseText, selectedCity.getId());
                }

                // 如果执行成功，之后需要进行一个操作，就是页面做显示操作
                if (result) {
                    getActivity().runOnUiThread(() -> {
                        // 关闭进度对话框
                        closeProgressDialog();
                        if ("province".equals(type)) {
                            // 需要再次调用 查询省信息操作，将其显示在页面上，上面必须在保存成功才能执行这个操作，否则会导致无限死循环，
                            // 因为没有保存到数据库 ，查询下面的方法，又会执行去查询网络，还会走到这里
                            queryProvinces();
                        } else if ("city".equals(type)) {
                            queryCities();
                        }else if ("county".equals(type)) {
                            queryCounties();
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            // 进行创建对象
            mProgressDialog = new ProgressDialog(getActivity());
            // 设置信息
            mProgressDialog.setMessage("正在加载....");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        // 显示出来
        mProgressDialog.show();
    }

    private void queryCities() {
        // 标题是选中省的名称
        titleText.setText(selectedProvince.getProvinceName());
        // 设置为可见
        backButton.setVisibility(View.VISIBLE);
        // 从数据库中查询
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.size() > 0) {
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties() {
        // 标题是选中市
        titleText.setText(selectedCity.getCityName());
        // 设置为可见
        backButton.setVisibility(View.VISIBLE);
        // 从数据库中查询
        mCountryList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(Country.class);
        if (mCountryList.size() > 0) {
            dataList.clear();
            for (Country country : mCountryList) {
                dataList.add(country.getCountName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
