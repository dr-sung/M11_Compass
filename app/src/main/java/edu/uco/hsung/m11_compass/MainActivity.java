package edu.uco.hsung.m11_compass;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private RelativeLayout viewFrame;
    private ArrowView arrowView;
    private float rotationDegree;

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

    private SensorChangeListener listener;
    private float[] gravity;
    private float[] magnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFrame = findViewById(R.id.view_frame);
        arrowView = new ArrowView(getApplicationContext());
        viewFrame.addView(arrowView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        listener = new SensorChangeListener();

        if (accelerometer == null) {
            Toast.makeText(MainActivity.this, "No Accelerometer available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (magnetometer == null) {
            Toast.makeText(MainActivity.this, "No Magnetometer available", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register sensor updates
        sensorManager.registerListener(listener, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private class SensorChangeListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravity = new float[3];
                System.arraycopy(e.values, 0, gravity, 0, 3);
            } else if (e.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetic = new float[3];
                System.arraycopy(e.values, 0, magnetic, 0, 3);
            }

            if (gravity != null && magnetic != null) {
                float[] rotation = new float[9];
                boolean success = SensorManager.getRotationMatrix(rotation, null, gravity, magnetic);
                if (success) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(rotation, orientation);

                    // rotation around Z-axis. Assume: phone is parallel to the ground
                    float rotationInRadians = orientation[0];
                    rotationDegree = (float) Math.toDegrees(rotationInRadians);

                    arrowView.invalidate(); // request to redraw

                    gravity = magnetic = null; // reset sensor event data
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    }

    private class ArrowView extends View {

        Bitmap compassImage;
        int imageWidth;
        int screenWidth, screenHeight;
        int centerX, centerY;
        int topX, leftY;

        public ArrowView(Context context) {
            super(context);

            compassImage = BitmapFactory.decodeResource(getResources(), R.drawable.compass);
            imageWidth = compassImage.getWidth();
        }

        // computer the location of the compass image
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            screenWidth = viewFrame.getWidth();
            screenHeight = viewFrame.getWidth();
            centerX = screenWidth / 2;
            centerY = screenHeight / 2;
            leftY = centerX - imageWidth / 2;
            topX = centerY - imageWidth / 2;
        }

        // redraw the commpass image
        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.rotate(-rotationDegree, centerX, centerY);
            canvas.drawBitmap(compassImage, leftY, topX, null);
            canvas.restore();
        }
    }
}
