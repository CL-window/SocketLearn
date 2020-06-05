package com.slack.androidclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.slack.androidclient.screen.ScreenPushClientActivity;
import com.slack.androidclient.screen.ScreenRecordActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void client(View view) {
        startActivity(new Intent(this, ClientActivity.class));
    }

    public void server(View view) {
        startActivity(new Intent(this, ServerActivity.class));
    }

    public void screenPush(View view) {
        startActivity(new Intent(this, ScreenRecordActivity.class));
    }

    public void screenPushClient(View view) {
        startActivity(new Intent(this, ScreenPushClientActivity.class));
    }
}
