package com.example.josh.currencies;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.github.ybq.android.spinkit.style.ThreeBounce;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class SplashActivity extends Activity {

    public static final String URL_CODES = "http://openexchangerates.org/api/currencies.json";
    public static final String KEY_ARRAYLIST = "key_arraylist";
    private ArrayList<String> currencies;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        new FetchCodesTask().execute(URL_CODES);

    }

    private class FetchCodesTask extends AsyncTask<String,Void,JSONObject> {
        private ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            progressBar = findViewById(R.id.progressBar);
            Sprite doubleBounce = new ThreeBounce();
            progressBar.setIndeterminateDrawable(doubleBounce);

        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            return new JSONParser().getJSONFromUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            try{
                if (jsonObject == null){
                    throw new JSONException("no data available. ");
                }
                Iterator iterator = jsonObject.keys();
                String key;
                currencies = new ArrayList<>();
                while (iterator.hasNext()){
                    key = (String) iterator.next();
                    currencies.add(key + " | " + jsonObject.getString(key));
                }
                final Intent intent = new Intent(SplashActivity.this,MainActivity.class);
                intent.putExtra(KEY_ARRAYLIST, currencies);
                if (isConnectedToInternet()){
                    startActivity(intent);
                    finish();
                }else {
                    progressBar.setVisibility(View.INVISIBLE);
                    Snackbar.make(findViewById(R.id.splash),"No connection",Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            progressBar.setVisibility(View.VISIBLE);
                            if (isConnectedToInternet()){
                                startActivity(intent);
                                finish();
                            }
                        }
                    }).show();
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    private boolean isConnectedToInternet(){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } return false;
    }
}
