package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.api.IdentityManager;

public class LandingActivity extends AppCompatActivity {

    private static final int REQUEST_LOG_IN = 0;
    private static final int REQUEST_REGISTER = 1;

    public static Intent newIntent(Context context) {
        return new Intent(context, LandingActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ExpertConnect.getInstance(this).getUserId() != null) {
            startActivity(MainActivity.newIntent(this));
            finish();
        } else {
            setContentView(R.layout.activity_landing);
        }
    }

    public void onLogInClicked(View view) {
        startActivityForResult(LogInActivity.newIntent(this), REQUEST_LOG_IN);
    }

    public void onRegisterClicked(View view) {
        startActivityForResult(RegisterActivity.newIntent(this), REQUEST_REGISTER);
    }

    public void onSkipClicked(View view) {
        startActivity(MainActivity.newIntent(this));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            startActivity(MainActivity.newIntent(this));
            finish();
        }
    }
}