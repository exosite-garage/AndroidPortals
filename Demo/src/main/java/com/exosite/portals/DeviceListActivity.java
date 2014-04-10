package com.exosite.portals;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteCursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;
import com.exosite.api.onep.OneException;
import com.exosite.api.onep.RPC;
import com.exosite.api.portals.Portals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class DeviceListActivity extends ListActivity {
    private static final String TAG = "DeviceListActivity";

    DrawerHelper mDrawerHelper;
    PullToRefreshLayout mPullToRefreshLayout;

    ArrayList<JSONObject> mDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        mDrawerHelper = new DrawerHelper();
        mDrawerHelper.setup(this);

        // Now find the PullToRefreshLayout to setup
        mPullToRefreshLayout = (PullToRefreshLayout)findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(this)
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

        populateList();

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        JSONObject device = mDeviceList.get(position);

        try {
            String cik = device.getString("cik");
            String name = device.getString("name");

            // select a device to use in the Thermostat demo
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(this);
            sharedPreferences.edit().putString(SettingsActivity.KEY_PREF_DEVICE_CIK, cik).commit();
            sharedPreferences.edit().putString(SettingsActivity.KEY_PREF_DEVICE_NAME, name).commit();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (JSONException je) {
            Log.e(TAG, je.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_device) {
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateList() {
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");
        mPullToRefreshLayout.setRefreshing(true);
        // ... and list the user's portals in it
        Portals.listPortalsInBackground(email, password, new ExoCallback<JSONArray>() {
            @Override
            public void done(final JSONArray portalList, ExoException e) {
                if (portalList != null) {
                    sharedPreferences.edit().putString("portal_list", portalList.toString());
                    new LoadDevicesTask(getListView().getContext()).execute(portalList);
                } else {
                    Log.e(TAG, "failed to list portals");
                }
            }
        });
    }

    // Represents a task that loads information about devices
    // from OneP on a background thread.
    class LoadDevicesTask extends AsyncTask<JSONArray, Integer, ArrayList<JSONObject>> {
        private static final String TAG = "LoadDevicesTask";
        private Exception exception;

        Context mCtx;
        public LoadDevicesTask(Context ctx) {
            mCtx = ctx;
        }

        protected ArrayList<JSONObject> doInBackground(JSONArray... params) {
            // list of device information for display
            ArrayList<JSONObject> response = new ArrayList<JSONObject>();
            RPC rpc = new RPC();
            exception = null;

            try {
                JSONArray portalList = params[0];
                JSONObject infoOptions = new JSONObject();
                infoOptions.put("description", true);
                infoOptions.put("key", true);
                for (int i = 0; i < portalList.length(); i++) {
                    JSONObject portal = (JSONObject)portalList.get(i);
                    String portalCIK = portal.getString("key");
                    JSONArray types = new JSONArray();
                    types.put("client");
                    JSONObject infoListing = rpc.infoListing(portalCIK, types, infoOptions);

                    JSONObject clients = infoListing.getJSONObject("client");
                    Iterator<String> iter = clients.keys();
                    while (iter.hasNext()) {
                        String deviceRID = iter.next();
                        JSONObject deviceInfo = clients.getJSONObject(deviceRID);
                        String name = deviceInfo.getJSONObject("description").getString("name");

                        // information for display
                        JSONObject device = new JSONObject();
                        device.put("rid", deviceRID);
                        device.put("name", name);
                        device.put("cik", deviceInfo.getString("key"));
                        device.put("portal_name", portal.getString("name"));
                        device.put("portal_cik", portalCIK);
                        response.add(device);
                    }
                }

            } catch (JSONException e) {
                exception = e;
                Log.e(TAG, "JSONException in ReadPortals.doInBackground: " + e.toString());
                return null;
            } catch (OneException e) {
                exception = e;
                Log.e(TAG, "OneException: " + e.toString());
                return null;
            }
            return response;
        }

        // this is executed on UI thread when doInBackground
        // returns a result
        protected void onPostExecute(final ArrayList<JSONObject> deviceList) {
            mPullToRefreshLayout.setRefreshing(false);
            if (exception == null) {
                mDeviceList = deviceList;
                final ArrayList<String> valuesArray = new ArrayList<String>();
                final JSONArray devicesArray = new JSONArray();

                // TODO: why is values array necessary for ArrayAdapter?
                try {
                    for (int i = 0; i < deviceList.size(); i++) {
                        valuesArray.add(
                            deviceList.get(i)
                                .getString("name"));
                    }
                } catch (JSONException je) {
                    Log.e(TAG, je.toString());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        DeviceListActivity.this,
                        android.R.layout.two_line_list_item,
                        android.R.id.text1,
                        valuesArray) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                        try {
                            JSONObject device = deviceList.get(position);
                            text1.setText(device.getString("name"));
                            text2.setText(String.format("Portal: %s",
                                    device.getString("portal_name")));
                        } catch (JSONException e) {
                            Log.e(TAG, e.toString());
                        }
                        return view;
                    }
                };
                setListAdapter(adapter);
            } else {
                Toast.makeText(getApplicationContext(),
                        String.format("Error fetching devices: %s", exception.getMessage()), Toast.LENGTH_LONG).show();
                Log.e(TAG, exception.toString());
            }
        }
    }
}