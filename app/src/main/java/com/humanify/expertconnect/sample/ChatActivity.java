package com.humanify.expertconnect.sample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
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
import com.humanify.expertconnect.ExpertConnectLog;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.ExpertConnectConversationApi;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.action.AnswerEngineAction;
import com.humanify.expertconnect.api.model.action.ChatAction;
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
import com.humanify.expertconnect.api.model.conversationengine.MediaUpload;
import com.humanify.expertconnect.api.model.conversationengine.Message;
import com.humanify.expertconnect.api.model.conversationengine.NotificationMessage;
import com.humanify.expertconnect.api.model.conversationengine.PostSurveyEvent;
import com.humanify.expertconnect.api.model.conversationengine.RenderUrlCommand;
import com.humanify.expertconnect.api.model.conversationengine.SendQuestionCommand;
import com.humanify.expertconnect.api.model.conversationengine.StatusMessage;
import com.humanify.expertconnect.api.model.conversationengine.TextMessage;
import com.humanify.expertconnect.api.model.experts.SkillDetail;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityChat;
import com.humanify.expertconnect.util.ApiResult;
import com.humanify.expertconnect.view.compat.MaterialIconButton;
import com.humanify.expertconnect.view.compat.MaterialIconToggle;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity implements Holdr_ActivityChat.Listener {

    private final static String TAG = ChatActivity.class.getSimpleName();
    private final static String DEMO_SKILL = "CE_Mobile_Chat";

    private enum State {NONE, REQUESTED, CONNECTED, DISCONNECTED}
    private State state = State.NONE;

    private static final int WAIT_TIME = 5;
    private final static int LOADER_GET_ANSWERS = 1000;
    private final static int LOADER_CHECK_AGENTS = 1001;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;

    private Holdr_ActivityChat holdr;
    private ExpertConnectApiProxy api;
    private ExpertConnect expertConnect;
    private Channel chatChannel;
    private MessageAdapter messageAdapter;
    private boolean chatStateNotified = false;

    private int estimatedWaitTime = -1;
    private ColorFilter buttonEnabledTint;
    private Uri mediaPath;

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
            actionBar.setTitle(getString(com.humanify.expertconnect.R.string.expertconnect_chat));
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

        api.registerCreateChannel(createChannelReceiver);
        api.registerGetConversationEvent(conversationEventReceiver);

        // start chat without checking agent available and then start chat
        startChat(DEMO_SKILL);

        // check agent available and then start chat
        //checkAgentThenStartChat(DEMO_SKILL);
    }

    @Override
    protected void onDestroy() {
        endChat();

        if (createChannelReceiver != null)
            api.unregister(createChannelReceiver);
        if (conversationEventReceiver != null)
            api.unregister(conversationEventReceiver);

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
        selectImage();
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
            getSupportLoaderManager().initLoader(LOADER_GET_ANSWERS, bundle, answerEngineResponseCallback);
            Log.d(TAG, "***** Get Answer for [" + questionCommand.getText(getResources()) + "] " + message.getClass().getSimpleName());
            return;
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
        Log.d(TAG, "Chat Message : ADD " + message.getClass().getSimpleName());
        messageAdapter.getMessages().add(message);
        messageAdapter.notifyDataSetChanged();
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
                if (skillDetail.getChatReady() == 0) {
                    // error - show dialog and finish activity
                } else {
                    // connect
                    addChannel(currentSkill);
                }

            } catch (ApiException e) {
                Log.i(TAG, e.getUserMessage(getResources()));
            }
        }

        @Override
        public void onLoaderReset(Loader<ApiResult<SkillDetail>> loader) {

        }
    };

    private void addChannel(final String skill) {
        //ChatAction chatAction = ChatAction.getInstance(skill, null, null, new ChatChannelOptions("Call Center Low Level", "Student"));

        /* Sending key/value pair channel options*/
        HashMap<String, String> channelOptions = new HashMap<String, String>();
        channelOptions.put("department", "Call Center Low Level");
        channelOptions.put("userType", "Student");

        ChatAction chatAction = ChatAction.getInstance(skill, null, null, channelOptions);
        ChannelRequest channelRequest = new ChannelRequest.Builder(ChatActivity.this, chatAction)
                .setTo(skill)
                .setFrom(expertConnect.getUserId())
                .setSubject("help")
                .setMediaType(ChannelRequest.MEDIA_TYPE_CHAT)
                .setPriority(1)
                .build();
        api.createChannel(channelRequest);
    }

    private void startChat(final String skill) {
        addChannel(skill);
    }

    private void checkAgentThenStartChat(final String skill) {
        Bundle args = new Bundle();
        args.putString("skill", skill);
        getSupportLoaderManager().initLoader(LOADER_CHECK_AGENTS, args, skillLoader);
    }

    private void endChat() {
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
            CharSequence chatMessage = null;
            if (message instanceof AddParticipant) {
                chatMessage = ((AddParticipant)message).getText(getResources()).toString();
            } else if (message instanceof TextMessage) {
                chatMessage = ((TextMessage) message).getText(getResources()).toString();
                if (message instanceof SendQuestionCommand) {
                    SendQuestionCommand sendQuestionCommand = ((SendQuestionCommand)message);
                    if (!TextUtils.isEmpty(sendQuestionCommand.getAnswer())) {
                        chatMessage = Html.fromHtml(sendQuestionCommand.getAnswer());
                    }
                }
            } else if (message instanceof StatusMessage) {
                chatMessage = ((StatusMessage)message).getText(getResources()).toString();
            } else if (message instanceof MediaMessage) {
                chatMessage = "Image file sent successfully";
            }
            if (!TextUtils.isEmpty(chatMessage)) {
                holder.textMessage.setText(chatMessage);
            } else {
                holder.textMessage.setText("Message type [" + message.getClass().getSimpleName() + "] not handled.");
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public ArrayList<Message> getMessages() {
            return messages;
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle("Send Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals("Take Photo")) {
                    cameraIntent();

                } else if (items[item].equals("Choose from Gallery")) {
                    galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
        }
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ExpertConnectLog.Error("Humanify", ex.getMessage(), ex);
                Toast.makeText(this, "Unable to create image path", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    mediaPath = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photoFile);
                } else {
                    mediaPath = Uri.fromFile(photoFile);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mediaPath);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && mediaPath != null) {
            postMedia(mediaPath);
            Toast.makeText(this, "Took picture", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == Activity.RESULT_OK && ((mediaPath = data.getData()) != null)) {
            postMedia(mediaPath);
            Toast.makeText(this, "Chose from gallery", Toast.LENGTH_SHORT).show();
        }
        mediaPath = null;
    }

    private void postMedia(final Uri uri){
        expertConnect.getConversationApi().sendMedia(chatChannel,
                new MediaUpload(chatChannel, IdentityManager.getInstance(this).getUserId(), uri),
                new ExpertConnectConversationApi.SendListener() {
                    @Override
                    public void onSuccess() {
                        appendMessage(new ClientImage(uri));
                        Log.i(TAG, "Image file sent successfully");
                    }

                    @Override
                    public void onError(ApiException e) {
                        Log.e(TAG, "Image upload failed: " + e.getMessage());
                    }
                });
    }

}
