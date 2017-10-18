package io.kristal.firebaseanalyticscobaltplugin;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.cobaltians.cobalt.Cobalt;
import org.cobaltians.cobalt.plugin.CobaltAbstractPlugin;
import org.cobaltians.cobalt.plugin.CobaltPluginWebContainer;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * <pre>
 * A plugin which allow Native & {@link android.webkit.WebView WebViews} contained in CobaltFragments to log events with parameters to Firebase Analytics.
 *
 * 1. Installation
 *  - Follow the steps of the <a href="https://firebase.google.com/docs/android/setup">Firebase documentation</a>, except adding the following line to the build.gradle file of your app
 *      {@code compile 'com.google.firebase:firebase-core:11.2.0'}
 *  - Import the Firebase Analytics plugin into your project
 *
 * 2. Debug
 *  - to enable, execute the following line in a terminal:
 *      {@code adb shell setprop debug.firebase.analytics.app <package_name>}
 *  - to disable, execute the following line in a terminal:
 *      {@code adb shell setprop debug.firebase.analytics.app .none.}
 * </pre>
 */
public final class FirebaseAnalyticsPlugin extends CobaltAbstractPlugin {

    private final static String TAG = FirebaseAnalyticsPlugin.class.getSimpleName();

    /***********************************************************************************************
     *
     * MEMBERS
     *
     **********************************************************************************************/

    private static FirebaseAnalyticsPlugin sInstance;
    private final FirebaseAnalytics mFirebaseAnalytics;

    /***********************************************************************************************
     *
     * CONSTRUCTORS
     *
     **********************************************************************************************/

    private FirebaseAnalyticsPlugin(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    /**
     * Returns an instance of @{FirebaseAnalyticsPlugin}
     * @param context a context
     * @return an instance of @{FirebaseAnalyticsPlugin}
     */
    public static FirebaseAnalyticsPlugin getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FirebaseAnalyticsPlugin(context);
        }

        return sInstance;
    }

    public static FirebaseAnalyticsPlugin getInstance(CobaltPluginWebContainer webContainer) {
        if (sInstance == null) {
            sInstance = new FirebaseAnalyticsPlugin(webContainer.getActivity());
        }

        return sInstance;
    }

    /***********************************************************************************************
     *
     * COBALT
     *
     **********************************************************************************************/

    @Override
    public void onMessage(CobaltPluginWebContainer webContainer, JSONObject message) {
        try {
            String action = message.getString(Cobalt.kJSAction);
            JSONObject data = message.getJSONObject(Cobalt.kJSData);

            if ("logEvent".equals(action)) {
                logEvent(data.getString("event"), toBundle(data.optJSONObject("params")));
            }
            else {
                throw new JSONException("Unsupported action");
            }
        }
        catch(JSONException exception) {
            Log.e(TAG, "onMessage: " + message.toString() + "\n"
                    + "Possible issues: \n"
                    + "\t- action is empty, not a string or not supported (supported actions: logEvent), \n"
                    + "\t- data object is empty or not an oject, \n"
                    + "\t- data.event is empty or not a string.");
            exception.printStackTrace();
        }
    }

    /***********************************************************************************************
     *
     * METHODS
     *
     **********************************************************************************************/

    /**
     * Logs an event with no parameters
     * @param event the event to log
     * @see #logEvent(String, Bundle)
     */
    public final void logEvent(@NonNull String event) {
        mFirebaseAnalytics.logEvent(event, null);
    }

    /**
     * Logs an event with parameters
     * @param event the event to log
     * @param parameters the parameters to pass alongside the event
     */
    public final void logEvent(@NonNull String event, @Nullable Bundle parameters) {
        mFirebaseAnalytics.logEvent(event, parameters);
    }

    /***********************************************************************************************
     *
     * HELPERS
     *
     **********************************************************************************************/

    private @Nullable Bundle toBundle(@Nullable JSONObject parameters) {
        Bundle bundle = new Bundle();
        if (parameters != null) {
            Iterator<String> keysIterator = parameters.keys();
            while(keysIterator.hasNext()) {
                String key = keysIterator.next();
                try {
                    Object value = parameters.get(key);
                    if (value != null) {
                        if (Long.class.isInstance(value)) {
                            bundle.putLong(key, (Long) value);
                        }
                        else if (Double.class.isInstance(value)) {
                            bundle.putDouble(key, (Double) value);
                        }
                        else if (String.class.isInstance(value)) {
                            bundle.putString(key, (String) value);
                        }
                    }
                }
                catch(JSONException exception) {
                    // Silent fail, could not happen
                }
            }
        }

        return bundle.isEmpty() ? null : bundle;
    }
}
