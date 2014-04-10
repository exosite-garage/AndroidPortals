package com.exosite.portals;

import android.support.v4.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;
import com.exosite.api.onep.RPC;
import com.exosite.api.onep.TimeSeriesPoint;
import com.exosite.api.portals.Portals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainActivity extends ActionBarActivity {
    /**
     * Exosite Portals plan identifier for new users.
     */
    public static final String PLAN_ID = "3676938388";
    /**
     * Vendor identifier for new devices.
     */
    public static final String VENDOR = "texasinstruments";
    /**
     * Device model identifier for new devices.
     */
    public static final String DEVICE_MODEL = "cc3101lpv1";
    /**
     * Domain for interacting with Portals API
     */
    public static final String PORTALS_DOMAIN = "ti.exosite.com";

    private static final String TAG = "MainActivity";
    // TI device CIK
    static String mCIK;
    static String mName;
    static String mEmail = "";
    // For a production app, this should be encrypted/obfuscated
    static String mPassword = "";

    DrawerHelper mDrawerHelper;

    SharedPreferences.OnSharedPreferenceChangeListener listener;

    private void updateFromSettings() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        mCIK = sharedPreferences.getString(SettingsActivity.KEY_PREF_DEVICE_CIK, "DEFAULT CIK");
        mName = sharedPreferences.getString(SettingsActivity.KEY_PREF_DEVICE_NAME, "");

        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager()
                .findFragmentByTag(PlaceholderFragment.FRAGMENT_TAG);
        if (fragment != null) {
            fragment.handleSettingsUpdate();
        }
    }

    protected boolean getLogin() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        mEmail = sharedPreferences.getString("email", null);
        mPassword = sharedPreferences.getString("password", null);
        boolean debug = false;
        if (mEmail == null || mPassword == null || debug) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    static void reportExoException(Exception e) {
        if (e != null) {
            Log.e(TAG, "Exception " + e.getMessage());
        } else {
            Log.e(TAG, "No result and no exception");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // configure the domain for Portals API calls
        Portals.setDomain(MainActivity.PORTALS_DOMAIN);

        if ( !getLogin() ) {
            return;
        }

        setContentView(R.layout.activity_main);

        mDrawerHelper = new DrawerHelper();
        mDrawerHelper.setup(this);

        if (savedInstanceState == null) {
            PlaceholderFragment frag = new PlaceholderFragment();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.add(R.id.container, frag, PlaceholderFragment.FRAGMENT_TAG).commit();
        } else if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        // set up preferences/settings
        updateFromSettings();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                Log.d(TAG, "called onSharedPreferenceChanged()");
                updateFromSettings();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        setTitle(mName);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }
    private void restoreInstanceState(Bundle savedInstanceState) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                int RESULT_SETTINGS = 1;
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends ListFragment {
        private static final String TAG = "PlaceholderFragment";
        public static final String FRAGMENT_TAG = "PLACEHOLDER_FRAGMENT";

        String lastToast;
        private PullToRefreshLayout mPullToRefreshLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            setRetainInstance(true);

            // Now find the PullToRefreshLayout to setup
            mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.ptr_layout);

            // Now setup the PullToRefreshLayout
            ActionBarPullToRefresh.from(getActivity())
                    // Mark All Children as pullable
                    .allChildrenArePullable()
                    // Set a OnRefreshListener
                    .listener(new OnRefreshListener() {
                        @Override
                        public void onRefreshStarted(View view) {
                            populateList();
                        }
                    })
                    // Finally commit the setup to our PullToRefreshLayout
                    .setup(mPullToRefreshLayout);

            // setup list
            populateList();

            return rootView;
        }

        void populateList() {

            try {
                final RPC rpc = new RPC();
                JSONArray types = new JSONArray();
                types.put("dataport");
                types.put("datarule");
                JSONObject infoOptions = new JSONObject();
                infoOptions.put("description", true);
                final PlaceholderFragment fragment = this;
                //Helper.showProgress(true, getActivity());
                mPullToRefreshLayout.setRefreshing(true);
                rpc.infoListingInBackground(mCIK, types, infoOptions, new ExoCallback<JSONObject>() {
                    @Override
                    public void done(JSONObject result, ExoException e) {
                        if (result != null) {
                            final ArrayList<String> resourceArray = new ArrayList<String>();
                            final ArrayList<String> rids = new ArrayList<String>();

                            try {
                                Iterator<String> typeIter = result.keys();
                                while (typeIter.hasNext()) {
                                    String type = typeIter.next();
                                    JSONObject resources = result.getJSONObject(type);
                                    Iterator<String> resIter = resources.keys();
                                    while (resIter.hasNext()) {
                                        String rid = resIter.next();
                                        rids.add(rid);
                                        JSONObject info = resources.getJSONObject(rid);
                                        resourceArray.add(info.getJSONObject("description").getString("name"));
                                    }
                                }
                            } catch (JSONException je) {
                                Log.e(TAG, "Exception while getting resource info: " + je.toString());
                            }
                            rpc.readLatestInBackground(mCIK, rids, new ExoCallback<HashMap<String, TimeSeriesPoint>>() {
                                @Override
                                public void done(HashMap<String, TimeSeriesPoint> result, ExoException e) {
                                    mPullToRefreshLayout.setRefreshing(false);
                                    if (result != null) {
                                        //Helper.showProgress(false, getActivity());

                                        final ArrayList<String> valuesArray = new ArrayList<String>();
                                        for (String rid: rids) {
                                            TimeSeriesPoint pt = result.get(rid);
                                            valuesArray.add(pt == null ? "No value" : pt.getValue().toString());
                                        }
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                                fragment.getActivity(),
                                                android.R.layout.two_line_list_item,
                                                android.R.id.text1,
                                                valuesArray) {
                                            @Override
                                            public View getView(int position, View convertView, ViewGroup parent) {
                                                View view = super.getView(position, convertView, parent);
                                                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                                                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                                                text1.setText(resourceArray.get(position));
                                                text2.setText(valuesArray.get(position));
                                                return view;
                                            }
                                        };
                                        setListAdapter(adapter);
                                    } else {
                                        reportExoException(e);
                                    }

                                }
                            });

                        } else {
                            mPullToRefreshLayout.setRefreshing(false);
                            reportExoException(e);
                        }
                    }
                });
            } catch (JSONException e) {
                Toast.makeText(this.getActivity(), "Failed to get device resources",
                        Toast.LENGTH_LONG).show();
            }
        }

        protected void handleSettingsUpdate() {
            View v = getView();
            if (v != null) {
                Context ctx = v.getContext();
            }
        }

    }

}
