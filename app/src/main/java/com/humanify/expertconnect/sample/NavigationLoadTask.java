package com.humanify.expertconnect.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.humanify.expertconnect.api.ActionDeserializer;
import com.humanify.expertconnect.api.model.action.Action;
import com.humanify.expertconnect.api.model.action.FormAction;
import com.humanify.expertconnect.api.model.action.NavigationAction;
import com.humanify.expertconnect.api.model.action.WebAction;
import com.humanify.expertconnect.api.model.action.ChatAction;
import com.humanify.expertconnect.sample.api.model.AppAction;
import com.humanify.expertconnect.sample.api.model.ProfileAction;
import com.humanify.expertconnect.sample.api.model.SettingsAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationLoadTask extends AsyncTask<Void, Void, Result<List<Action>>> {
    public static final String BROADCAST = "com.humanify.expertconnect.sample.navigation.BROADCAST";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_ERROR = "error";

    private AssetManager assetManager;
    private LocalBroadcastManager bm;
    private Result<List<Action>> result;

    private Gson gson;
    private Type listType = new TypeToken<List<Action>>() {}.getType();

    public NavigationLoadTask(Context context) {
        assetManager = context.getAssets();
        bm = LocalBroadcastManager.getInstance(context);

        Map<String, Class<? extends Action>> actionMap = new HashMap<>();
        actionMap.put(Action.TYPE_NAVIGATION, NavigationAction.class);
        actionMap.put(Action.TYPE_WEB, WebAction.class);
        actionMap.put(Action.TYPE_FORM, FormAction.class);
        actionMap.put(AppAction.TYPE_PROFILE, ProfileAction.class);
        actionMap.put(AppAction.TYPE_SETTINGS, SettingsAction.class);
        // todrichards 6/11/2015
        actionMap.put(Action.TYPE_CHAT, ChatAction.class);

        gson = new GsonBuilder()
                .registerTypeAdapter(Action.class, new ActionDeserializer(actionMap))
                .create();
    }

    @Override
    protected Result<List<Action>> doInBackground(Void... params) {
        if (result != null && result.isSuccess()) {
            return result;
        }
        try {
            return result = Result.success(readFromAssets());
        } catch (IOException e) {
            return Result.error(e);
        }
    }

    private List<Action> readFromAssets() throws IOException {
        InputStream in = null;
        try {
            in = assetManager.open("navigation.json");
            return gson.fromJson(new InputStreamReader(in, "UTF-8"), listType);
        } finally {
            if (in != null) in.close();
        }
    }

    @Override
    protected void onPostExecute(Result<List<Action>> result) {
        Intent intent = new Intent(BROADCAST);
        if (result.isSuccess()) {
            intent.putParcelableArrayListExtra(EXTRA_RESULT, new ArrayList<>(result.getSuccess()));
        } else {
            intent.putExtra(EXTRA_ERROR, result.getError());
        }
        bm.sendBroadcast(intent);
    }

    public static abstract class NavigationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<Action> result = intent.getParcelableArrayListExtra(EXTRA_RESULT);
            if (result != null) {
                onSuccess(context, result);
            } else {
                Exception error = (Exception) intent.getSerializableExtra(EXTRA_ERROR);
                onError(context, error);
            }
        }

        public abstract void onSuccess(Context context, List<Action> items);

        public abstract void onError(Context context, Exception error);
    }
}
