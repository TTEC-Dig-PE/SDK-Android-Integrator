package com.humanify.expertconnect.sample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.fragment.BaseExpertConnectFragment;
import com.humanify.expertconnect.sample.holdr.Holdr_FragmentSettings;
import com.humanify.expertconnect.view.compat.MaterialButton;
import com.urbanairship.UAirship;

import java.util.Arrays;

import de.psdev.licensesdialog.LicensesDialog;

public class SettingsFragment extends BaseExpertConnectFragment implements Holdr_FragmentSettings.Listener {

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private Holdr_FragmentSettings holdr;
    private View compatShadowActionTop;
    private ExpertConnect expertConnect;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return wrapInflater(inflater).inflate(Holdr_FragmentSettings.LAYOUT, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        holdr = new Holdr_FragmentSettings(view);
        holdr.setListener(this);

        expertConnect = ExpertConnect.getInstance(getActivity());

        compatShadowActionTop = view.findViewById(R.id.compat_shadow_action_top);
        holdr.pushNotificationsButton.setChecked(UAirship.shared().getPushManager().getUserNotificationsEnabled());

        if (expertConnect.getUserId() != null) {
            holdr.buttonContainer.setVisibility(View.VISIBLE);
            if (compatShadowActionTop != null) {
                compatShadowActionTop.setVisibility(View.VISIBLE);
            }
        }

        if (BuildConfig.DEBUG) {
            String agent = expertConnect.getDebugChatAgent();
            String endpoint = ((MainApplication) getActivity().getApplication()).getEndpoint();
            if (agent == null) agent = "Default";
            holdr.debugChatAgent.setText(getString(R.string.chat_agent, agent));
            holdr.debugEndpoint.setText(getString(R.string.endpoint, endpoint));
        }

        float elevationCard = getResources().getDimension(R.dimen.expertconnect_elevation_card);
        float elevationActions = getResources().getDimension(R.dimen.expertconnect_elevation_actions);
        ViewCompat.setElevation(holdr.pushNotificationsContainer, elevationCard);
        ViewCompat.setElevation(holdr.versionContainer, elevationCard);
        ViewCompat.setElevation(holdr.buttonContainer, elevationActions);
    }

    @Override
    public void onPushNotificationsButtonCheckedChanged(SwitchCompat pushNotificationsButton, boolean isChecked) {
        UAirship.shared().getPushManager().setUserNotificationsEnabled(isChecked);
    }

    @Override
    public void onOpenSourceButtonClick(TextView openSourceButton) {
//        Saguaro.showOpenSourceDialog(getActivity());
        new LicensesDialog.Builder(getActivity())
                .setNotices(R.raw.notices)
                .build().show();
    }

    @Override
    public void onLogOutClick(final MaterialButton logOut) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.log_out)
                .setMessage(R.string.log_out_confirmation)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logOut();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();

    }

    private void logOut() {
        expertConnect.setUserId(null);
        Toast.makeText(getActivity(), "You have been logged out.", Toast.LENGTH_SHORT).show();

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator hideButton = ObjectAnimator.ofFloat(holdr.buttonContainer, View.TRANSLATION_Y, 0, holdr.buttonContainer.getHeight());
        set.playTogether(hideButton);
        if (compatShadowActionTop != null) {
            ObjectAnimator hideShadow = ObjectAnimator.ofFloat(compatShadowActionTop, View.TRANSLATION_Y, 0, holdr.buttonContainer.getHeight());
            set.playTogether(hideShadow);
        }

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                holdr.buttonContainer.setVisibility(View.GONE);
                if (compatShadowActionTop != null) {
                    compatShadowActionTop.setVisibility(View.GONE);
                }
                startActivity(LandingActivity.newIntent(getActivity()));
                getActivity().finish();
            }
        });

        set.setInterpolator(new AccelerateInterpolator());
        set.setDuration(300);
        set.start();
    }

    @Override
    public void onDebugChatAgentClick(TextView view) {
        final String[] agents = getResources().getStringArray(R.array.chat_agent_options);
        final String currentAgent = expertConnect.getDebugChatAgent();
        int currentIndex = 0;
        if (currentAgent != null) {
            currentIndex = Arrays.asList(agents).indexOf(currentAgent);
        }

        new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(R.array.chat_agent_options, currentIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String agent = agents[which];
                        if (which == 0) {
                            expertConnect.setDebugChatAgent(null);
                        } else if (which == agents.length - 1) {
                            final EditText editText = new EditText(getActivity());
                            editText.setText(currentAgent);

                            new AlertDialog.Builder(getActivity())
                                    .setView(editText)
                                    .setPositiveButton(R.string.expertconnect_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String agent = editText.getText().toString();
                                            if (TextUtils.isEmpty(agent)) {
                                                expertConnect.setDebugChatAgent(null);
                                                holdr.debugChatAgent.setText(getString(R.string.chat_agent, "Default"));
                                            } else {
                                                expertConnect.setDebugChatAgent(agent);
                                                holdr.debugChatAgent.setText(getString(R.string.chat_agent, agent));
                                            }
                                        }
                                    })
                                    .show();

                        } else {
                            expertConnect.setDebugChatAgent(agent);
                        }
                        holdr.debugChatAgent.setText(getString(R.string.chat_agent, agent));
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onDebugEndpointClick(TextView debugEndpoint) {
        final MainApplication app = (MainApplication) getActivity().getApplication();
        final String[] endpoints = getResources().getStringArray(R.array.endpoint_options);
        final String currentEndpoint = app.getEndpoint();
        int currentIndex = endpoints.length - 1;
        if (currentEndpoint != null) {
            currentIndex = Arrays.asList(endpoints).indexOf(currentEndpoint);
        }

        new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(R.array.endpoint_options, currentIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String endpoint = endpoints[which];
                        if ("Custom".equals(endpoint)) {
                            endpoint = "http://";
                        }
                        if (which == 0) {
                            app.setEndpoint(MainApplication.ENDPOINT_DEV);
                        } else if (which == endpoints.length - 1) {
                            final EditText editText = new EditText(getActivity());
                            editText.setText(endpoint);

                            new AlertDialog.Builder(getActivity())
                                    .setView(editText)
                                    .setPositiveButton(R.string.expertconnect_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String endpoint = editText.getText().toString();
                                            if (TextUtils.isEmpty(endpoint)) {
                                                app.setEndpoint(MainApplication.ENDPOINT_DEV);
                                                holdr.debugEndpoint.setText(getString(R.string.endpoint, MainApplication.ENDPOINT_DEV));
                                            } else {
                                                app.setEndpoint(endpoint);
                                                holdr.debugEndpoint.setText(getString(R.string.endpoint, endpoint));
                                            }
                                        }
                                    })
                                    .show();

                        } else {
                            app.setEndpoint(endpoint);
                        }
                        holdr.debugEndpoint.setText(getString(R.string.endpoint, endpoint));
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
