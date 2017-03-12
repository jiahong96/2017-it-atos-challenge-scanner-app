package com.example.user.preconsumerapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static com.example.user.preconsumerapp.MainActivity.ConnectionAlert;
import static com.example.user.preconsumerapp.MainActivity.PostConnectionAlert;

public class Transaction extends AppCompatActivity {
    //Connectivity checking
    ConnectivityManager connectivityManager;
    NetworkInfo activeNetworkInfo;

    //string for nxt link
    String link1, link2, link3, link4;

    //string for QR scanning result
    String batchID,movement,productName;

    int errorCounter;

    JSONObject toPost1 = null;
    JSONObject toPost2 = null;
    JSONObject toPost3 = null;
    JSONObject toPost4 = null;
    JsonObjectRequest postRequest;
    JSONObject responseData = null;
    RequestQueue queue;
    Button btnPost;
    TextView tvProduct,tvBatch, tvMovement;

    //nxt url parts
    String nxtAccNum ="NXT-2N9Y-MQ6D-WAAS-G88VH";
    String secretPhrase = "appear morning crap became fire liquid probably tease rare swear shut grief";
    private static final String nxtPostLinkPart1 = "http://174.140.168.136:6876/nxt?requestType=sendMessage&secretPhrase=";
    private static final String nxtPostLinkPart2 = "&recipient=";
    private static final String nxtPostLinkPart3 = "&message=";
    private static final String nxtPostLinkPart4 = "&deadline=60&feeNQT=0";

    //Local Server IP
    private static final String getInfoUrl = "http://192.168.0.11:7080/";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected;

    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        queue= Volley.newRequestQueue(this);
        pDialog = new ProgressDialog(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
        builder.setMessage("Connect to WIFI and try again")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        PostConnectionAlert = builder.create();
        PostConnectionAlert.setCanceledOnTouchOutside(false);

        AlertDialog.Builder builder2 = new AlertDialog.Builder(Transaction.this);
        builder2.setMessage("Wifi Connection Lost")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        ConnectionAlert = builder2.create();
        ConnectionAlert.setCanceledOnTouchOutside(false);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        nxtAccNum = intent.getStringExtra("nxtAccNum");
        productName = intent.getStringExtra("productName");
        batchID = intent.getStringExtra("batchID");
        movement = intent.getStringExtra("movement");

        tvProduct = (TextView)findViewById(R.id.productName);
        tvBatch = (TextView)findViewById(R.id.batchID);
        tvMovement = (TextView)findViewById(R.id.movement);

        tvProduct.setText("Product: "+productName);
        tvBatch.setText("BatchID: "+batchID);
        tvMovement.setText("Movement: "+movement);

        btnPost = (Button) findViewById(R.id.button);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                secretPhrase = secretPhrase.replaceAll(" ","%20");
                updateConnectedFlags();

                if(wifiConnected==false){
                    PostConnectionAlert.show();
                }else{
                    getLocalInfoFromServer();
                }

            }
        });
    }

    public void firstPost(String urlString) {
        pDialog.setMessage("Sending to blockchain...");

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());
                            secondPost(link2);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        if(!isNetworkAvailable()){
                            ConnectionAlert.show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    public void secondPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());
                            thirdPost(link3);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        if(!isNetworkAvailable()){
                            ConnectionAlert.show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        queue.add(postRequest);
    }

    public void thirdPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());
                            fourthPost(link4);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        if(!isNetworkAvailable()){
                            ConnectionAlert.show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    public void fourthPost(String urlString) {

        postRequest = new JsonObjectRequest(Request.Method.POST, urlString, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        try {
                            pDialog.dismiss();
                            Log.d("Response", response.toString(4));
                            Log.d("response", response.toString());

                            AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                            builder.setMessage("Succcessfully sent to blockchain")
                                    .setCancelable(false)
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Intent intent = new Intent(Transaction.this, MainActivity.class);
                                                startActivity(intent);
                                            }
                                        });
                            AlertDialog alert = builder.create();
                            alert.setCanceledOnTouchOutside(false);
                            alert.show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        if(!isNetworkAvailable()){
                            ConnectionAlert.show();
                        }else{
                            Toast.makeText(getApplicationContext(), "Server error occured, please try again", Toast.LENGTH_LONG).show();
                        }
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    public void getLocalInfoFromServer(){
        queue = Volley.newRequestQueue(getApplicationContext());

        // Showing progress dialog before making http request
        pDialog.setMessage("Getting response from server...");
        //pDialog.setCanceledOnTouchOutside(false);
        pDialog.setCancelable(false);
        pDialog.show();

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, getInfoUrl, (String) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //get json Object
                    responseData = response;
                    Log.d("Response: ", responseData.toString());

                    //assign data to post objects
                    toPost1 = new JSONObject();
                    toPost2 = new JSONObject();
                    toPost3 = new JSONObject();
                    toPost4 = new JSONObject();

                    try {
                        //first post data
                        toPost1.put("batchID", batchID);
                        toPost1.put("movement", movement);
                        toPost1.put("unhashedData", responseData.getString("unhashedData"));
                        link1 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost1.toString()) + nxtPostLinkPart4;

                        //second post data
                        toPost2.put("batchID", batchID);
                        toPost2.put("encryptedHash1", responseData.getString("encryptedHash1"));
                        link2 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost2.toString()) + nxtPostLinkPart4;

                        //third post data
                        toPost3.put("batchID", batchID);
                        toPost3.put("encryptedHash2", responseData.getString("encryptedHash2"));
                        link3 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost3.toString()) + nxtPostLinkPart4;

                        //fourth post data
                        toPost4.put("batchID", batchID);
                        toPost4.put("encryptedHash3", responseData.getString("encryptedHash3"));
                        link4 = nxtPostLinkPart1 + secretPhrase + nxtPostLinkPart2 + nxtAccNum + nxtPostLinkPart3 +
                                URLEncoder.encode(toPost4.toString()) + nxtPostLinkPart4;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    firstPost(link1);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                    builder.setMessage("Site server connection error occured")
                            .setCancelable(false)
                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
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

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    public void updateConnectedFlags() {
        if (isNetworkAvailable()) {
            wifiConnected = true;
        } else {
            wifiConnected = false;
        }
    }

    private boolean isNetworkAvailable() {
        connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
