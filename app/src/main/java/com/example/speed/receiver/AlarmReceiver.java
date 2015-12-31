package com.example.speed.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.speed.service.StepService;

/**
 * Created by 豆浆 on 2015-12-29.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, StepService.class);
        context.startService(i);
    }
}
