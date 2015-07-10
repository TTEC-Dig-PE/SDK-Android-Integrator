package com.humanify.expertconnect.sample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.humanify.expertconnect.api.model.action.Action;
import com.humanify.expertconnect.fragment.BaseExpertConnectFragment;
import com.humanify.expertconnect.sample.holdr.Holdr_FragmentNavigationDrawer;
import com.humanify.expertconnect.sample.holdr.Holdr_ItemNavigationDrawer;
import com.humanify.expertconnect.util.ActivityUtils;
import com.humanify.expertconnect.util.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer. See the <a
 * href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction"> design
 * guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends BaseExpertConnectFragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String STATE_NAV_ITEMS = "nav_items";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private View mFragmentContainerView;
    private Holdr_FragmentNavigationDrawer holdr;
    private ArrayList<Action> mNavItems;

    private Action mCurrentSelectedAction;
    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private BroadcastReceiver receiver;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mNavItems = savedInstanceState.getParcelableArrayList(STATE_NAV_ITEMS);

            mFromSavedInstanceState = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);

        if (mNavItems == null) {
            new NavigationLoadTask(getActivity()).execute();
        } else {
            holdr.list.setAdapter(new Adapter(mNavItems));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver = new NavigationLoadTask.NavigationReceiver() {
            @Override
            public void onSuccess(Context context, List<Action> items) {
                mNavItems = new ArrayList<>(items);
                AnimationUtils.fadeOut(holdr.loading);
                holdr.list.setAdapter(new Adapter(mNavItems));

                if (mCurrentSelectedAction != null) {
                    selectItem(mCurrentSelectedAction);
                } else {
                    selectItem(mCurrentSelectedPosition);
                }
            }

            @Override
            public void onError(Context context, Exception error) {
                Log.e("Humanify", error.getMessage(), error);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, new IntentFilter(NavigationLoadTask.BROADCAST));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        holdr = new Holdr_FragmentNavigationDrawer(wrapInflater(inflater).inflate(Holdr_FragmentNavigationDrawer.LAYOUT, container, false));
        holdr.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        holdr.list.setItemChecked(mCurrentSelectedPosition, true);

        if (savedInstanceState == null) {
            holdr.loading.setVisibility(View.VISIBLE);
        }

        return holdr.getView();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                getToolbar(),             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                ActivityUtils.hideKeyboard(getActivity());
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (holdr != null) {
            holdr.list.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (holdr != null && mCallbacks != null) {
            Action action = (Action) holdr.list.getAdapter().getItem(position);
            mCurrentSelectedAction = action;
            mCallbacks.onNavigationDrawerItemSelected(position, action);
        }
    }

    public void selectItem(Action action) {
        mCurrentSelectedAction = action;
        if (holdr != null && holdr.list.getAdapter() != null) {
            int position = ((Adapter) holdr.list.getAdapter()).items.indexOf(action);
            if (position != -1) {
                selectItem(position);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        outState.putParcelableArrayList(STATE_NAV_ITEMS, mNavItems);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setInsets(Rect insets) {
        holdr.getView().setPadding(insets.left, insets.top, insets.right, insets.bottom);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private Toolbar getToolbar() {
        return ((MainActivity) getActivity()).getToolbar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position, Action action);
    }

    private class Adapter extends BaseAdapter {
        private List<Action> items;

        Adapter(List<Action> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Action getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holdr_ItemNavigationDrawer holdr;
            if (convertView == null) {
                holdr = new Holdr_ItemNavigationDrawer(getLayoutInflater().inflate(Holdr_ItemNavigationDrawer.LAYOUT, parent, false));
                holdr.getView().setTag(holdr);
            } else {
                holdr = (Holdr_ItemNavigationDrawer) convertView.getTag();
            }

            Action action = getItem(position);

            holdr.text.setText(action.getDisplayName());

            if (TextUtils.isEmpty(action.getIcon())) {
                holdr.image.setVisibility(View.INVISIBLE);
            } else {
                holdr.image.setVisibility(View.VISIBLE);
                int drawableRes = getResources().getIdentifier(action.getIcon() , "drawable", getActivity().getPackageName());
                if (drawableRes != 0) {
                    holdr.image.setImageResource(drawableRes);
                } else {
                    holdr.image.setImageResource(R.drawable.nav_drawer_icon);
                }
            }

            return holdr.getView();
        }
    }
}
