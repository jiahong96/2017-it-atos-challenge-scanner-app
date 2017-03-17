package com.example.user.preconsumerapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import static com.example.user.preconsumerapp.MainActivity.PostConnectionAlert;

/**
 * Created by CheahHong on 3/4/2017.
 */

public class NetworkReceiver extends BroadcastReceiver {
    MainActivity main = null;
    void setMainActivityHandler(MainActivity main){
        this.main=main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // Userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
        if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            // If device has its Wi-Fi connection, close
            // the connection alert dialog
            if(PostConnectionAlert!=null){
                if(PostConnectionAlert.isShowing()){
                    PostConnectionAlert.dismiss();
                }
            }
            // Otherwise, the app can't download content--because the pref setting is WIFI, and there
            // is no Wi-Fi connection.
        }  else{
            Toast.makeText(main, R.string.lost_connection, Toast.LENGTH_SHORT).show();
        }
    }
}