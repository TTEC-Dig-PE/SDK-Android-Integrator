package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.ExpertConnectNotification;
import com.humanify.expertconnect.api.model.JourneyResponse;
import com.humanify.expertconnect.api.model.ParcelableMap;
import com.humanify.expertconnect.api.model.breadcrumbs.BreadcrumbsAction;
import com.humanify.expertconnect.api.model.breadcrumbs.BreadcrumbsSession;
import com.humanify.expertconnect.api.model.conversationengine.ConversationEvent;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivitySample;
import com.humanify.expertconnect.view.compat.MaterialButton;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SampleActivity extends AppCompatActivity implements Holdr_ActivitySample.Listener {

    private static String USER_NAME = "Humanify Demo";
    private static String USER_ID = "demo@humanify.com";

    private static String DEMO_ANSWER_ENGINE = "Park";
    private static String DEMO_SKILL = "CE_Mobile_Chat";
    private static String DEMO_FORM = "rate_agent_form";

    private Holdr_ActivitySample holdr;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;

    private int noAnswerCount = 0;
    private static final int MAX_NO_ANSWER_COUNT = 2;

    boolean highLevelChatActive = false;

    /*private ApiBroadcastReceiver<ParcelableMap> decisionReceiver = new ApiBroadcastReceiver<ParcelableMap>(){

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
    };*/

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

    ApiBroadcastReceiver<JourneyResponse> journeyReceiver = new ApiBroadcastReceiver<JourneyResponse>() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, JourneyResponse result) {
            IdentityManager identityManager = ExpertConnect.getInstance(context).getIdentityManager();
            String journeyId = result.getId();
            identityManager.setJourneyId(journeyId);
            breadcrumbsSession(context);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
        }
    };

    /*ApiBroadcastReceiver<BreadcrumbsSession> breadcrumbsSessionReceiver = new ApiBroadcastReceiver<BreadcrumbsSession>() {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, BreadcrumbsSession result) {
            expertConnect.setBreadcrumbsSessionId(result.getSessionId());

            breadcrumbsAction(context,
                    "Initialize",
                    "Application Journey Initialization",
                    "CreateJourney",
                    "NA");
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
        }
    };*/

    /*ApiBroadcastReceiver<BreadcrumbsAction> breadcrumbsActionReceiver = new ApiBroadcastReceiver<BreadcrumbsAction>() {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, BreadcrumbsAction result) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String responseData =  "Breadcrumb Action Sent\n" + gson.toJson(result) + "\n";
            holdr.message.setText(responseData);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);

        if (/* Static Token */ !expertConnect.isUserTokenProvided() /* &&  !expertConnect.isTokenProviderAvailable() Token Provider */) {
            showAccessTokenMissingDialog();
            return;
        }

        expertConnect.setUserId(USER_ID);
        expertConnect.setUserName(USER_NAME);

        holdr = new Holdr_ActivitySample(findViewById(android.R.id.content));
        holdr.setListener(this);

        setSupportActionBar(holdr.toolbar);
        getSupportActionBar().setTitle(getString(R.string.humanify));

        api.registerForSDKNotifications(notificationReceiver);

        registerConversation();
    }

    @Override
    protected void onDestroy() {
        api.unregister(notificationReceiver);
        unregisterConversation();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(highLevelChatActive && expertConnect.isChatActive()) {
            holdr.startChat.setText(R.string.continue_chat);
        }
        if(expertConnect.isCallbackActive()) {
            holdr.voiceCallback.setText(R.string.end_callback);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // register
        //api.registerPostDecisionData(decisionReceiver);
        api.registerCreateJourney(journeyReceiver);
        //api.registerBreadcrumbsSession(breadcrumbsSessionReceiver);
        //api.registerBreadcrumbsAction(breadcrumbsActionReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        // unregister
        //api.unregister(decisionReceiver);
        api.unregister(journeyReceiver);
        //api.unregister(breadcrumbsSessionReceiver);
        //api.unregister(breadcrumbsActionReceiver);
    }

    @Override
    public void onStartChatClick(MaterialButton startChat) {
        highLevelChatActive = true;
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
    public void onVoiceCallbackClick(MaterialButton voiceCallback) {
        if(expertConnect.isCallbackActive()) {
            api.closeReplyBackChannel(expertConnect.getCallbackChannel());
        } else {
            startActivity(new Intent(this, VoiceCallbackActivity.class));
        }
    }

    @Override
    public void onAnswerEngineClick(MaterialButton answerEngineCallback) {
        Toast.makeText(this, "Coming soon...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onChatClick(MaterialButton chatCallback) {
        startActivity(new Intent(this, ChatActivity.class));
    }

    private static int interactionsCount = 0;
    @Override
    public void onSendBreadcrumbClick(MaterialButton startForm) {
        interactionsCount++;
        breadcrumbsAction(this,
                "User interaction count",
                Integer.toString(interactionsCount),
                "HumanifyDemo-SampleActivity",
                "NA");
    }

    @Override
    public void onMakeDecisionClick(MaterialButton startForm) {
        ParcelableMap decisionDict = new ParcelableMap();
        decisionDict.put("tenantId", "sce1_ops");
        decisionDict.put("projectServiceName", "HuSimple");
        decisionDict.put("eventId", "validateDE");
        decisionDict.put("inputString", "hello world");

        api.postDecisionData(decisionDict, new Callback<ParcelableMap>() {
            @Override
            public void success(ParcelableMap result, Response response) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String responseData = gson.toJson(result);
                holdr.message.setText(responseData);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Retrofit_decision", error.getMessage());
            }
        });
    }

    private void handleAllNotifications(ExpertConnectNotification notification) {
        switch (notification.getType()){
            case ExpertConnectNotification.TYPE_CHAT_ENDED:
                handleChatEnd(notification);
                break;
            case ExpertConnectNotification.TYPE_CHAT_OPEN_FAILED:
                handleChatOpenFailed(notification);
                break;
            case ExpertConnectNotification.TYPE_CHAT_LEFT_WITHOUT_ENDING:
                handleChatLeftWithoutEnding(notification);
                break;
            case ExpertConnectNotification.TYPE_CALLBACK_ENDED:
                handleCallbackEnd(notification);
                break;
            case ExpertConnectNotification.TYPE_ANSWER_ENGINE_NO_ANSWER:
                handleNoAnswer(notification);
                break;
        }
    }

    private void handleCallbackEnd(ExpertConnectNotification notification) {
        holdr.voiceCallback.setText(R.string.start_voice_callback);
        Toast.makeText(this, notification.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void handleChatEnd(ExpertConnectNotification notification) {
        holdr.startChat.setText(R.string.start_chat);
    }

    private void handleNoAnswer(ExpertConnectNotification notification) {
        noAnswerCount++;
        if ( (noAnswerCount%MAX_NO_ANSWER_COUNT) == 0) {
            ExpertConnectApiProxy.
                getInstance(getApplicationContext())
                .sendNotification(new ExpertConnectNotification(
                    ExpertConnectNotification.TYPE_WORKFLOW_ESCALATE_TO_CHAT, DEMO_SKILL));
        }
    }

    private void handleChatOpenFailed(ExpertConnectNotification notification) {
        String message = notification.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleChatLeftWithoutEnding(ExpertConnectNotification notification) {
        if(highLevelChatActive && expertConnect.isChatActive()) {
            holdr.startChat.setText(R.string.continue_chat);
        }
    }

    private void breadcrumbsSession(final Context context) {

        if(expertConnect.getIdentityManager().getJourneyId() == null)
            return;

        BreadcrumbsSession breadcrumbsSession = expertConnect.newBreadcrumbsSession();

        breadcrumbsSession.setJourneyId(expertConnect.getIdentityManager().getJourneyId());
        breadcrumbsSession.setTenantId(expertConnect.getOrganization());

        api.breadcrumbsSession(breadcrumbsSession, new Callback<BreadcrumbsSession>() {
            @Override
            public void success(BreadcrumbsSession result, Response response) {
                expertConnect.setBreadcrumbsSessionId(result.getSessionId());

                breadcrumbsAction(context,
                        "Initialize",
                        "Application Journey Initialization",
                        "CreateJourney",
                        "NA");
            }

            @Override
            public void failure(RetrofitError error) {
                ApiException apiException = new ApiException(error);
                Toast.makeText(context, apiException.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void breadcrumbsAction(final Context context,
                                   String actionType,
                                   String actionDescription,
                                   String actionSource,
                                   String actionDestination) {

        BreadcrumbsAction breadcrumbsAction = expertConnect.newBreadcrumbsAction();

        breadcrumbsAction.setJourneyId(expertConnect.getIdentityManager().getJourneyId());
        breadcrumbsAction.setSessionId(expertConnect.getBreadcrumbsSessionId());
        breadcrumbsAction.setTenantId(expertConnect.getOrganization());
        breadcrumbsAction.setActionType(actionType);
        breadcrumbsAction.setActionDescription(actionDescription);
        breadcrumbsAction.setActionSource(actionSource);
        breadcrumbsAction.setActionDestination(actionDestination);

        api.breadcrumbSendOne(breadcrumbsAction, new Callback<BreadcrumbsAction>() {
            @Override
            public void success(BreadcrumbsAction result, Response response) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String responseData =  "Breadcrumb Action Sent\n" + gson.toJson(result) + "\n";
                holdr.message.setText(responseData);
            }

            @Override
            public void failure(RetrofitError error) {
                ApiException apiException = new ApiException(error);
                Toast.makeText(context, apiException.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAccessTokenMissingDialog() {
        new AlertDialog.Builder(this)
            .setMessage("Set your access token in source code")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .show();
    }

    /*
     * Show that Host APP can receive messages while chat is active
     */
    ApiBroadcastReceiver<ConversationEvent> receiverConversationEvent;
    private void unregisterConversation() {
        ExpertConnectApiProxy.getInstance(this).unregister(receiverConversationEvent);
    }
    private void registerConversation() {
        ExpertConnectApiProxy.getInstance(this)
            .registerGetConversationEvent(receiverConversationEvent = new ApiBroadcastReceiver<ConversationEvent>() {
                @Override
                public void onSuccess(Context context, ConversationEvent result) {
                    if (highLevelChatActive && expertConnect.isChatActive()) {
                        holdr.startChat.setText("*" + getResources().getString(R.string.continue_chat));
                    }
                }

                @Override
                public void onError(Context context, ApiException error) {
                    Log.d(getClass().getSimpleName(), error.getUserMessage(getResources()));
                }
            });
    }
}
