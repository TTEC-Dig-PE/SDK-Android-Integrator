package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.form.Form;
import com.humanify.expertconnect.api.model.form.FormItem;
import com.humanify.expertconnect.api.model.form.TextFormItem;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityLogin;
import com.humanify.expertconnect.util.AnimationUtils;
import com.humanify.expertconnect.util.ApiResult;
import com.humanify.expertconnect.view.compat.MaterialButton;

public class LogInActivity extends AppCompatActivity implements Holdr_ActivityLogin.Listener, LoaderManager.LoaderCallbacks<ApiResult<Form>> {
    private static final String STATE_LOADING = "is_loading";

    private Holdr_ActivityLogin holdr;
    private ExpertConnectApiProxy api;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Holdr_ActivityLogin.LAYOUT);
        holdr = new Holdr_ActivityLogin(findViewById(android.R.id.content));
        holdr.setListener(this);
        api = ExpertConnectApiProxy.getInstance(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ValidatorWatcher watcher = new ValidatorWatcher();
        holdr.loginEmail.addTextChangedListener(watcher);
        holdr.loginEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (isValid()) {
                        send();
                        return true;
                    }
                }
                return false;
            }
        });

        float cardElevation = getResources().getDimension(R.dimen.expertconnect_elevation_card);
        float actionsElevation = getResources().getDimension(R.dimen.expertconnect_elevation_actions);

        ViewCompat.setElevation(holdr.answerCard, cardElevation);
        ViewCompat.setElevation(holdr.buttonContainer, actionsElevation);

        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(STATE_LOADING);
            if (isLoading) {
                holdr.loading.setVisibility(View.VISIBLE);
                getSupportLoaderManager().initLoader(0, null, this);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_LOADING, isLoading);
    }

    /**
     * Constructs a new intent to start this Activity.
     *
     * @param context an android context
     * @return the intent to start this Activity with
     */
    public static Intent newIntent(Context context) {
        return new Intent(context, LogInActivity.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoginClick(MaterialButton login) {
        send();
    }

    private void send() {
        if (isLoading) {
            return;
        }
        String email = holdr.loginEmail.getText().toString();
        ExpertConnect.getInstance(this).setUserId(email);
        isLoading = true;
        AnimationUtils.fadeIn(holdr.loading);
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    private boolean isValid() {
        return !TextUtils.isEmpty(holdr.loginEmail.getText());
    }

    @Override
    public Loader<ApiResult<Form>> onCreateLoader(int id, Bundle args) {
        return api.getForm("userprofile");
    }

    @Override
    public void onLoadFinished(Loader<ApiResult<Form>> loader, ApiResult<Form> data) {
        isLoading = false;
        if (holdr != null) {
            AnimationUtils.fadeOut(holdr.loading);
        }

        try {
            Form form = data.get();
            for (FormItem item : form.getFormData()) {
                if ("profile.fullname".equals(item.getMetadata())) {
                    String userName = ((TextFormItem) item).getValue();
                    ExpertConnect.getInstance(this).setUserName(userName);
                    break;
                }
            }
            setResult(RESULT_OK);
            finish();
        } catch (ApiException e) {
            IdentityManager.getInstance(this).setUserId(null);

            // 500 errors mean the user's login doesn't exist, because reasons.
            if (e.getStatus() == 500) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Unknown user, please register to create an account.")
                        .setPositiveButton(R.string.expertconnect_ok, null)
                        .show();
            } else {
                Toast.makeText(this, e.getUserMessage(getResources()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<ApiResult<Form>> loader) {

    }

    private class ValidatorWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            holdr.login.setEnabled(isValid());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}