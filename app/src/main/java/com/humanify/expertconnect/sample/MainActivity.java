package com.humanify.expertconnect.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.humanify.expertconnect.ExpertConnect;
import com.humanify.expertconnect.activity.ChatActivity;
import com.humanify.expertconnect.api.IdentityManager;
import com.humanify.expertconnect.api.model.action.Action;
import com.humanify.expertconnect.api.model.action.FormAction;
import com.humanify.expertconnect.api.model.action.WebAction;
import com.humanify.expertconnect.api.model.action.ChatAction;
import com.humanify.expertconnect.fragment.FormFragment;
import com.humanify.expertconnect.fragment.NavigationFragment;
import com.humanify.expertconnect.fragment.ProfileFragment;
import com.humanify.expertconnect.fragment.WebFragment;
import com.humanify.expertconnect.fragment.chat.ChatFragment;
import com.humanify.expertconnect.sample.api.model.AppAction;
import com.humanify.expertconnect.sample.api.model.ProfileAction;
import com.humanify.expertconnect.sample.holdr.Holdr_ActivityMain;
import com.humanify.expertconnect.sample.view.ScrimInsetsFrameLayout;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ScrimInsetsFrameLayout.OnInsetsCallback, FormFragment.FormListener {

    private Holdr_ActivityMain holdr;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private ViewGroup mContent;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private Action mCurrentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        holdr = new Holdr_ActivityMain(findViewById(android.R.id.content));
        setSupportActionBar(holdr.toolbar);
        holdr.insetLayout.setOnInsetsCallback(this);

        Action action = getIntent() == null ? null : getIntent().<Action>getParcelableExtra(ExpertConnect.EXTRA_ACTION);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mContent = (ViewGroup) findViewById(R.id.container);

        mTitle = getTitle();

        if (action != null && (action.getType().equals(Action.TYPE_CHAT_HISTORY) || action.getType().equals(Action.TYPE_ANSWER_HISTORY))) {
            mNavigationDrawerFragment.selectItem(new ProfileAction());
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    /**
     * Constructs a new intent to start this Activity.
     *
     * @param context an android context
     * @return the intent to start this Activity with
     */
    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, Action action) {
        if (mCurrentAction != null && mCurrentAction.equals(action)) {
            return; // We have already loaded the current fragment.
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (action.getType()) {
            case Action.TYPE_NAVIGATION:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, NavigationFragment.newInstance(action))
                        .commit();
                break;
            case Action.TYPE_WEB:
                String url = ((WebAction) action).getUrl();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, WebFragment.newInstance(url))
                        .commit();
                break;
            case Action.TYPE_FORM:
                FormAction formAction = (FormAction) action;
                FormFragment formFragment = FormFragment.newInstance(formAction);
                fragmentManager.beginTransaction()
                        .replace(R.id.container, formFragment)
                        .commit();
                break;
            case AppAction.TYPE_PROFILE:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, ProfileFragment.newInstance())
                        .commit();
                break;
            case AppAction.TYPE_SETTINGS:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, SettingsFragment.newInstance())
                        .commit();
                break;
            case AppAction.TYPE_CHAT:
                //This code gets the actual device ID, the SDK returns a random GUID.
                //String uuid = Settings.Secure.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                //uuid = uuid == null ? "[NOT_SPECIFIED]" : uuid;
                IdentityManager idMgr = IdentityManager.getInstance(this);
                String uid = idMgr.getUserName() == null ? idMgr.getDeviceId() : idMgr.getUserId();
                idMgr.setUserId(uid);
                ChatAction chatAction = (ChatAction) action;
                String xyz = chatAction.getAgentSkill();

                //startActivity(ChatActivity.newIntent(this.getApplication().getApplicationContext(), chatAction));
                //finish();

                fragmentManager.beginTransaction()
                        .replace(R.id.container, ChatFragment.newInstance(chatAction))
                        .commit();

                break;
            default:
                break;
        }

        mCurrentAction = action;
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Don't show the debug stuff on the web fragment because it already has enough
        // actionbar stuff.
        boolean result = false;
        if (!(mCurrentAction instanceof WebAction)) {
            result = super.onCreateOptionsMenu(menu);
        }

        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            restoreActionBar();
            return true;
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public Toolbar getToolbar() {
        return holdr.toolbar;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof WebFragment && ((WebFragment) fragment).handleBack()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onInsetsChanged(Rect insets) {
        Toolbar toolbar = holdr.toolbar;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        lp.topMargin = insets.top;
        int top = insets.top;
        insets.top += holdr.toolbar.getHeight();
        toolbar.setLayoutParams(lp);
        mNavigationDrawerFragment.setInsets(insets);
        insets.top = top; // revert

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContent.getLayoutParams();
        params.bottomMargin = insets.bottom;
        mContent.setLayoutParams(params);
    }

    @Override
    public void onFormPageChanged(int currentIndex, int pageCount) {

    }

    @Override
    public void onFormSubmitted() {

    }

    @Override
    public void onFormFinished() {
        mNavigationDrawerFragment.selectItem(0);
    }
}
