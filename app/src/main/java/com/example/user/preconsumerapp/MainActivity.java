package com.example.user.preconsumerapp;

import android.Manifest;
import android.app.Activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    String batchID, productName;
    String nxtAccNum ="NXT-2N9Y-MQ6D-WAAS-G88VH";
    Spinner spinner;
    ImageView imgScan;

    public static AlertDialog ConnectionAlert=null;
    public static AlertDialog PostConnectionAlert=null;

    // Whether the display should be refreshed.
    public static boolean refreshDisplay;

    //Request external storage
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver receiver = new NetworkReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        receiver.setMainActivityHandler(this);
        this.registerReceiver(receiver, filter);

        verifyStoragePermissions(MainActivity.this);

        imgScan = (ImageView) findViewById(R.id.scanIcon);
        imgScan.setImageResource(R.drawable.scan);

        spinner = (Spinner) findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.action_array, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        imgScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.initiateScan(); // intent to open external qr app
            }
        });
    }

    //broadcast receiver on start
    @Override
    public void onStart () {
        super.onStart();
    }

    //unregister broadcast receiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregisters BroadcastReceiver when app is destroyed.
        if (receiver != null) {
            this.unregisterReceiver(receiver);
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
}
