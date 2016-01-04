package com.example.speed.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.example.speed.R;
import com.example.speed.db.SpeedDB;
import com.example.speed.model.MinuteStep;
import com.example.speed.service.StepService;

import java.util.Date;
import java.util.List;
import java.util.logging.LogRecord;

public class StepMain extends AppCompatActivity implements View.OnClickListener {
    private int VISIBILITY = 0;
    private Button step;
    private Button calendar;
    private Button weather;
    private Button friends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, StepService.class);
        startService(intent);
        setContentView(R.layout.activity_step_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        step = (Button) findViewById(R.id.step_button);
        updateStep();

        calendar = (Button) findViewById(R.id.calendar_button);
        weather = (Button) findViewById(R.id.weather_button);
        friends = (Button) findViewById(R.id.friends_button);

        step.setOnClickListener(this);
        calendar.setOnClickListener(this);
        weather.setOnClickListener(this);
        friends.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.step_button:
                updateStep();
                VISIBILITY = (VISIBILITY + 1) % 2;
                setVISIBILITY();
                break;
            case R.id.calendar_button:
                intent = new Intent(StepMain.this, StepDetail.class);
                intent.putExtra("from_main_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.weather_button:
                intent = new Intent(StepMain.this, WeatherActivity.class);
                intent.putExtra("from_main_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.friends_button:
                break;
            default:
                break;
        }
    }

    private void setVISIBILITY() {
        if (VISIBILITY == 0) {
            calendar.setVisibility(View.GONE);
            weather.setVisibility(View.GONE);
            friends.setVisibility(View.GONE);
        } else {
            calendar.setVisibility(View.VISIBLE);
            weather.setVisibility(View.VISIBLE);
            friends.setVisibility(View.VISIBLE);
        }
    }

    public void updateStep() {
        SpeedDB speedDB = SpeedDB.getInstance(StepMain.this);
        int date_step = speedDB.loadDateStep(new Date(System.currentTimeMillis()));

        step.setText(Integer.toString(date_step) + "步");
    }
}
