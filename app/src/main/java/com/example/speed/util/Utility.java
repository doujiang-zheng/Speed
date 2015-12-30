package com.example.speed.util;

import android.text.TextUtils;

import com.example.speed.db.SpeedDB;
import com.example.speed.model.City;
import com.example.speed.model.County;
import com.example.speed.model.Province;

/**
 * Created by 豆浆 on 2015-12-30.
 */
public class Utility {

    /*
    *  解析和处理服务器返回的省级数据
    * */
    public synchronized static boolean handlePrvinceResponse(SpeedDB speedDB, String response) {
        if (TextUtils.isEmpty(response)) {
            String[] allProvince = response.split(",");
            if (allProvince != null && allProvince.length > 0) {
                for(String p : allProvince) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);

                    speedDB.saveProvince(province);
                }

                return true;
            }
        }

        return false;
    }

    /*
    *  解析和处理服务器返回的市级数据
    * */
    public static boolean handleCityResponse(SpeedDB speedDB, String response, int provinceId) {
        if(!TextUtils.isEmpty(response)) {
            String[] allCity = response.split(",");
            if (allCity != null && allCity.length > 0) {
                for (String c : allCity) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);

                    speedDB.saveCity(city);
                }

                return true;
            }
        }

        return false;
    }

    /**
     *  解析和处理服务器返回的县级数据
     */
    public static boolean handleCountyResponse(SpeedDB speedDB, String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCounty = response.split(",");
            if (allCounty != null && allCounty.length > 0) {
                for (String c : allCounty) {
                    String[] array = c.split("\\|");
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);

                    speedDB.saveCounty(county);
                }

                return true;
            }
        }

        return false;
    }
}
