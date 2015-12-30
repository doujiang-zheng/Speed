package com.example.speed.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.*;
import android.hardware.SensorEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.example.speed.receiver.AlarmReceiver;

import java.net.BindException;

public class Caculator extends Service implements  SensorEventListener{
    private double SPEED_SHRESHOLD = 10.0 ;
    private long UPTATE_INTERVAL_TIME = 1;
    private long lastUpdateTime;
    private float lastX, lastY, lastZ;

    public Caculator() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - lastUpdateTime;
        if (timeInterval < UPTATE_INTERVAL_TIME)
            return;
        lastUpdateTime = currentUpdateTime;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float deltaZ = z - lastZ;
        lastX = x;
        lastY = y;
        lastZ = z;
        double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ* deltaZ)/ timeInterval * 10000;
        Log.v("thelog", "===========log===================");
        //if (speed >= SPEED_SHRESHOLD)
//        {onShakeListener.onShake();}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v("thelog", "===========logCreate===================");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // calculate steps here
            }
        }
        ).start();
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int wait = 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + wait;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        Log.v("thelog", "===========logStart===================");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
