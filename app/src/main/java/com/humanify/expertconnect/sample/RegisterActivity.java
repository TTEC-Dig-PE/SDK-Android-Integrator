package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.CharacterPickerDialog;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.ApiBroadcastReceiver;
import com.humanify.expertconnect.api.ApiException;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.ExpertConnectApiProxy;
import com.humanify.expertconnect.api.model.form.Form;
import com.humanify.expertconnect.api.model.form.FormItem;
import com.humanify.expertconnect.api.model.form.FormSubmitResponse;
import com.humanify.expertconnect.api.model.form.TextFormItem;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityRegister;
import com.humanify.expertconnect.util.AnimationUtils;
import com.humanify.expertconnect.util.ApiResult;
import com.humanify.expertconnect.view.LoadingView;
import com.humanify.expertconnect.view.compat.MaterialButton;
import com.humanify.expertconnect.view.form.FormView;
import com.humanify.expertconnect.view.form.TextInputFormView;

public class RegisterActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ApiResult<Form>>, Holdr_ActivityRegister.Listener {
    private static final String STATE_FORM = "form";

    private ExpertConnect expertConnect;
    private Holdr_ActivityRegister holdr;
    private ExpertConnectApiProxy api;
    private Form form;
    private ApiBroadcastReceiver<FormSubmitResponse> receiver;

    /**
     * Constructs a new intent to start this Activity.
     *
     * @param context an android context
     * @return the intent to start this Activity with
     */
    public static Intent newIntent(Context context) {
        return new Intent(context, RegisterActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(Holdr_ActivityRegister.LAYOUT);
        holdr = new Holdr_ActivityRegister(findViewById(android.R.id.content));
        holdr.setListener(this);
        expertConnect = ExpertConnect.getInstance(this);
        api = ExpertConnectApiProxy.getInstance(this);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null && (form = savedInstanceState.getParcelable(STATE_FORM)) != null) {
            holdr.loading.setState(LoadingView.STATE_NONE);
            loadForm(form);
        } else {
            holdr.loading.setState(LoadingView.STATE_PROGRESS);
            getSupportLoaderManager().initLoader(0, null, this);
        }

        holdr.loading.setRetryOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holdr.loading.animateToState(LoadingView.STATE_PROGRESS);
                getSupportLoaderManager().restartLoader(0, null, RegisterActivity.this);
            }
        });

        float elevationCard = getResources().getDimension(R.dimen.expertconnect_elevation_card);
        float elevationActions = getResources().getDimension(R.dimen.expertconnect_elevation_actions);

        ViewCompat.setElevation(holdr.answerCard, elevationCard);
        ViewCompat.setElevation(holdr.buttonContainer, elevationActions);
    }

    @Override
    protected void onStart() {
        super.onStart();
        api.registerPostForm(receiver = new ApiBroadcastReceiver<FormSubmitResponse>() {
            @Override
            public void onReceive(Context context, Intent intent) {
                holdr.loadingOverlay.setVisibility(View.INVISIBLE);
                super.onReceive(context, intent);
            }

            @Override
            public void onSuccess(Context context, FormSubmitResponse result) {
                if (result.isProfileUpdated()) {
                    expertConnect.setUserId(result.getIdentityToken());
                    for (FormItem item : form.getFormData()) {
                        if ("profile.fullname".equals(item.getMetadata())) {
                            expertConnect.setUserName(((TextFormItem) item).getValue());
                        } else if ("profile.email".equals(item.getMetadata())) {
                            expertConnect.setUserId(((TextFormItem) item).getValue());
                        }
                    }
                    RegisterActivity.this.setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(context, R.string.error_creating_profile, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Context context, ApiException error) {
                Toast.makeText(context, R.string.error_creating_profile, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        api.unregister(receiver);
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
    public void onRegisterClick(MaterialButton view) {
        AnimationUtils.fadeIn(holdr.loadingOverlay);
        api.postForm(null, form);
    }

    private void loadForm(final Form form) {
        FormView lastView = null;
        for (final FormItem question : form.getFormData()) {
            lastView = FormView.newInstance(this, question);
            lastView.setOnValidationChangedListener(new FormView.OnValidationChangedListener() {
                @Override
                public void onValidationChanged() {
                    holdr.register.setEnabled(isValid(form));
                }
            });
            holdr.answerCard.addView(lastView);
        }
        if (lastView != null && lastView instanceof TextInputFormView) {
            ((TextInputFormView) lastView).setOnActionListener(EditorInfo.IME_ACTION_SEND, new TextInputFormView.OnActionListener() {
                @Override
                public void onAction() {
                    if (isValid(form)) {
                        AnimationUtils.fadeIn(holdr.loadingOverlay);
                        api.postForm(null, form);
                    }
                }
            });
        }
        holdr.register.setEnabled(isValid(form));
        AnimationUtils.fadeIn(holdr.registrationContent);
    }

    private boolean isValid(Form form) {
        for (FormItem question : form.getFormData()) {
            if (!question.validate().isValid) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Loader<ApiResult<Form>> onCreateLoader(int id, Bundle args) {
        return api.getForm("userprofile");
    }

    @Override
    public void onLoadFinished(Loader<ApiResult<Form>> loader, ApiResult<Form> data) {
        try {
            form = data.get();
            holdr.loading.animateToState(LoadingView.STATE_NONE);
            loadForm(form);
        } catch (ApiException e) {
            holdr.loading.setErrorMessage(e.getUserMessage(getResources()));
            holdr.loading.animateToState(LoadingView.STATE_ERROR);
        }
    }

    @Override
    public void onLoaderReset(Loader<ApiResult<Form>> loader) {

    }
}