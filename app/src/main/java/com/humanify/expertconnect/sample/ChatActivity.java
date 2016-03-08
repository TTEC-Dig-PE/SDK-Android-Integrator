package com.humanify.expertconnect.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;

public class ChatActivity extends AppCompatActivity {

//    private Holdr_ActivityVoiceCallback holdr;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
