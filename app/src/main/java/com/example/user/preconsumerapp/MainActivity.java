package com.example.user.preconsumerapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    RequestQueue queue;
    String batchID, productName;
    String nxtAccNum ="NXT-2N9Y-MQ6D-WAAS-G88VH";
    Spinner spinner;
    ConnectivityManager connectivityManager;
    NetworkInfo activeNetworkInfo;
    ImageView imgScan;
    int errorCounter;

    public static JSONObject responseData = null;
    public static AlertDialog ConnectionAlert;

    // Whether the display should be refreshed.
    public static boolean refreshDisplay;

    //Local Server IP
    private static final String getInfoUrl = "http://192.168.43.61:7080/";

    //Request external storage
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected;

    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check initial state of network connection
        updateConnectedFlags();

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);

        verifyStoragePermissions(MainActivity.this);

        pDialog = new ProgressDialog(this);

        imgScan = (ImageView) findViewById(R.id.scanIcon);
        imgScan.setImageResource(R.drawable.scan);

        spinner = (Spinner) findViewById(R.id.spinner);

        queue = Volley.newRequestQueue(getApplicationContext());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.action_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //if no response data from server, send Get Request
        if(responseData==null && wifiConnected == true) {
            errorCounter=0;
            getLocalInfoFromServer();
        }

        imgScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(responseData!=null){
                    IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                    integrator.initiateScan(); // intent to open external qr app
                }else{
                    errorCounter=0;
                    getLocalInfoFromServer();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregisters BroadcastReceiver when app is destroyed.
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onStart () {
        super.onStart();
        updateConnectedFlags();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect to site's wifi to proceed")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                });
        ConnectionAlert = builder.create();
        ConnectionAlert.setCanceledOnTouchOutside(false);

        if(refreshDisplay==false){
            ConnectionAlert.show();
        }else{
            //if no response data from server, send Get Request
            if(responseData==null) {
                errorCounter=0;
                getLocalInfoFromServer();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            Log.d("result", scanResult.toString());

            try {
                JSONObject qrData = new JSONObject(scanResult.getContents());

                if (qrData.has("nxtAccNum") && qrData.has("batchID") && qrData.has("productName")) {
                    Toast.makeText(getApplicationContext(), "Valid FoodChain™ QR detected", Toast.LENGTH_LONG).show();
                    nxtAccNum = qrData.getString("nxtAccNum");
                    batchID = qrData.getString("batchID");        // format of qr data
                    productName = qrData.getString("productName");
                    Intent intent = new Intent(this, Transaction.class);
                    intent.putExtra("jsonObjInString",responseData.toString());
                    intent.putExtra("nxtAccNum",nxtAccNum);
                    intent.putExtra("batchID",batchID);
                    intent.putExtra("productName",productName);
                    intent.putExtra("movement",spinner.getSelectedItem().toString().toLowerCase());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Not a Valid FoodChain™ QR , please try again", Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    public void updateConnectedFlags() {
        if (isNetworkAvailable()) {
            wifiConnected = true;
            refreshDisplay = true;
        } else {
            wifiConnected = false;
            refreshDisplay = false;
        }
    }

    private boolean isNetworkAvailable() {
        connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static void verifyStoragePermissions(Activity activity) { // for marshmallow permissions
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void getLocalInfoFromServer(){
        queue = Volley.newRequestQueue(getApplicationContext());

        // Showing progress dialog before making http request
        pDialog.setMessage("Getting response from server...");
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.setCancelable(false);
        pDialog.show();

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, getInfoUrl, (String) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                pDialog.dismiss();
                try {
                    //get json Object
                    responseData = response;
                    Log.d("Response: ", responseData.toString());
                    Toast.makeText(MainActivity.this, "Received response from server", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VolleyError: ", error.toString());
                if(errorCounter <=5){
                    getLocalInfoFromServer();
                    errorCounter ++;
                }else{
                    pDialog.dismiss();
                    Toast.makeText(MainActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Please check your server connection and restart app")
                            .setCancelable(false)
                            .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    MainActivity.this.finish();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.setCanceledOnTouchOutside(false);
                    alert.show();
                }

            }
        });

        queue.add(getRequest);
    }
}
