package com.shane.temperaturesensor;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.robocatapps.thermodosdk.Thermodo;
import com.robocatapps.thermodosdk.ThermodoFactory;
import com.robocatapps.thermodosdk.ThermodoListener;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends Activity {

    static final long UPDATE_INTERVAL_MILLIS = 1000 * 60 * 60 * 12;
    static final long MESSAGE_INTERVAL_MILLIS = 1000 * 60 * 60 * 12;
    static final int WARNING_TEMP = 50;
    static final int LOWEST_TEMP = 45;
    static final int HIGHEST_TEMP = 75;

    Twitter mTwitter;
    Thermodo mSensor;
    View mBackground;
    TextView mTemperature;

    long mLastUpdated;
    long mLastMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("4TFTwReKyZIwZhKlW5XiABgQS")
                .setOAuthConsumerSecret("IQRiIgj2My2NqoqoaDfPp4yztt9e3RK7yPMNwn8vyDYZfE0jYc")
                .setOAuthAccessToken("3831119833-bqtabbrCqPqz3wLGw60QCdrbyg9DlYyEZZm7auH")
                .setOAuthAccessTokenSecret("6A3aKWewSiXvsUT0qwqq40pi3cQnLpKYRRmeztbeN0rzO");

        mTwitter = new TwitterFactory(cb.build()).getInstance();

        mBackground = findViewById(R.id.container_main);
        mTemperature = (TextView) findViewById(R.id.txt_temp);

        mSensor = ThermodoFactory.getThermodoInstance(getApplicationContext());
        mSensor.setThermodoListener(new ThermodoListener() {
            @Override
            public void onStartedMeasuring() {

            }

            @Override
            public void onStoppedMeasuring() {

            }

            @Override
            public void onTemperatureMeasured(float temperature) {
                int degrees = (int) Math.round((temperature * (9.0 / 5.0)) + 32f);
                updateDisplay(degrees);
                sendUpdatesIfNecessary(degrees);
            }

            @Override
            public void onErrorOccurred(int i) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensor.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensor.stop();
    }

    private void sendUpdatesIfNecessary(int degrees) {

        long currentTime = System.currentTimeMillis();

        if ((currentTime - mLastUpdated) > UPDATE_INTERVAL_MILLIS) {
            mLastUpdated = currentTime;
            new UpdateTwitterTask().execute("Current temperature: " + degrees);
        }

        if ((degrees < WARNING_TEMP) && ((currentTime - mLastMessage) > MESSAGE_INTERVAL_MILLIS)) {
            mLastMessage = currentTime;
            new SendDirectMessageTask().execute("Current temperature: " + degrees);
        }
    }

    private void updateDisplay(int degrees) {

        mTemperature.setText("" + degrees);

        // 75 is the max, 45 is the min
        degrees = Math.min(degrees, HIGHEST_TEMP);
        degrees = Math.max(degrees, LOWEST_TEMP);

        double level = degrees / (HIGHEST_TEMP * 1.0);
        int red = (int) Math.round(level * 255);
        int blue = 255 - red;

        mBackground.setBackgroundColor(Color.argb(255, red, 0, blue));
    }

    private class SendDirectMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... message) {

            try {
                mTwitter.sendDirectMessage("@shane_ewert", message[0]);
            } catch (Exception e) {
                Log.e("MainActivityFragment", "failed to post update", e);
            }

            return null;
        }
    }

    private class UpdateTwitterTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... update) {

            try {
                mTwitter.updateStatus(update[0]);
            } catch (Exception e) {
                Log.e("MainActivityFragment", "failed to post update", e);
            }

            return null;
        }
    }
}
