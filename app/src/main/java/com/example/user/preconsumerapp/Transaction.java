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
import com.android.volley.toolbox.StringRequest;
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
    String batchID,productName,nxtAccNum;

    int errorCounter;

    JSONObject toPost1 = null;
    JSONObject toPost2 = null;
    JSONObject toPost3 = null;
    JSONObject toPost4 = null;
    JSONObject responseData = null;
    JsonObjectRequest postRequest;
    RequestQueue queue;

    Button btnPost;
    TextView tvProduct,tvBatch;

    //nxt url parts
   // String secretPhrase = "appear morning crap became fire liquid probably tease rare swear shut grief";
    String secretPhrase;

    private static final String nxtPostLinkPart1 = "http://174.140.168.136:6876/nxt?requestType=sendMessage&secretPhrase=";
    private static final String nxtPostLinkPart2 = "&recipient=";
    private static final String nxtPostLinkPart3 = "&message=";
    private static final String nxtPostLinkPart4 = "&deadline=60&feeNQT=0";

    //Local Server IP
    private static final String getInfoUrl = "http://192.168.0.5:7080/";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected;

    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        queue= Volley.newRequestQueue(this);
        pDialog = new ProgressDialog(this);

        tvProduct = (TextView)findViewById(R.id.productName);
        tvBatch = (TextView)findViewById(R.id.batchID);

        //initialize dialog 1 - connect to wifi for action
        AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
        builder.setMessage("Please connect to WIFI")
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

        //initialize dialog 2 - wifi connection lost during the process
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

        getSecretPhrase(nxtAccNum); // here

        tvProduct.setText(productName);
        tvBatch.setText("BatchID: "+batchID);

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

    //post first json object to blockchain (batchID, movement, unhashed data)
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
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        pDialog.dismiss();
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    //post second json object to blockchain (batchID, encryptedhashPart1)
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
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        pDialog.dismiss();
                    }
                }
        );
        queue.add(postRequest);
    }

    //post third json object to blockchain (batchID, encryptedhashPart2)
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
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        pDialog.dismiss();
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    //post third json object to blockchain (batchID, encryptedhashPart3)
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
                            builder.setMessage(R.string.successful_transaction)
                                    .setCancelable(false)
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Intent intent = new Intent(Transaction.this, MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                            Toast.makeText(Transaction.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                        pDialog.dismiss();
                        Log.d("Error.PostResponse", error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    //get response from local server
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

                if(errorCounter <=3){
                    getLocalInfoFromServer();
                    errorCounter ++;
                }else{
                    pDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(Transaction.this);
                    builder.setMessage(R.string.server_error)
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

    // Checks the network connection
    private boolean isNetworkAvailable() {
        connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private void getSecretPhrase(String nxtAcc){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "yourip/generate/getSecret.php?nxtAccountNumber="+nxtAcc,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try{
                            JSONObject res = new JSONObject(response);
                            if(res.has("secretPhrase")){
                                secretPhrase = res.getString("secretPhrase");
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"Error getting secret phrase, no such account",Toast.LENGTH_LONG).show();
                            }

                        }catch(JSONException e){
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Error getting secret phrase,please check your connection",Toast.LENGTH_LONG).show();
                Log.d("secret error", error.toString());
            }
        });
        queue.add(stringRequest);
    }
}
