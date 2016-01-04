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
    private double[] acc_smooth = new double[3005];
    private boolean[] accHigh = new boolean[3005];

    /*
    * 下标均以mod 3000 * 10 计算，使数组下标不会越界
    * */
    private int mode = 3005;
    private int current_end_index = -1;

    private long startMinute;

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

        for (int i = 0; i < mode; i++) {
            acc[i] = -1;
        }
        
        startMinute = getCurrentMinute();
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
            if (current_end_index < mode) {
                acc[current_end_index] = Math.sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]);
            }

            /*
            * 与ShakeService保持一致，进入下一分钟的时刻对前一分钟数据进行处理
            * */
            if(getCurrentMinute() > startMinute) {
                
                for(int i = 0; i < current_end_index && acc[i] >= 0; i++) {
                    /*
                    * 5点均值平滑去噪
                    * */
                    acc_smooth[i] = smooth(i);
                }

                for (int i = 0; i < current_end_index; i++) {
                    accHigh[i] = check(i);
                }

                /*
                * 取最大最小值，防止k-means产生不好的结果
                * */
                double max = acc_smooth[0], min = acc_smooth[0];
                for(int i = 1; i < current_end_index; i++) {
                    if(acc_smooth[i] > max)
                        max = acc_smooth[i];
                    if(acc_smooth[i] < min)
                        min = acc_smooth[i];
                }

                /*
                * 得到聚类的平均值
                * */
                double high_average = 0, low_avergae = 0;
                int high_size = 0, low_size = 0;
                for(int i = 0; i < current_end_index; i++) {
                    if (Math.abs(max - acc_smooth[i]) < Math.abs(acc_smooth[i] - min)) {
                        high_average = high_average + acc_smooth[i];
                        high_size++;
                    } else {
                        low_avergae = low_avergae + acc_smooth[i];
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
                double min_creast = -0.2 * (high_average - low_avergae) / 2 + high_average;
                double max_creast =  0.2 * (high_average - low_avergae) / 2 + high_average;
                for (int i = 0; i < current_end_index; i++) {
                    if(accHigh[i] && acc_smooth[i] > min_creast && acc_smooth[i] < max_creast) {
                        CURRENT_STEP++;
                    }
                }
                
                /*
                * 计步结束，初始化加速度为-1，进行下一时间窗口统计
                * */
                startMinute = getCurrentMinute();
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
    
    public double smooth(int index) {
        int min = (index - 2 > 0) ? index - 2 : 0;
        int max = (index + 2 < current_end_index) ? index + 3 : current_end_index;
        double result = 0;
        
        for(int i = min; i < max; i++) {
            result += acc[i];
        }
        
        return result / (max - min);
    }

    /*
    * 判断是否波峰
    * */
    public boolean check(int index) {
        int min = (index - 12 > 0) ? index - 12 : 0;
        int max = (index + 12 < current_end_index) ? index + 13 : current_end_index;

        for(int i = min; i < max; i++) {
            if (acc_smooth[i] > acc_smooth[index])
                return false;
        }

        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
