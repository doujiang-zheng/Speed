package com.example.speed.activity;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.example.speed.R;

public class StepDetail extends AppCompatActivity {
    private boolean isFromMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromMainActivity = getIntent().getBooleanExtra("from_main_activity", false);
        setContentView(R.layout.activity_step_detail);
    }

    public void onBackPressed() {
        if (isFromMainActivity) {
            Intent intent = new Intent(this, StepMain.class);
            startActivity(intent);
            finish();
        }
    }
}
