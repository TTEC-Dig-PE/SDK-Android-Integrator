package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.model.AgentAvailabilityResponse;
import com.humanify.expertconnect.api.model.ExpertConnectNotification;
import com.humanify.expertconnect.api.model.ParcelableMap;
import com.humanify.expertconnect.api.model.SkillStatus;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivitySample;
import com.humanify.expertconnect.util.ApiResult;
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

    ApiBroadcastReceiver<ExpertConnectNotification> notificationReceiver = new ApiBroadcastReceiver<ExpertConnectNotification>() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, ExpertConnectNotification result) {
            handleAllNotifications(result);
        }

        @Override
        public void onError(Context context, ApiException error) {

        }
    };

    LoaderManager.LoaderCallbacks<ApiResult<AgentAvailabilityResponse>> agentAvailabilityLoader = new LoaderManager.LoaderCallbacks<ApiResult<AgentAvailabilityResponse>>() {
        private String currentSkill;

        @Override
        public Loader<ApiResult<AgentAvailabilityResponse>> onCreateLoader(int id, Bundle args) {
            currentSkill = args.getString("skill");
            return api.getAgentAvilabilityOnSkill(currentSkill);
        }

        @Override
        public void onLoadFinished(Loader<ApiResult<AgentAvailabilityResponse>> loader, ApiResult<AgentAvailabilityResponse> data) {
            AgentAvailabilityResponse agentAvailResp = data.get();
            String message = "No Agents available for "+currentSkill;

            if(agentAvailResp.getCount()>0){
                for (SkillStatus skillStatus : agentAvailResp.getSkillsList()) {
                    if (skillStatus != null && skillStatus.getSkillName().equals(currentSkill)) {
                        int agentCount = skillStatus.getAgentsLoggedOn();
                        message = skillStatus.getSkillName()+" - "+ agentCount
                                + (agentCount > 1 ? " agents are " : " agent is ") +(skillStatus.isOpen()?"open":"close");
                    }
                }
            }
            Toast.makeText(SampleActivity.this, message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoaderReset(Loader<ApiResult<AgentAvailabilityResponse>> loader) {

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

        Bundle args = new Bundle();
        args.putString("skill", DEMO_SKILL);
        getSupportLoaderManager().initLoader(1, args, agentAvailabilityLoader);
    }

    @Override
    public void onStart() {
        super.onStart();

        // register decision receiver
        api.registerPostDecisionData(decisionReceiver);

        api.registerForSDKNotifications(notificationReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        // unregister decision receiver
        api.unregister(decisionReceiver);
        api.unregister(notificationReceiver);
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
        Toast.makeText(this, "Coming soon!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMakeDecisionClick(MaterialButton startForm) {
        ParcelableMap decisionDict = new ParcelableMap();
        decisionDict.put("name", "Henry Ford");
        decisionDict.put("ceTenant", "henry");
        decisionDict.put("eventId", "sayHello");

        api.postDecisionData(decisionDict);
    }

    private void handleAllNotifications(ExpertConnectNotification notification) {
        switch (notification.getType()){
            case ExpertConnectNotification.TYPE_AGENT_AVAILABILITY_INFO_UPDATED:
                handleAgentAvailabilityNotification(notification);
                break;
            case ExpertConnectNotification.TYPE_CHAT_ENDED:
                handleChatEnd(notification);
                break;
            case ExpertConnectNotification.TYPE_CALLBACK_ENDED:
                handleCallbackEnd(notification);
                break;
        }
    }

    private void handleCallbackEnd(ExpertConnectNotification notification) {
        String message = "Callback ended with reason - "+notification.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleChatEnd(ExpertConnectNotification notification) {
        String message = "Chat ended with reason - "+notification.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleAgentAvailabilityNotification(ExpertConnectNotification notification) {
        String skill = notification.getMessage();
        final ExpertConnectApiProxy api = ExpertConnectApiProxy.getInstance(this);

        Bundle args = new Bundle();
        args.putString("skill", skill);
        Toast.makeText(this, skill, Toast.LENGTH_LONG).show();
    }
}
