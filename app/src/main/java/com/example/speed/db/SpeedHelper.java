package com.example.speed.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 豆浆 on 2015-12-28.
 */
/*
* User:
* create table User(
*   id integer primary key autoincrement,
*   user text
* )
* MinuteStep:
* create table if not exists MinuteStep(
*   id integer foreign key reference User(id),
*   minute integer,
*   step integer,
*   primary key(id, minute)
* )
* */
public class SpeedHelper extends SQLiteOpenHelper{
    /*
    * 目前只建一张表 MinuteStep
    * */
    public static final String CREATE_MINUTESTEP = "create table if not exists MinuteStep("
            + "minute integer primary key,"
            + "step integer)";
    public static final String CREATE_PROVINCE = "create table if not exists Province("
            + "id integer primary key autoincrement,"
            + "province_name text,"
            + "province_code text)";

    public static final String CREATE_CITY = "create table if not exists City("
            + "id integer primary key autoincrement,"
            + "city_name text,"
            + "city_code text,"
            + "province_id integer)";

    public static final String CREATE_COUNTY = "create table if not exists County("
            + "id integer primary key autoincrement,"
            + "county_name text,"
            + "county_code text,"
            + "city_id integer)";

    public SpeedHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MINUTESTEP);
        db.execSQL(CREATE_PROVINCE);
        db.execSQL(CREATE_CITY);
        db.execSQL(CREATE_COUNTY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
