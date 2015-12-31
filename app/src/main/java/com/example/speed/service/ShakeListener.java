package com.example.speed.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by 豆浆 on 2015-12-30.
 */
public class ShakeListener implements SensorEventListener {
    public static int CURRENT_STEP = 0;
    public static float SENSITIVITY = 1.5f;

    private float lastValues[] = new float[3 * 2];
    private float scale[] = new float[2];

    private float offSet;
    private static long end  = 0;
    private static long start  = 0;

    private float lastDirections[] = new float[3 * 2];
    private float lastExtremes[][] = {new float[3 * 2], new float[3 * 2] };
    private float lastDiff[] = new float[3 * 2];
    private int lastMatch = -1;

    private SensorManager sensorManager;
    private Sensor sensor;
    private OnShakeListener onShakeListener;

    private Context context;

    private float lastX;
    private float lastY;
    private float lastZ;

    private long lastUpdateTime;

    public ShakeListener(Context c) {
        super();
        int h = 480;
        offSet = h * 0.5f;
        scale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        scale[1] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        context = c;
        start();
    }

    public void start() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
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
            float vSum = 0;
            for (int i = 0; i < 3; i++) {
                final float v = offSet + event.values[i] * scale[1];
                vSum += v;
            }
            int k = 0;
            float v = vSum / 3;
            float direction = (v > lastValues[k] ? 1 : (v < lastValues[k] ? -1 : 0));
            if (direction == -lastDirections[k]) {
                int extType = (direction > 0 ? 0 : 1);

                lastExtremes[extType][k] = lastValues[k];
                float diff = Math.abs(lastExtremes[extType][k] - lastExtremes[1 - extType][k]);
                if (diff > SENSITIVITY) {
                    boolean isAlmostAsLargeAsPrevious = diff > (lastDiff[k] * 2 / 3);
                    boolean isPreviousLargeEnough = lastDiff[k] > (diff / 3);
                    boolean isNotContra = (lastMatch != 1 - extType);
                    if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                        end  = System.currentTimeMillis();
                        if (end - start > 500) {
                            CURRENT_STEP++;
                            lastMatch = extType;
                            start = end;
                        }
                    } else {
                        lastMatch = -1;
                    }
                }
                lastDiff[k] = diff;
            }
            lastDirections[k] = direction;
            lastValues[k] = v;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static void reviseSensitivity(float sensitivity) {
        SENSITIVITY = sensitivity;
    }
}
