package com.red61.android.nfcreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

public class NFCReader extends AppCompatActivity {

    private static final String TAG = NFCReader.class.getSimpleName();
    private NfcAdapter mAdapter;
    private String[][] techLists;
    private IntentFilter[] intentFilters;
    private PendingIntent pendingIntent;
    private TextView homeText;
    private URL returnUrl;
    private static final String PARAM_NAME = "returnUrl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcreader);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        // Handle all of our received NFC intents in this activity.
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
             You should specify only the ones that you need. */

        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFilters = new IntentFilter[2];
        intentFilters[0] = ndef;
        intentFilters[1] = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        techLists = new String[][]{new String[]{NfcF.class.getName(), MifareClassic.class.getName()}};


        Intent intent = getIntent();
        homeText = (TextView) findViewById(R.id.home_text);

        if (mAdapter.isEnabled()) {
            homeText.setText("NFC reader ready");
            if (intent.getAction() == Intent.ACTION_VIEW) {
                Uri data = intent.getData();
                Log.i(TAG,"raw data [" + data +"]");
                String rawString = data.getQueryParameter(PARAM_NAME);
                Log.i(TAG,"raw query param [" + rawString +"]");
                if (rawString == null) {
                    homeText.setText("Failed to find "+PARAM_NAME+" param in query");
                } else {
                    String param = URLDecoder.decode(rawString);
                    Log.i(TAG, "Param [" + param +"]");
                    if(!param.contains("?")) {
                        param += "?result=";
                    }
                    else {
                        param += "&result=";
                    }
                    try {
                        returnUrl = new URL(param);
                    }
                    catch(MalformedURLException e) {
                        Log.e(TAG,e.getMessage(),e);
                        homeText.setText("Failed to parse "+PARAM_NAME+" from ["+param+"] - " + e.getMessage());
                    }
                }
            } else {
                homeText.setText("Failed to find url");
            }
        } else {
            homeText.setText("NFC reader is not enabled");
        }
    }

    public void onNewIntent(Intent intent) {
        Log.i(TAG, "********** tag found **********");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String uid = bin2hex(tag.getId());

        Log.i(TAG, "UID: " + uid);
        homeText.setText("UID: " + uid);
        if(returnUrl != null) {
            String url = returnUrl + "ok&uid=" + uid;
            Log.i(TAG,"Load url : " + url);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            finish();
        }
    }


    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            try {
                mAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
            } catch (Exception e) {
                homeText.setText(e.getMessage());
            }
        }
    }

    //To display the UID
    static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
    }
}
