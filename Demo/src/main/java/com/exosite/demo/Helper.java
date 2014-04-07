package com.exosite.portals;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;
import com.exosite.api.portals.Portals;

import org.json.JSONArray;


public class Helper {
    private static final String TAG = "Helper";

    static void selectDomainAndDoIntent(String domain, final Intent intent, final Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(activity);

        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");

        sharedPreferences.edit().putString("domain", domain).commit();
        Portals.setDomain(domain);

        // ... and list the user's portals in it
        Portals.listPortalsInBackground(email, password, new ExoCallback<JSONArray>() {
            @Override
            public void done(JSONArray result, ExoException e) {
                if (result != null) {
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(activity);
                    sharedPreferences.edit().putString("portal_list", result.toString()).commit();

                    activity.startActivity(intent);
                    activity.finish();
                } else {
                    Log.e(TAG, "failed to list portals");
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    static void showProgress(final boolean show, Activity activity) {
        final View formView = activity.findViewById(R.id.form);
        final View statusView = activity.findViewById(R.id.status);
        if (formView == null || statusView == null) {

            // TODO: try to look up these things in a fragment
            return;
        }
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);

            statusView.setVisibility(View.VISIBLE);
            statusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            statusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            formView.setVisibility(View.VISIBLE);
            formView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            formView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            statusView.setVisibility(show ? View.VISIBLE : View.GONE);
            formView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
