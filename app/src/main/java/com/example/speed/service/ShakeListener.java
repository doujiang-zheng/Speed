package com.example.speed.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by 豆浆 on 2015-12-30.
 */
public class ShakeListener implements SensorEventListener {
    public static int CURRENT_STEP = 0;
    public static float SENSITIVITY = 1.5f;

    private static final int FREQUENCY = 3000;
    private double[] acc = new double[3005];
    private boolean[] accHigh = new boolean[3005];
    /*
    * 下标均以mod 3000 * 10 计算，使数组下标不会越界
    * */
    private int mode = 3005;
    private int current_end_index = -1;
    private long nextMinute = getCurrentMinute() + 60 * 1000;

    private SensorManager sensorManager;
    private Sensor sensor;
    private OnShakeListener onShakeListener;

    private Context context;

    public ShakeListener(Context c) {
        super();
        context = c;
        start();
    }

    public void start() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensor != null) {
            try {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
            } finally {
                   stop();
            }
        }

        for (int i = 1; i < mode; i++) {
            acc[i] = -1;
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public interface OnShakeListener {
        public void onShake();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        onShakeListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] gravity = new float[3];
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];

            current_end_index++;
            current_end_index %= mode;
            acc[current_end_index] = Math.sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]);

            /*
            * 继续获取当前一分钟的加速度信息，否则进行步数计算并存储到数据库中
            * */
            if(!(nextMinute > System.currentTimeMillis())) {
                for(int i = 2; i < current_end_index - 2 && acc[i] >= 0; i++ ) {
                    /*
                    * 5点均值平滑去噪
                    * */
                    acc[i] = (acc[i - 2] + acc[i - 1] + acc[i] + acc[i + 1] + acc[i + 2]) / 5;
                }

                for (int i = 0; i < current_end_index; i++) {
                    accHigh[i] = check(i);
                }

                /*
                * 取最大最小值，防止k-means产生不好的结果
                * */
                double max = acc[0], min = acc[0];
                for(int i = 1; i < current_end_index; i++) {
                    if(acc[i] > max)
                        max = acc[i];
                    if(acc[i] < min)
                        min = acc[i];
                }

                /*
                * 得到聚类的平均值
                * */
                double high = max, low = min, high_average = 0, low_avergae = 0;
                int high_size = 0, low_size = 0;
                for(int i = 0; i < current_end_index; i++) {
                    if (Math.abs(max - acc[i]) < Math.abs(acc[i] - min)) {
                        high_average = high_average + acc[i];
                        high_size++;
                    } else {
                        low_avergae = low_avergae + acc[i];
                        low_size++;
                    }
                }
                if (high_size != 0)
                    high_average /= high_size;
                if (low_size != 0)
                    low_avergae /= low_size;

                /*
                * 去除正常行走所带来的伪波峰
                * */
                for (int i = 0; i < current_end_index; i++) {
                    double min_creast = -(high_average - low_avergae) /2 + high_average;
                    double max_creast = (high_average - low_avergae) / 2 + high_average;
                    if(accHigh[i] && acc[i] > min_creast && acc[i] < max_creast) {
                        CURRENT_STEP++;
                    }
                }
                /*
                * 计步结束，初始化加速度为-1，进行下一时间窗口统计
                * */
                nextMinute = getCurrentMinute() + 60 * 1000;
                acc[0] = acc[current_end_index];
                current_end_index = 0;
                for (int i = 1; i < mode; i++) {
                    acc[i] = -1;
                }
            }
        }
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

    /*
    * 判断是否波峰
    * */
    public boolean check(int index) {
        int min = (index - 12 > 0) ? index - 12 : 0;
        int max = (index + 12 < current_end_index) ? index + 12 : current_end_index;

        for(int i = min; i < max; i++) {
            if (acc[i] > acc[index])
                return false;
        }

        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static void reviseSensitivity(float sensitivity) {
        SENSITIVITY = sensitivity;
    }
}
