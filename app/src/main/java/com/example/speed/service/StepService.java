package com.example.speed.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

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
    private static final String TAG = "StepService";
    public static boolean serviceFlag = false;
    public static boolean stableWalkStatusFlag = false;
    public static int  SELECTED_STEP = 0;

    private int stepToday;
    private int stepMinute;
    private int stepHistory = -1;
    private static final int MIN_STEP_IN_10SECONDS = 10;
    private static final int MAX_STEP_IN_10SECONDS = 50;

    private SensorManager mSensorManager;
    private Sensor mSensor, mStepDetectorSensor, mStepCountSensor;
    private ShakeListener mShakeListener;

    private static final float SCALE_STEO_CALORIES = 43.22f;
    private Thread selectStepThread;
    private Thread updateStepThread;

    private SpeedDB speedDB;
    private SpeedHelper speedHelper;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceFlag = true;

        speedDB = SpeedDB.getInstance(StepService.this);
        List<MinuteStep> minuteSteps = speedDB.loadMinuteStep(new Date(System.currentTimeMillis()));
        int step = 0;
        if (minuteSteps.size() >  0) {
            for (int i = 0; i < minuteSteps.size(); i++) {
                step += minuteSteps.get(i).getStep();
            }
        } else {
            if (stepMinute != 0) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(new Date(System.currentTimeMillis()));
                cal.setTimeZone(TimeZone.getDefault());
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                long startTime = cal.getTimeInMillis();
                MinuteStep minuteStep = new MinuteStep();
                minuteStep.setMinute(startTime);
                minuteStep.setStep(stepMinute);
                speedDB.saveMinuteStep(minuteStep);
            }
            startShakeListener();
            return START_STICKY;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }
        ).start();
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int wait = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + wait;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

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
                String date_after = new Date(System.currentTimeMillis()).toString();
                if (date_after.equals(date_before)) {
                    step_after = ShakeListener.CURRENT_STEP;
                    step_diff = step_after - step_before;
                    if (step_diff >MIN_STEP_IN_10SECONDS) {
                        stableWalkStatusFlag = true;
                    } else {
                        stableWalkStatusFlag = false;
                        ShakeListener.CURRENT_STEP = ShakeListener.CURRENT_STEP - step_diff;
                    }
                    } else {
                    ShakeListener.CURRENT_STEP = 0;
                    date_before = date_after;
                }
            }
        };
    }

    public void initUpdateThread() {
        updateStepThread = new Thread() {
          @Override
            public void run() {
              while (serviceFlag) {
                  try {
                      sleep(1000);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
                  if (stableWalkStatusFlag) {
                      SELECTED_STEP = ShakeListener.CURRENT_STEP;

                      GregorianCalendar cal = new GregorianCalendar();
                      cal.setTime(new Date(System.currentTimeMillis()));
                      cal.setTimeZone(TimeZone.getDefault());
                      cal.set(Calendar.SECOND, 0);
                      cal.set(Calendar.MILLISECOND, 0);
                      long startTime = cal.getTimeInMillis();

                      MinuteStep minuteStep = new MinuteStep();
                      minuteStep.setMinute(startTime);
                      minuteStep.setStep(SELECTED_STEP);
                      if (!speedDB.saveMinuteStep(minuteStep)) {
                          ShakeListener.CURRENT_STEP = 0;
                          SELECTED_STEP = 0;
                      }
                  }
              }
          }
        };
    }
}
