package com.example.speed.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.speed.model.City;
import com.example.speed.model.County;
import com.example.speed.model.MinuteStep;
import com.example.speed.model.Province;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by 豆浆 on 2015-12-29.
 */
public class SpeedDB {
    public static final String DB_NAME = "Speed";
    public static final int VERSION = 1;
    private static SpeedDB speedDB;
    private SQLiteDatabase db;

    /*
    * 构造方法私有化，实现单例
     */
    private SpeedDB(Context context){
        SpeedHelper dbHelper = new SpeedHelper(context, DB_NAME, null, VERSION);
        db = dbHelper.getWritableDatabase();
    }

    /*
    * 获取SpeedDB的实例
    * */
    public synchronized static SpeedDB getInstance(Context context){
        if(speedDB == null){
            speedDB = new SpeedDB(context);
        }

        return speedDB;
    }

    /*
    * 将产生步数的每分钟与对应步数存储到数据库
    * */
    public synchronized boolean saveMinuteStep(MinuteStep minuteStep){
        if(minuteStep != null){
            ContentValues values = new ContentValues();
            values.put("minute",minuteStep.getMinute());
            values.put("step", minuteStep.getStep());
            db.insert("MinuteStep", null, values);

            return true;
        }

        return false;
    }

    /*
    * 从数据库读取对应日期的步数
    * */
    public List<MinuteStep> loadMinuteStep(Date date) {
        List<MinuteStep> list = new ArrayList<MinuteStep>();
        /*
        * 构造long型的日期格式
        * */
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        //根据需要设置时区
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long startTime = cal.getTimeInMillis();
        long endTime   = startTime + 24 * 3600 * 1000;

        /*
        * 数据库查询
        * */
        Cursor cursor = db.rawQuery("select * from MinuteStep where"
                + "minute>=? and minute<?",
                new String[] {String.valueOf(startTime), String.valueOf(endTime)}
        );
        if (cursor.moveToFirst()) {
            do {
                MinuteStep minuteStep = new MinuteStep();
                minuteStep.setMinute(cursor.getLong(cursor.getColumnIndex("minute")));
                minuteStep.setStep(cursor.getInt(cursor.getColumnIndex("step")));
                list.add(minuteStep);
            }while(cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public void saveProvince(Province province){
        if(province != null){
            ContentValues values = new ContentValues();
            values.put("province_name", province.getProvinceName());
            values.put("province_code", province.getProvinceCode());
            db.insert("Province", null, values);
        }
    }

    public List<Province> loadProvinces() {
        List<Province> list = new ArrayList<Province>();
        Cursor cursor = db.query("Province", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Province province = new Province();
                province.setId(cursor.getInt(cursor.getColumnIndex("id")));
                province.setProvinceName(cursor.getString(cursor.getColumnIndex("province_name")));
                province.setProvinceCode(cursor.getString(cursor.getColumnIndex("province_code")));
                list.add(province);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public void saveCity(City city) {
        if (city != null) {
            ContentValues values = new ContentValues();
            values.put("city_name", city.getCityName());
            values.put("city_code", city.getCityCode());
            values.put("province_id", city.getProvinceId());
            db.insert("City", null, values);
        }
    }

    public List<City> loadCity(int provinceId) {
        List<City> list = new ArrayList<City>();
        Cursor cursor = db.query("City", null, "province_id=?",
                new String[] { String.valueOf(provinceId) }, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                City city = new City();
                city.setId(cursor.getInt(cursor.getColumnIndex("id")));
                city.setCityName(cursor.getString(cursor.getColumnIndex("city_name")));
                city.setCityCode(cursor.getString(cursor.getColumnIndex("city_code")));
                city.setProvinceId(provinceId);
                list.add(city);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public void saveCounty(County county) {
       if (county != null) {
           ContentValues values = new ContentValues();
           values.put("county_name", county.getCountyName());
           values.put("county_code", county.getCountyCode());
           values.put("city_id", county.getCityId());
           db.insert("County", null, values);
       }
    }

    public List<County> loadCounty (int cityId) {
        List<County> list = new ArrayList<County>();
        Cursor cursor = db.query("County", null, "city_id=?",
                new String[] { String.valueOf(cityId) }, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                County county = new County();
                county.setId(cursor.getInt(cursor.getColumnIndex("id")));
                county.setCountyName(cursor.getString(cursor.getColumnIndex("county_name")));
                county.setCountyCode(cursor.getString(cursor.getColumnIndex("county_code")));
                county.setCityId(cityId);
                list.add(county);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

}
