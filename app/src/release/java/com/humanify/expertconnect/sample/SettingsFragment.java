package com.humanify.expertconnect.sample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.fragment.BaseExpertConnectFragment;
import com.humanify.expertconnect.sample.BuildConfig;
import com.humanify.expertconnect.sample.LandingActivity;
import com.humanify.expertconnect.sample.R;
import com.humanify.expertconnect.sample.holdr.Holdr_FragmentSettings;
import com.humanify.expertconnect.view.compat.MaterialButton;
import com.urbanairship.UAirship;
import com.willowtreeapps.saguaro.android.Saguaro;

import java.util.Arrays;

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
        Saguaro.showOpenSourceDialog(getActivity());
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
}
