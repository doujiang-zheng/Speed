
package com.example.speed.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.example.speed.R;
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
import    java.text.SimpleDateFormat;

public class StepService extends Service{
    public static boolean serviceFlag = false;

    private long startMinute;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private ShakeListener mShakeListener;

    private Thread updateStepThread;

    private SpeedDB speedDB;

    private StepBinder stepBinder = new StepBinder();

    public class StepBinder extends Binder {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return stepBinder;
    }

    public void sendNotification() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd    hh:mm:ss");
        String date = sDateFormat.format(new Date());
        String message = sDateFormat.format(new Date()) + "  步数：" + Integer.toString(ShakeListener.CURRENT_STEP);
        Bitmap btm = BitmapFactory.decodeResource(getResources(), R.mipmap.run);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(StepService.this)
                .setSmallIcon(R.mipmap.run)
                .setContentTitle("Speed").setContentText(message);
        //第一次提示消息的时候显示在通知栏上
        mBuilder.setTicker("New message");
        mBuilder.setLargeIcon(btm);
        //自己维护通知的消失
        mBuilder.setAutoCancel(true);
        //构建一个Intent
        Intent resultIntent = new Intent(StepService.this, StepMain.class);
        //封装一个Intent
        PendingIntent resultPendingIntent = PendingIntent.getActivity(StepService.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 设置通知主题的意图
        mBuilder.setContentIntent(resultPendingIntent);
        //获取通知管理器对象
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
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
            String address = "http://www.weather.com.cn/data/cityinfo/101190101.html";
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
        mSensorManager.registerListener(mShakeListener, mSensor, SensorManager.SENSOR_DELAY_GAME);

        initUpdateThread();
        updateStepThread.start();
    }

    public void initUpdateThread() {
        updateStepThread = new Thread() {
          @Override
            public void run() {
              while (serviceFlag) {
                  long currentMinute = getCurrentMinute();
                  
                  /*
                  * 进入下一分钟的时刻，重新计步并存储上一分钟的步数
                  */
                  if (currentMinute > startMinute) {
                      try {
                         saveMinuteStep();
                      } finally {
                          startMinute = getCurrentMinute();
                          ShakeListener.CURRENT_STEP = 0;
                      }

                  }
              }
          }
        };
    }

    public boolean saveMinuteStep() {

        MinuteStep minuteStep = new MinuteStep();
        minuteStep.setMinute(startMinute);
        minuteStep.setStep(ShakeListener.CURRENT_STEP);

        if (ShakeListener.CURRENT_STEP > 0 && ShakeListenner.CURRENT_STEP < 200)
        {
            sendNotification();
            return speedDB.saveMinuteStep(minuteStep);
        }
        
        return true;
    }

    public long getCurrentMinute() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(System.currentTimeMillis()));
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long currentMinute = cal.getTimeInMillis();

        return currentMinute;
    }

}
