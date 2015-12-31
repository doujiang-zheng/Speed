package com.example.speed.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.speed.activity.StepMain;
import com.example.speed.db.SpeedDB;
import com.example.speed.db.SpeedHelper;
import com.example.speed.model.MinuteStep;
import com.example.speed.receiver.AlarmReceiver;
import com.example.speed.util.HttpCallbackListener;
import com.example.speed.util.HttpUtil;
import com.example.speed.util.Utility;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class StepService extends Service{
    public static boolean serviceFlag = false;
    public static boolean stableWalkStatusFlag = false;
    public static int  SELECTED_STEP = 0;

    private long startMinute;
    private static final int MIN_STEP_IN_10SECONDS = 10;
    private static final int MAX_STEP_IN_10SECONDS = 50;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private ShakeListener mShakeListener;

    private Thread selectStepThread;
    private Thread updateStepThread;

    private SpeedDB speedDB;

    private StepBinder stepBinder = new StepBinder();

    public class StepBinder extends Binder {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return stepBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceFlag = true;

        speedDB = SpeedDB.getInstance(StepService.this);
        startMinute = getCurrentMinute();

        startShakeListener();
        updateWeather();
        return super.onStartCommand(intent, flags, startId);

    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode = prefs.getString("weather_code", "");
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Utility.handleWeatherResponse(StepService.this, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceFlag = false;

        if (mShakeListener != null) {
            mSensorManager.unregisterListener(mShakeListener);
            mShakeListener = null;
        }
    }

    private void startShakeListener() {
        if (mShakeListener == null) {
            mShakeListener = new ShakeListener(StepService.this);
        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mShakeListener, mSensor, SensorManager.SENSOR_DELAY_FASTEST);

        initSelectStepThread();
        selectStepThread.start();

        initUpdateThread();
        updateStepThread.start();
    }

    private void initSelectStepThread() {
        selectStepThread = new Thread() {
            private int step_before;
            private int step_after;
            private int step_diff;
            private String date_before = new Date(System.currentTimeMillis()).toString();

            @Override
            public void run() {
                    step_before = ShakeListener.CURRENT_STEP;
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    step_after = ShakeListener.CURRENT_STEP;
                    step_diff = step_after - step_before;
                    if (step_diff >MIN_STEP_IN_10SECONDS && step_diff < MAX_STEP_IN_10SECONDS) {
                        stableWalkStatusFlag = true;
                    } else {
                        stableWalkStatusFlag = false;
                        ShakeListener.CURRENT_STEP = ShakeListener.CURRENT_STEP - step_diff;
                    }
            }
        };
    }

    public void initUpdateThread() {
        updateStepThread = new Thread() {
          @Override
            public void run() {
              while (serviceFlag) {
                  if (stableWalkStatusFlag && !insideMinute()) {
                      if (saveMinuteStep()) {
                          startMinute = getCurrentMinute();
                          ShakeListener.CURRENT_STEP = 0;
                          SELECTED_STEP = 0;
                      }
                  }
              }
          }
        };
    }

    public boolean saveMinuteStep() {
        SELECTED_STEP = ShakeListener.CURRENT_STEP;

        MinuteStep minuteStep = new MinuteStep();
        minuteStep.setMinute(startMinute);
        minuteStep.setStep(SELECTED_STEP);

        if (SELECTED_STEP > 0)
            return speedDB.saveMinuteStep(minuteStep);
        return true;
    }

    public long getCurrentMinute() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(System.currentTimeMillis()));
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        return startTime;
    }

    public boolean insideMinute() {
        long currentMinute = getCurrentMinute();

        if (currentMinute > startMinute) {
            return true;
        }

        return false;
    }
}
