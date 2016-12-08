package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.EmptyApiBroadcastReceiver;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.model.conversationengine.Channel;
import com.humanify.expertconnect.api.model.conversationengine.ChannelRequest;
import com.humanify.expertconnect.api.model.conversationengine.ChannelState;
import com.humanify.expertconnect.api.model.conversationengine.ConversationEvent;
import com.humanify.expertconnect.api.model.experts.SkillDetail;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityVoicecallback;
import com.humanify.expertconnect.util.ApiResult;
import com.humanify.expertconnect.view.compat.MaterialButton;

public class VoiceCallbackActivity extends AppCompatActivity implements Holdr_ActivityVoicecallback.Listener {

    private final static String TAG = VoiceCallbackActivity.class.getSimpleName();
    private final static String DEMO_SKILL = "CE_Mobile_Chat";

    private enum State {NONE, REQUESTED, CONNECTED, DISCONNECTED}

    private Holdr_ActivityVoicecallback holdr;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;

    private Channel voiceChannel;
    private ApiBroadcastReceiver<Channel> createChannelReceiver = new ApiBroadcastReceiver<Channel>() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, Channel result) {
            voiceChannel = result;
            state = State.CONNECTED;
            holdr.requestCall.setText(R.string.cancel_callback);
        }

        @Override
        public void onError(Context context, ApiException error) {
            state = State.DISCONNECTED;
            holdr.requestCall.setText(R.string.request_callback);
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            Log.d(TAG, error.getUserMessage(getResources()));
        }
    };
    private ApiBroadcastReceiver<ConversationEvent> conversationEventReceiver = new ApiBroadcastReceiver<ConversationEvent>() {
        @Override
        public void onSuccess(Context context, ConversationEvent result) {
            if (result instanceof ChannelState) {
                if (voiceChannel != null) {
                    handleChannelState((ChannelState)result, voiceChannel);
                }
            }
        }

        @Override
        public void onError(Context context, ApiException error) {
            Log.d(TAG, error.getMessage(), error);
        }
    };
    private EmptyApiBroadcastReceiver closeChannelReceiver = new EmptyApiBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context) {
            voiceChannel = null;
            state = State.DISCONNECTED;
            holdr.requestCall.setText(R.string.request_callback);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            Log.d(TAG, error.getUserMessage(getResources()));
        }
    };

    private static final int WAIT_TIME = 5;
    private int estimatedWaitTime = -1;

    State state = State.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voicecallback);

        holdr = new Holdr_ActivityVoicecallback(findViewById(android.R.id.content));
        holdr.setListener(this);

        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(com.humanify.expertconnect.R.string.expertconnect_phone_call));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        holdr.requestCall.setEnabled(false);
        holdr.phoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        holdr.phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhoneNumber(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Bundle args = new Bundle();
        args.putString("skill", DEMO_SKILL);
        getSupportLoaderManager().initLoader(0, args, skillLoader);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        api.registerCreateChannel(createChannelReceiver);
        api.registerGetConversationEvent(conversationEventReceiver);
        api.registerCloseReplyBackChannel(closeChannelReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        api.unregister(createChannelReceiver);
        api.unregister(conversationEventReceiver);
        api.unregister(closeChannelReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestCallClick(MaterialButton requestCall) {
        switch(state) {
            case NONE:
            case DISCONNECTED:
                if (!expertConnect.isCallbackActive()) {
                    ChannelRequest channelRequest = new ChannelRequest.Builder(this)
                            .setTo(DEMO_SKILL)
                            .setFrom(expertConnect.getUserName())
                            .setSubject("help")
                            .setMediaType(ChannelRequest.MEDIA_TYPE_VOICE)
                            .setSourceType(ChannelRequest.SOURCE_TYPE_CALLBACK)
                            .setPriority(5)
                            .setSourceAddress(getPhoneNumber())
                            .build();
                    api.createChannel(channelRequest);
                    holdr.requestCall.setText(R.string.processing_callback);

                    state = State.REQUESTED;
                }
                break;
            case REQUESTED:
                if (!expertConnect.isCallbackActive()) {
                    Toast.makeText(this, "Your request is in progress", Toast.LENGTH_LONG).show();
                }
                break;
            case CONNECTED:
                if (expertConnect.isCallbackActive()) {
                    api.closeReplyBackChannel(voiceChannel);
                }
                break;
        }
    }

    @Override
    public boolean onPhoneNumberEditorAction(EditText phoneNumber, int actionId, KeyEvent event) {
        return false;
    }


    private void validatePhoneNumber(String number) {
        boolean isValid = PhoneNumberUtils.isGlobalPhoneNumber(PhoneNumberUtils.stripSeparators(number));
        holdr.requestCall.setEnabled(isValid);
    }

    private LoaderManager.LoaderCallbacks<ApiResult<SkillDetail>> skillLoader = new LoaderManager.LoaderCallbacks<ApiResult<SkillDetail>>() {
        private String currentSkill;
        @Override
        public Loader<ApiResult<SkillDetail>> onCreateLoader(int id, Bundle args) {
            currentSkill = args.getString("skill");
            Loader<ApiResult<SkillDetail>> loader = null;
            loader = api.getDetailsForExpertSkill(currentSkill);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<ApiResult<SkillDetail>> loader, ApiResult<SkillDetail> data) {
            try {
                SkillDetail skillDetail = data.get();
                estimatedWaitTime = (skillDetail.getEstWait() < 0 ? skillDetail.getEstWait() : (int) Math.round(skillDetail.getEstWait() / 60.0));
                holdr.estimatedWaitTime.setText(getResources().getQuantityString(R.plurals.agents_wait_time, estimatedWaitTime, estimatedWaitTime));
                holdr.estimatedWaitTime.setText(getWaitMessage(estimatedWaitTime, skillDetail.getVoiceReady()));
            } catch (ApiException e) {
                Log.i(TAG, e.getUserMessage(getResources()));
            }
        }

        @Override
        public void onLoaderReset(Loader<ApiResult<SkillDetail>> loader) {

        }
    };

    private String getWaitMessage(int estimatedWaitTIme, int agentsAvailable) {
        String waitMessage = "";

        if(estimatedWaitTIme < 0 || (estimatedWaitTIme == 0 && agentsAvailable == 0)){
            holdr.estimatedWaitTimeLabel.setVisibility(View.GONE);
            waitMessage = getString(R.string.no_agents_available);
        }
        else{
            holdr.estimatedWaitTimeLabel.setVisibility(View.VISIBLE);
            waitMessage = getResources().getQuantityString(R.plurals.agents_wait_time, estimatedWaitTIme, estimatedWaitTIme);
        }

        return waitMessage;
    }

    private void handleChannelState(ChannelState state, Channel channel){
        switch(state.getState()) {
            case ChannelState.STATE_ANSWERED:
                Log.d(TAG, "Channel answered by agent");
                holdr.requestCall.setText(R.string.end_callback);
                break;
            case ChannelState.STATE_DISCONNECTED:
            case ChannelState.STATE_TIMEOUT:
                Log.d(TAG, "Channel disconnected by agent");
                holdr.requestCall.setText(R.string.request_callback);
                this.state = State.DISCONNECTED;
                break;
            default:
                estimatedWaitTime = channel.getEstimatedWait();
                Log.d(TAG, "Estimated Wait Time = " + estimatedWaitTime);
                break;
        }
    }

    private String getPhoneNumber() {
        String phoneNumber = PhoneNumberUtils.stripSeparators(holdr.phoneNumber.getText().toString());
        if (phoneNumber.length() > 10) {
            // Assume numbers greater then 10 digits are international.
            phoneNumber = PhoneNumberUtils.stringFromStringAndTOA(phoneNumber, PhoneNumberUtils.TOA_International);
        }
        return phoneNumber;
    }
}
