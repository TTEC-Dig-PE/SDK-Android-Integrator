package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.EmptyApiBroadcastReceiver;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.SkillDetail;
import com.humanify.expertconnect.api.model.action.AnswerEngineAction;
import com.humanify.expertconnect.api.model.answerengine.AnswerEngineRequest;
import com.humanify.expertconnect.api.model.answerengine.AnswerEngineResponse;
import com.humanify.expertconnect.api.model.conversationengine.AddParticipant;
import com.humanify.expertconnect.api.model.conversationengine.Channel;
import com.humanify.expertconnect.api.model.conversationengine.ChannelRequest;
import com.humanify.expertconnect.api.model.conversationengine.ChannelState;
import com.humanify.expertconnect.api.model.conversationengine.ChannelTimeoutWarning;
import com.humanify.expertconnect.api.model.conversationengine.ChatMessage;
import com.humanify.expertconnect.api.model.conversationengine.ChatState;
import com.humanify.expertconnect.api.model.conversationengine.ClientImage;
import com.humanify.expertconnect.api.model.conversationengine.ClientPDF;
import com.humanify.expertconnect.api.model.conversationengine.ClientVideo;
import com.humanify.expertconnect.api.model.conversationengine.ConversationEvent;
import com.humanify.expertconnect.api.model.conversationengine.MediaMessage;
import com.humanify.expertconnect.api.model.conversationengine.Message;
import com.humanify.expertconnect.api.model.conversationengine.NotificationMessage;
import com.humanify.expertconnect.api.model.conversationengine.PostSurveyEvent;
import com.humanify.expertconnect.api.model.conversationengine.RenderUrlCommand;
import com.humanify.expertconnect.api.model.conversationengine.SendQuestionCommand;
import com.humanify.expertconnect.api.model.conversationengine.StatusMessage;
import com.humanify.expertconnect.api.model.conversationengine.TextMessage;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityChat;
import com.humanify.expertconnect.util.ApiResult;
import com.humanify.expertconnect.view.compat.MaterialIconButton;
import com.humanify.expertconnect.view.compat.MaterialIconToggle;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements Holdr_ActivityChat.Listener {

    private final static String TAG = ChatActivity.class.getSimpleName();
    //private final static String DEMO_SKILL = "CE_Mobile_Chat";
    private final static String DEMO_SKILL = "Calls for nainesh_mktwebextc";

    private final static int LOADER_CHECK_AGENT = 1000;
    private final static int LOADER_GET_ANSWERS = 1001;

    private enum State {NONE, REQUESTED, CONNECTED, DISCONNECTED}

    private Holdr_ActivityChat holdr;

    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;

    private Channel chatChannel;
    private State state = State.NONE;
    private MessageAdapter messageAdapter;
    private boolean chatStateNotified = false;

    private static final int WAIT_TIME = 5;
    private int estimatedWaitTime = -1;

    private ColorFilter buttonEnabledTint;

    private ApiBroadcastReceiver<Channel> createChannelReceiver = new ApiBroadcastReceiver<Channel>() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        @Override
        public void onSuccess(Context context, Channel result) {
            Log.d(TAG, "Channel created : " + (chatChannel != null));
            chatChannel = result;
            state = State.CONNECTED;
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            Log.d(TAG, error.getMessage(), error);
        }
    };

    private ApiBroadcastReceiver<ConversationEvent> conversationEventReceiver = new ApiBroadcastReceiver<ConversationEvent>() {
        @Override
        public void onSuccess(Context context, ConversationEvent result) {
            Log.d(TAG, "Channel ConversationEvent received");
            handleConversationEvent(result);
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
            Log.d(TAG, "Channel closed");
            chatChannel = null;
            state = State.DISCONNECTED;
            setEnableEntry(false);
        }

        @Override
        public void onError(Context context, ApiException error) {
            Toast.makeText(context, error.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            Log.d(TAG, error.getMessage(), error);
        }
    };

    private LoaderManager.LoaderCallbacks<ApiResult<AnswerEngineResponse>> answerEngineResponseCallback
            = new LoaderManager.LoaderCallbacks<ApiResult<AnswerEngineResponse>>() {

        SendQuestionCommand sendQuestionCommand;

        @Override
        public Loader<ApiResult<AnswerEngineResponse>> onCreateLoader(int id, Bundle args) {

            sendQuestionCommand = args.getParcelable("questionCommand");

            String question = sendQuestionCommand.getQuestionText();
            String context = sendQuestionCommand.getInterfaceName();

            AnswerEngineAction answerEngineAction = new AnswerEngineAction().withQuestion(question);
            answerEngineAction.setAnswerEngineContext(context);
            AnswerEngineRequest req = new AnswerEngineRequest.Builder(answerEngineAction).build();
            return api.getAnswer(req);
        }

        @Override
        public void onLoadFinished(Loader<ApiResult<AnswerEngineResponse>> loader, ApiResult<AnswerEngineResponse> data) {
            try {
                AnswerEngineResponse answerEngineResponse = data.get();
                String answer = answerEngineResponse.getAnswer();
                sendQuestionCommand.setAnswer(answer);
            } catch (ApiException e) {
                sendQuestionCommand.setAnswer(e.getMessage());
            }
            appendMessage(sendQuestionCommand);
        }

        @Override
        public void onLoaderReset(Loader<ApiResult<AnswerEngineResponse>> loader) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        holdr = new Holdr_ActivityChat(findViewById(android.R.id.content));
        holdr.setListener(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(com.humanify.expertconnect.R.string.expertconnect_phone_call));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        api = ExpertConnectApiProxy.getInstance(this);
        expertConnect = ExpertConnect.getInstance(this);

        buttonEnabledTint = new PorterDuffColorFilter(getResources().getColor(R.color.expertconnect_color_blue), PorterDuff.Mode.SRC_IN);
        holdr.chatMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    holdr.send.setEnabled(false);
                    holdr.send.setColorFilter(null);
                } else {
                    holdr.send.setEnabled(true);
                    holdr.send.setColorFilter(buttonEnabledTint);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1 && !chatStateNotified) {
                    chatStateNotified = true;
                    sendChatStateMessage(ChatState.STATE_COMPOSING);
                }
            }
        });

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        holdr.chatList.setLayoutManager(manager);

        messageAdapter = new MessageAdapter();
        holdr.chatList.setAdapter(messageAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        api.registerCreateChannel(createChannelReceiver);
        api.registerGetConversationEvent(conversationEventReceiver);
        api.registerCloseReplyBackChannel(closeChannelReceiver);

        startChat(DEMO_SKILL);
    }

    @Override
    public void onStop() {
        super.onStop();

        endChat();

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
    public void onAttachImageClick(MaterialIconToggle attachImage) {
    }

    @Override
    public void onCancelClick(MaterialIconButton cancel) {
    }

    @Override
    public void onSendClick(MaterialIconButton send) {
        String message = holdr.chatMessage.getText().toString();
        ChatMessage chatMessage = new ChatMessage.Builder(chatChannel, message)
                .setFrom(IdentityManager.getInstance(this).getUserName())
                .build();
        appendMessage(chatMessage);
        holdr.chatMessage.setText("");
        api.sendMessage(chatMessage);
    }

    private void sendChatStateMessage(String composing) {
        ChatState chatState = new ChatState.Builder(chatChannel)
                .setFrom(IdentityManager.getInstance(this).getUserName())
                .setState(ChatState.STATE_COMPOSING)
                .setDuration(Long.valueOf(10000))
                .build();
        api.sendChatStateMessage(chatState);
    }

    private void setEnableEntry(boolean enable) {
        holdr.chatMessage.setEnabled(enable);
    }

    private void handleConversationEvent(ConversationEvent result) {
        if (!expertConnect.isChatActive() && chatChannel == null) {
            return;
        }
        if (result instanceof ChannelState) {
            handleChannelState((ChannelState)result);
        } else if (result instanceof ChatState) {
            handleChatState((ChatState)result);
        } else if (result instanceof Message) {
            handleMessage((Message)result);
        } else if (result instanceof ChannelTimeoutWarning) {
            handleChannelTimeoutWarning((ChannelTimeoutWarning)result);
        } else if (result instanceof PostSurveyEvent) {
            handlePostSurveyEvent((PostSurveyEvent)result);
        } else {
            throw new IllegalArgumentException("Unknown conversation event: " + result);
        }
    }

    private void handleChannelState(ChannelState state) {
        switch(state.getState()) {
            case ChannelState.STATE_ANSWERED:
                Log.d(TAG, "Channel answered by agent");
                setEnableEntry(true);
                break;
            case ChannelState.STATE_DISCONNECTED:
            case ChannelState.STATE_TIMEOUT:
                Log.d(TAG, "Channel disconnected by agent");
                ChatMessage chatMessage = new ChatMessage.Builder(chatChannel,
                        state.getState().equals(ChannelState.STATE_DISCONNECTED) ? "Agent disconnected" : "Chat session timeout")
                        .setFrom(IdentityManager.getInstance(this).getUserName())
                        .build();
                appendMessage(chatMessage);
                api.sendMessage(chatMessage);
                this.state = State.DISCONNECTED;
                setEnableEntry(false);
                break;
            default:
                estimatedWaitTime = chatChannel.getEstimatedWait();
                Log.d(TAG, "Estimated Wait Time = " + estimatedWaitTime);
                break;
        }
    }

    private void handleChatState(ChatState state) {
        Log.d(TAG, "Handle Chat State Message");
    }

    private void handleChannelTimeoutWarning(ChannelTimeoutWarning channelTimeoutWarning) {
        Log.d(TAG, "Handle ChannelTimeoutWarning Message");
    }

    private void handlePostSurveyEvent(PostSurveyEvent postSurveyEvent) {
        Log.d(TAG, "Handle PostSurveyEvent Message");
    }

    private void handleMessage(Message message) {
        Message appendMessage = message;
        if (message instanceof RenderUrlCommand) {
            RenderUrlCommand renderUrlCommand = (RenderUrlCommand) message;
        } else if (message instanceof SendQuestionCommand) {
            SendQuestionCommand questionCommand = (SendQuestionCommand) message;
            Bundle bundle = new Bundle();
            bundle.putParcelable("questionCommand", questionCommand);
            getSupportLoaderManager().restartLoader(LOADER_GET_ANSWERS, bundle, answerEngineResponseCallback);
        } else if (message instanceof NotificationMessage) {
            MediaMessage mediaMessage = (MediaMessage) message;
            if (mediaMessage.getMediaType() == MediaMessage.MEDIA_IMAGE) {
                String mediaEndPoint = expertConnect.getEndPoint() + "/utils/v1/media/files?name=";
                appendMessage = new ClientImage(mediaMessage.getFrom(),
                        Uri.parse(mediaEndPoint + mediaMessage.getMediaUri()),
                        mediaMessage.getConversationId(),
                        mediaMessage.getChannelId(),
                        Message.OWNER_AGENT,
                        false);
            } else if (mediaMessage.getMediaType() == MediaMessage.MEDIA_VIDEO) {
                appendMessage = new ClientVideo(mediaMessage.getFrom(),
                        mediaMessage.getMediaUri(),
                        mediaMessage.getConversationId(),
                        mediaMessage.getChannelId(),
                        Message.OWNER_AGENT,
                        false);
            } else if (mediaMessage.getMediaType() == MediaMessage.MEDIA_PDF) {
                appendMessage = new ClientPDF(mediaMessage.getFrom(),
                        mediaMessage.getMediaUri(),
                        mediaMessage.getConversationId(),
                        mediaMessage.getChannelId(),
                        Message.OWNER_AGENT,
                        false);
            }
        }
        appendMessage(appendMessage);
    }

    public void appendMessage(Message message) {
        messageAdapter.getMessages().add(message);
        messageAdapter.notifyDataSetChanged();
    }

    private LoaderManager.LoaderCallbacks<ApiResult<SkillDetail>> skillLoader = new LoaderManager.LoaderCallbacks<ApiResult<SkillDetail>>() {
        private String currentSkill;
        @Override
        public Loader<ApiResult<SkillDetail>> onCreateLoader(int id, Bundle args) {
            currentSkill = args.getString("skill");
            Loader<ApiResult<SkillDetail>> loader = null;
            loader = api.getDetailsForSkill(currentSkill);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<ApiResult<SkillDetail>> loader, ApiResult<SkillDetail> data) {
            try {
                SkillDetail skillDetail = data.get();
                estimatedWaitTime = (skillDetail.getEstimatedWait() < 0 ? skillDetail.getEstimatedWait() : (int) Math.round(skillDetail.getEstimatedWait() / 60.0));
                if (skillDetail.getAgentsLoggedOn() == 0) {
                    // error - show dialog and finish activity
                } else {
                    // connect
                    ChannelRequest channelRequest = new ChannelRequest.Builder(ChatActivity.this)
                        .setTo(currentSkill)
                        .setFrom(expertConnect.getUserId())
                        .setSubject("help")
                        .setMediaType(ChannelRequest.MEDIA_TYPE_CHAT)
                        .setPriority(1)
                        .build();
                    api.createChannel(channelRequest);
                }

            } catch (ApiException e) {
                Log.i(TAG, e.getUserMessage(getResources()));
            }
        }

        @Override
        public void onLoaderReset(Loader<ApiResult<SkillDetail>> loader) {

        }
    };

    private void startChat(final String skill) {
        Bundle args = new Bundle();
        args.putString("skill", skill);
        getSupportLoaderManager().initLoader(0, args, skillLoader);
    }

    private void endChat() {
        Log.d(TAG, "Close Chat : " + expertConnect.isChatActive() + ":" + (chatChannel != null) );
        if (expertConnect.isChatActive()) {
            api.closeChannel(chatChannel);
        }
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;

        public MessageViewHolder(View itemView) {
            super(itemView);
            textMessage = (TextView) itemView.findViewById(R.id.textMessage);
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private ArrayList<Message> messages = new ArrayList<>();

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_chat_message, parent, false);
            MessageViewHolder holder = new MessageViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            String chatMessage = null;
            if (message instanceof AddParticipant) {
                chatMessage = ((AddParticipant)message).getText(getResources()).toString();
            } else if (message instanceof TextMessage) {
                chatMessage = ((TextMessage)message).getText(getResources()).toString();
            } else if (message instanceof StatusMessage) {
                chatMessage = ((StatusMessage)message).getText(getResources()).toString();
            }
            Log.d(TAG, "ChatMessage : " + message.getClass().getSimpleName() + "::" + chatMessage);
            if (!TextUtils.isEmpty(chatMessage)) {
                Log.d(TAG, "ChatMessage : " + message.getClass().getSimpleName() + "::" + chatMessage);
                holder.textMessage.setText(chatMessage);
            } else {
                holder.textMessage.setText("ChatMessage : " + message.getClass().getSimpleName());
            }
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "getItemCount : " + messages.size());
            return messages.size();
        }

        public ArrayList<Message> getMessages() {
            return messages;
        }
    }

}
