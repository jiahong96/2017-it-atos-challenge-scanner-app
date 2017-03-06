package com.example.user.preconsumerapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import static com.example.user.preconsumerapp.MainActivity.ConnectionAlert;
import static com.example.user.preconsumerapp.MainActivity.refreshDisplay;

/**
 * Created by CheahHong on 3/4/2017.
 */

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // Checks the user prefs and the network connection. Based on the result, decides whether
        // to refresh the display or keep the current display.
        // Userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
        if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            // If device has its Wi-Fi connection, sets refreshDisplay
            // to true. This causes the display to be refreshed when the user
            // returns to the app.
            refreshDisplay = true;
            if(ConnectionAlert.isShowing()){
                ConnectionAlert.dismiss();
            }
            Toast.makeText(context, R.string.wifi_connected, Toast.LENGTH_SHORT).show();

            // Otherwise, the app can't download content--because the pref setting is WIFI, and there
            // is no Wi-Fi connection.
            // Sets refreshDisplay to false.
        } else {
            refreshDisplay = false;
            Toast.makeText(context, R.string.lost_connection, Toast.LENGTH_SHORT).show();
        }
    }
}