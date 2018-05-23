package eu.skowron.socket_test;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG = "MainActivity";

    TextView tv_pitch;
    TextView tv_roll;

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetic;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    float pitch_offset = 0.f;
    float roll_offset = 0.f;

    public static volatile String pitch_s = "";
    public static volatile String roll_s = "";
    public static volatile float pitch_f = 0.f;
    public static volatile float roll_f = 0.f;

    public static volatile boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        final Button bt_send = findViewById(R.id.bt_send);
        bt_send.setText("Start");
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!running){
                    updateOrientationAngles();
                    pitch_offset = pitch_f;
                    roll_offset = roll_f;
                    new Connection(pitch_s + " " + roll_s).execute();
                    bt_send.setText("Stop");
                }else{
                    bt_send.setText("Start");
                }

                running = !running;
            }
        });

        tv_pitch = findViewById(R.id.tv_pitch);
        tv_roll = findViewById(R.id.tv_roll);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetic,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        String azimuth = String.format("%1$.2f",  mOrientationAngles[0]);
        String pitch = String.format("pitch %1$.2f",  mOrientationAngles[1] - pitch_offset);
        String roll = String.format("roll %1$.2f",  mOrientationAngles[2] - roll_offset);

        tv_pitch.setText(pitch);
        tv_roll.setText(roll);

        pitch_s = pitch;
        roll_s = roll;

        pitch_f = mOrientationAngles[1];
        roll_f = mOrientationAngles[2];
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    class Connection extends AsyncTask<Void, Void, Void>{

        String data;

        Connection(String data){
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Socket socket = new Socket("192.168.4.1", 8080);
                OutputStream outputStream = socket.getOutputStream();//new ByteArrayOutputStream(1024);
                byte[] buffer;

                buffer = data.getBytes();

                outputStream.write(buffer);
                outputStream.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(running)
                new Connection(pitch_s + " " + roll_s).execute();
        }
    }
}

