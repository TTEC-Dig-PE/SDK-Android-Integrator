package com.humanify.expertconnect.sample;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.ExpertConnectConfig;
import com.humanify.expertconnect.ExpertConnectLog;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import static com.humanify.expertconnect.ExpertConnectLog.ExpertConnectLogLevelDebug;
import static com.humanify.expertconnect.ExpertConnectLog.ExpertConnectLogLevelError;
import static com.humanify.expertconnect.ExpertConnectLog.ExpertConnectLogLevelNone;
import static com.humanify.expertconnect.ExpertConnectLog.ExpertConnectLogLevelVerbose;
import static com.humanify.expertconnect.ExpertConnectLog.ExpertConnectLogLevelWarning;

public class MainApplication extends Application {

    public static final String API_ENDPOINT = "https://api.ce03.humanify.com";
    public static final String TOKEN = "";          // YOUR TOKEN GOES HERE

    // breadcrumb configuration
    public static final int CACHE_COUNT = 3;
    public static final int CACHE_TIME = 30; // seconds

    @Override
    public void onCreate() {
        super.onCreate();

        configureWithUserToken(TOKEN);
    }

    private void configureWithUserToken(String userToken) {
        final ExpertConnect expertConnect = ExpertConnect.getInstance(this);

        if (!TextUtils.isEmpty(userToken)) {
            // *********************************** Static User Token ***********************************;
            expertConnect.setConfig(new ExpertConnectConfig()
                    .setMainNavigationClass(SampleActivity.class)
                    .setEndpoint(MainApplication.API_ENDPOINT)
                    .setCacheCount(CACHE_COUNT)
                    .setCacheTime(CACHE_TIME)
                    .setUserIdentityToken(userToken)
                    .setAppName(getApplicationName(this))
                    .setAppVersion(BuildConfig.VERSION_NAME)
                    .setAppId(BuildConfig.APPLICATION_ID));
        } else {
            // *********************************** Token Provider ***********************************;
            expertConnect.setConfig(new ExpertConnectConfig()
                    .setMainNavigationClass(SampleActivity.class)
                    .setEndpoint(MainApplication.API_ENDPOINT)
                    .setCacheCount(CACHE_COUNT)
                    .setCacheTime(CACHE_TIME)
                    .setTokenProvider(new ExpertConnectConfig.TokenProvider() {
                        @Override
                        public String token() {
                            String token = getUserSessionToken();
                            if (!TextUtils.isEmpty(token)) {
                                return token;
                            }
                            return null;
                        }
                    })
                    .setAppName(getApplicationName(this))
                    .setAppVersion(BuildConfig.VERSION_NAME)
                    .setAppId(BuildConfig.APPLICATION_ID));
        }

        expertConnect.setDebugLevel(ExpertConnectLog.ExpertConnectLogLevelVerbose);
        expertConnect.setLoggingCallback(new ExpertConnect.LoggingCallback() {
            @Override
            public void getLog(int logLevel, String tag, String message, Throwable tr) {
                String levelString = getLogLevel(logLevel);
                if (levelString != null) {
                    String TAG = String.format("[Android SDK - %s : %s]", levelString, tag);
                    if (tr != null) {
                        Log.i(TAG, message, tr);
                    } else {
                        Log.i(TAG, message);
                    }
                }
            }
        });
    }

    private String getUserSessionToken() {
        final String TOKEN_PROVIDER_ENDPOINT = "Your TOKEN_PROVIDER_ENDPOINT goes here";
        final String TOKEN_PROVIDER_PATH = "Your TOKEN_PROVIDER_PATH goes here";
        final String USER_ID = "Your USER_ID goes here";

        String newAuthUrl = Uri.parse(TOKEN_PROVIDER_ENDPOINT).buildUpon()
                .appendEncodedPath(TOKEN_PROVIDER_PATH)
                .appendQueryParameter("username", USER_ID)
                .toString();

        Request newAuthRequest = new Request.Builder()
                .url(newAuthUrl)
                .get()
                .build();

        try {
            OkHttpClient client = new OkHttpClient();

            Response newAuthResponse = client.newCall(newAuthRequest).execute();
            if (newAuthResponse.isSuccessful()) {
                String userToken = newAuthResponse.body().string();
                return userToken;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLogLevel(int logLevel) {
        switch (logLevel) {
            case ExpertConnectLogLevelError:
                return "Error";
            case ExpertConnectLogLevelWarning:
                return "Warning";
            case ExpertConnectLogLevelDebug:
                return "Debug";
            case ExpertConnectLogLevelVerbose:
                return "Info";
            case ExpertConnectLogLevelNone:
                return "None";
            default:
                return null;
        }
    }

    public String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }
}
