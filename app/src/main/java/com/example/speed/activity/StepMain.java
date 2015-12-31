package com.example.speed.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.example.speed.R;
import com.example.speed.service.StepService;

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
                VISIBILITY = (VISIBILITY + 1) % 2;
                setVISIBILITY();
                break;
            case R.id.calendar_button:
                intent = new Intent(StepMain.this, StepDetail.class);
                startActivity(intent);
                break;
            case R.id.weather_button:
                intent = new Intent(StepMain.this, WeatherActivity.class);
                startActivity(intent);
                break;
            case R.id.friends_button:
                break;
            default:
                break;
        }
    }

    private void setVISIBILITY() {
        if (VISIBILITY == 0) {
            calendar.setVisibility(View.INVISIBLE);
            weather.setVisibility(View.INVISIBLE);
            friends.setVisibility(View.INVISIBLE);
        } else {
            calendar.setVisibility(View.VISIBLE);
            weather.setVisibility(View.VISIBLE);
            friends.setVisibility(View.VISIBLE);
        }
    }
}
