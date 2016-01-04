package com.example.speed.activity;

import android.app.ProgressDialog;
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

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.speed.R;
import com.example.speed.db.SpeedDB;
import com.example.speed.model.City;
import com.example.speed.model.County;
import com.example.speed.model.MinuteStep;
import com.example.speed.model.Province;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class StepDetail extends AppCompatActivity {
    private boolean isFromMainActivity;

    private CalendarView calendarView;
    private Date selectedDate = new Date();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TextView titleText;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private SpeedDB speedDB;
    private List<String> dataList = new ArrayList<String>();

    private List<MinuteStep> minuteStepList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromMainActivity = getIntent().getBooleanExtra("from_main_activity", false);
        setContentView(R.layout.activity_step_detail);

        calendarView = (CalendarView) findViewById(R.id.calendar);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate = new Date(new GregorianCalendar(year, month, dayOfMonth).getTimeInMillis());
            }
        });

        titleText = (TextView) findViewById(R.id.title_text);
        listView = (ListView) findViewById(R.id.minute_list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        speedDB = SpeedDB.getInstance(this);
        titleText.setText(dateFormat.format(selectedDate));

        queryMinuteStep();
        titleText.setText(dateFormat.format(selectedDate));
    }

    public void queryMinuteStep() {
        minuteStepList = speedDB.loadMinuteStep(selectedDate);
        if (minuteStepList.size() > 0) {
            dataList.clear();
            for (MinuteStep minuteStep : minuteStepList) {
                String date = simpleDateFormat.format(minuteStep.getMinute());
                String step = Integer.toString(minuteStep.getStep());

                String content  = date + "  " + step;
                dataList.add(content);
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(dateFormat.format(selectedDate));
        }
    }

    public void onBackPressed() {
        if (isFromMainActivity) {
            Intent intent = new Intent(this, StepMain.class);
            startActivity(intent);
            finish();
        }
    }
}
