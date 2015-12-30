package com.example.speed.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.*;
import android.hardware.SensorEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.example.speed.receiver.AlarmReceiver;

import java.net.BindException;

public class Caculator extends Service{
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
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
        int wait = 8 * 60 * 60 * 1000;
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

}
