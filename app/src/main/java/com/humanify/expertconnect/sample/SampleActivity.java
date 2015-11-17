package com.humanify.expertconnect.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.model.ParcelableMap;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivitySample;
import com.humanify.expertconnect.view.compat.MaterialButton;

public class SampleActivity extends AppCompatActivity implements Holdr_ActivitySample.Listener {

    private static String USER_NAME = "Humanify Demo";
    private static String USER_ID = "demo@humanify.com";

    private static String DEMO_ANSWER_ENGINE = "FordPass";
    private static String DEMO_SKILL = "CE_Mobile_Chat";
    private static String DEMO_FORM = "agentperformance";

    private Holdr_ActivitySample holdr;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;


    private ApiBroadcastReceiver<ParcelableMap> decisionReceiver = new ApiBroadcastReceiver<ParcelableMap>(){

        @Override
        public void onSuccess(Context context, ParcelableMap result) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String responseData =  gson.toJson(result);
            holdr.message.setText(responseData);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Log.d("Retrofit_decision", error.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);

        expertConnect.setUserId(USER_ID);
        expertConnect.setUserName(USER_NAME);

        holdr = new Holdr_ActivitySample(findViewById(android.R.id.content));
        holdr.setListener(this);

        setSupportActionBar(holdr.toolbar);
        getSupportActionBar().setTitle(getString(R.string.humanify));
    }

    @Override
    public void onStart() {
        super.onStart();

        // register receiver
        api.registerPostDecisionData(decisionReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        // unregister receiver
        api.unregister(decisionReceiver);
    }

    @Override
    public void onStartChatClick(MaterialButton startChat) {
        api.startChat(DEMO_SKILL, null);
    }

    @Override
    public void onStartAnswerEngineClick(MaterialButton startAnswerEngine) {
        api.startAnswerEngine(DEMO_ANSWER_ENGINE);
    }

    @Override
    public void onStartVoiceCallbackClick(MaterialButton startVoiceCallback) {
        api.startVoiceCallback(DEMO_SKILL);
    }

    @Override
    public void onStartFormClick(MaterialButton startForm) {
        api.startInterviewForms(DEMO_FORM);
    }

    @Override
    public void onSendBreadcrumbClick(MaterialButton startForm) {

    }

    @Override
    public void onMakeDecisionClick(MaterialButton startForm) {
        ParcelableMap decisionDict = new ParcelableMap();
        decisionDict.put("name", "Henry Ford");
        decisionDict.put("ceTenant", "henry");
        decisionDict.put("eventId", "sayHello");

        api.postDecisionData(decisionDict);
    }
}
