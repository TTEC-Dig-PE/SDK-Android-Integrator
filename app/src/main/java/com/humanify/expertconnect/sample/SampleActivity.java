package com.humanify.expertconnect.sample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.ExpertConnectConversationApi;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.ExpertConnectNotification;
import com.humanify.expertconnect.api.model.JourneyResponse;
import com.humanify.expertconnect.api.model.ParcelableMap;
import com.humanify.expertconnect.api.model.breadcrumbs.BreadcrumbsAction;
import com.humanify.expertconnect.api.model.breadcrumbs.BreadcrumbsSession;
import com.humanify.expertconnect.api.model.conversationengine.ConversationEvent;
import com.humanify.expertconnect.api.model.form.Form;
import com.humanify.expertconnect.api.model.form.FormItem;
import com.humanify.expertconnect.sample.databinding.ActivitySampleBinding;
import com.humanify.expertconnect.view.compat.MaterialButton;

import java.util.HashMap;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SampleActivity extends AppCompatActivity implements ExpertConnectConversationApi.FormListener {

    public final static String TAG = "SampleActivity";
    private static String USER_NAME = "demo@humanify.com";
    private static String USER_ID = "demo@humanify.com";

    private static String DEMO_ANSWER_ENGINE = "Park";
    private static String DEMO_SKILL = "CE_Mobile_Chat";
    private static String DEMO_FORM = "rate_agent_form";

    private ActivitySampleBinding binding;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;

    private int noAnswerCount = 0;
    private static final int MAX_NO_ANSWER_COUNT = 2;

    boolean highLevelChatActive = false;
    Button chatst1;

    /*private ApiBroadcastReceiver<ParcelableMap> decisionReceiver = new ApiBroadcastReceiver<ParcelableMap>(){

        @Override
        public void onSuccess(Context context, ParcelableMap result) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String responseData =  gson.toJson(result);
            binding.message.setText(responseData);
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
            binding.message.setText(responseData);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding = DataBindingUtil.setContentView(this, R.layout.activity_sample);
       // binding.setHandler(new Handler());
        setContentView(R.layout.sample2);

        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);
        expertConnect.setFormListener(true, this);

        if (/* Static Token */ !expertConnect.isUserTokenProvided() /* &&  !expertConnect.isTokenProviderAvailable() Token Provider */) {
            showAccessTokenMissingDialog();
            return;
        }

        expertConnect.setUserId(USER_ID);
        expertConnect.setUserName(USER_NAME);
        chatst1 = findViewById(R.id.startchat1);

        /*setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(getString(R.string.humanify));
*/
        api.registerForSDKNotifications(notificationReceiver);

        registerConversation();

        /* Callback function to override the actions of end chat button. */
       /* expertConnect.setChatEndButtonListener(new ExpertConnect.ChatEndButtonListener() {
            @Override
            public boolean onEndButtonPressed(final com.humanify.expertconnect.activity.ChatActivity chatActivity) {
                if (chatActivity != null) {

                    //To check if the chat is disconnected or not.
                    if (chatActivity.isChatDisconnected()) {
                        return true;
                    }

                    //To check if the chat is in queue
                    if (chatActivity.getChatState() == com.humanify.expertconnect.activity.ChatActivity.CHAT_STATE_OPEN || chatActivity.getChatFragment().isWaitScreenVisible()) {
                        new AlertDialog.Builder(chatActivity)
                                .setTitle(com.humanify.expertconnect.R.string.expertconnect_leave_queue_title)
                                .setMessage(com.humanify.expertconnect.R.string.expertconnect_leave_queue_message)
                                .setPositiveButton(com.humanify.expertconnect.R.string.expertconnect_leave_queue_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //call this method for leave queue "yes" action.
                                        chatActivity.leaveQueueAction();
                                    }
                                })
                                .setNegativeButton(com.humanify.expertconnect.R.string.expertconnect_leave_queue_no, null)
                                .show();
                    } else {
                        // show alert dialog to end chat
                        new AlertDialog.Builder(chatActivity)
                                .setTitle(com.humanify.expertconnect.R.string.expertconnect_chat_exit_title)
                                .setMessage(com.humanify.expertconnect.R.string.expertconnect_exit_chat)
                                .setPositiveButton(com.humanify.expertconnect.R.string.expertconnect_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //call this method for ending chat "yes" action.
                                        chatActivity.endChatAction();
                                    }
                                })
                                .setNegativeButton(com.humanify.expertconnect.R.string.expertconnect_no, null)
                                .show();
                    }
                }
                return false;
            }
        });

        *//* Callback function to override the actions of back button. *//*
        expertConnect.setChatBackButtonListener(new ExpertConnect.ChatBackButtonListener() {
            @Override
            public boolean onBackButtonPressed(final com.humanify.expertconnect.activity.ChatActivity chatActivity, boolean fromNavigationUp) {
                //check if the chat is in queue
                if (chatActivity.getChatState() == com.humanify.expertconnect.activity.ChatActivity.CHAT_STATE_OPEN || chatActivity.getChatFragment().isWaitScreenVisible()) {
                    new AlertDialog.Builder(chatActivity)
                            .setTitle(com.humanify.expertconnect.R.string.expertconnect_leave_queue_title)
                            .setMessage(com.humanify.expertconnect.R.string.expertconnect_leave_queue_message)
                            .setPositiveButton(com.humanify.expertconnect.R.string.expertconnect_leave_queue_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //call this method for leave queue "yes" action.
                                    chatActivity.leaveQueueAction();
                                }
                            })
                            .setNegativeButton(com.humanify.expertconnect.R.string.expertconnect_leave_queue_no, null)
                            .show();
                } else {
                    //call this method for back button navigation.
                    return chatActivity.backKeyAction(fromNavigationUp);
                }
                return false;
            }
        });*/

        chatst1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  highLevelChatActive = true;
                api.startChat(DEMO_SKILL, null);
            }
        });
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
       /* if(highLevelChatActive && expertConnect.isChatActive()) {
            binding.startChat.setText(R.string.continue_chat);
        }
        if(expertConnect.isCallbackActive()) {
            binding.voiceCallback.setText(R.string.end_callback);
        }*/
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

    private static int interactionsCount = 0;

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
        binding.voiceCallback.setText(R.string.start_voice_callback);
        Toast.makeText(this, notification.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void handleChatEnd(ExpertConnectNotification notification) {
        binding.startChat.setText(R.string.start_chat);
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
            binding.startChat.setText(R.string.continue_chat);
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
                binding.message.setText(responseData);
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
                        binding.startChat.setText("*" + getResources().getString(R.string.continue_chat));
                    }
                }

                @Override
                public void onError(Context context, ApiException error) {
                    Log.d(getClass().getSimpleName(), error.getUserMessage(getResources()));
                }
            });
    }

    /**
     * Invoked when the user has navigated to the next question. This can be used to parse or react to a specific question being answered.
     *
     * @param context  FormActivity context returned from SDK
     * @param formItem The form item the user just navigated away from.
     * @param index    The index of the form item within the array of form elements.
     */
    @Override
    public void answeredFormItem(Activity context, FormItem formItem, int index) {
        Log.i(TAG, "User answered question " + index + " with answer: " + formItem.valueToText());
    }

    /**
     * Invoked when the user has navigated forward on the last question in the form, and the form has been submitted to the Humanify server.
     * This can be used to perform actions after a form is completed.
     *
     * @param context   FormActivity context returned from SDK
     * @param form      The form object containing each form element and potentially the user's answers to each item.
     * @param name      The form name
     * @param exception If an error occurred submitting the form
     */
    @Override
    public void submittedForm(Activity context, Form form, String name, ApiException exception) {
        Log.i(TAG, "User submitted form " + name);
    }

    /**
     * Invoked when the user clicks the Close button on the form submitted view. If your code contains this function, the SDK will perform no action after the user clicks close.
     * The transitioning and navigation stack manipulation will be left up to you. This can be used to override behavior after a form is completed,
     * such as moving straight into another high-level feature of the SDK.
     *
     * @param context FormActivity context returned from SDK
     * @param form    The form object containing each form element and potentially the user's answers to each item.
     */
    @Override
    public boolean closedWithForm(Activity context, Form form) {
        Log.i(TAG, "User closed the form view.");
        return true;
    }

    /**
     * Invoked when user gets error while loading the form. This can be used to perform actions after a form loading error occurred.
     *
     * @param context   FormActivity context returned from SDK
     * @param exception An error while loading the form
     */
    @Override
    public void formErrorRaised(Activity context, ApiException exception) {
        Log.i(TAG, "Error occurred while loading the form: " + exception);
    }

    public class Handler {

        public void onStartChatClick(MaterialButton startChat) {
            highLevelChatActive = true;
            api.startChat(DEMO_SKILL, null);
        }

        public void onStartAnswerEngineClick(MaterialButton startAnswerEngine) {
            api.startAnswerEngine(DEMO_ANSWER_ENGINE);
        }

        public void onStartVoiceCallbackClick(MaterialButton startVoiceCallback) {
            api.startVoiceCallback(DEMO_SKILL);
        }

        public void onStartFormClick(MaterialButton startForm) {
            api.startInterviewForms(DEMO_FORM);
        }

        public void onVoiceCallbackClick(MaterialButton voiceCallback) {
            if(expertConnect.isCallbackActive()) {
                api.closeReplyBackChannel(expertConnect.getCallbackChannel());
            } else {
                startActivity(new Intent(SampleActivity.this, VoiceCallbackActivity.class));
            }
        }

        public void onAnswerEngineClick(MaterialButton answerEngineCallback) {
            Toast.makeText(SampleActivity.this, "Coming soon...", Toast.LENGTH_LONG).show();
        }

        public void onChatClick(MaterialButton chatCallback) {
            startActivity(new Intent(SampleActivity.this, ChatActivity.class));
        }

        public void onSendBreadcrumbClick(MaterialButton startForm) {
            interactionsCount++;
            breadcrumbsAction(SampleActivity.this,
                    "User interaction count",
                    Integer.toString(interactionsCount),
                    "HumanifyDemo-SampleActivity",
                    "NA");
        }

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
                    binding.message.setText(responseData);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d("Retrofit_decision", error.getMessage());
                }
            });
        }

    }
}
