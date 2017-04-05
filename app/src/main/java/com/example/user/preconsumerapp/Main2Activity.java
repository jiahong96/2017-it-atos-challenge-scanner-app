package com.example.user.preconsumerapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

public class Main2Activity extends AppCompatActivity {
    String batchID, productName,nxtAccNum,nxtTransactionAccNum;
    int quantity;
    ImageView imgScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        nxtAccNum = intent.getStringExtra("nxtAccNum");
        productName = intent.getStringExtra("productName");
        batchID = intent.getStringExtra("batchID");
        quantity = intent.getIntExtra("Quantity",0);

        imgScan = (ImageView) findViewById(R.id.scanIcon);
        imgScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(Main2Activity.this);
                integrator.initiateScan(); // intent to open external qr app
            }
        });
    }

    //Scanner on result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            Log.d("result", scanResult.toString());

            try {
                JSONObject qrData = new JSONObject(scanResult.getContents());

                //standardized QR data format for nxt account
                if (qrData.has("nxtAccNum") && !qrData.has("batchID") && !qrData.has("productName")) {
                    nxtTransactionAccNum = qrData.getString("nxtAccNum");
                    Intent intent = new Intent(this, Transaction.class);
                    intent.putExtra("nxtAccNum",nxtAccNum);
                    intent.putExtra("nxtTransactionAccNum",nxtTransactionAccNum);
                    intent.putExtra("batchID",batchID);
                    intent.putExtra("productName",productName);
                    intent.putExtra("Quantity",quantity);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Not a Valid FoodChain™ Account QR , please try again", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
