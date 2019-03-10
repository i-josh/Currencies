package com.example.josh.currencies;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private EditText editTextUsed;
    private Spinner foreignSpinner, homeSpinner;
    private TextView resultTextView;
    private Button calcBtn, refreshButton;
    private String[] currencies;
    private Toolbar toolbar;

    public static final String FOR = "FOR_CURRENCY";
    public static final String HOM = "HOM_currency";

    private String mKey;
    public static final String RATES = "rates";
    public static final String URL_BASE = "http://openexchangerates.org/api/latest.json?app_id=";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00000");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);

        ArrayList<String> arrayList = ((ArrayList<String>) getIntent().getSerializableExtra(SplashActivity.KEY_ARRAYLIST));
        Collections.sort(arrayList);
        currencies = arrayList.toArray(new String[arrayList.size()]);

        resultTextView = findViewById(R.id.textViewResult);
        foreignSpinner = findViewById(R.id.spinner);
        homeSpinner = findViewById(R.id.spinner2);
        editTextUsed = findViewById(R.id.editText);
        editTextUsed.addTextChangedListener(new NumberTextWatcherForThousand(editTextUsed));
        calcBtn = findViewById(R.id.button);
        refreshButton = findViewById(R.id.btnRefresh);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CurrencyConverterTask task = new CurrencyConverterTask();
                if (task.execute(URL_BASE + mKey) != null){
                    Toast.makeText(getApplicationContext(),"Rates refreshed",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getApplicationContext(),"Refresh failed",Toast.LENGTH_SHORT).show();
                }
            }
        });

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_closed,currencies);

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        foreignSpinner.setAdapter(arrayAdapter);
        homeSpinner.setAdapter(arrayAdapter);

        foreignSpinner.setOnItemSelectedListener(this);
        homeSpinner.setOnItemSelectedListener(this);

        if (savedInstanceState == null && (Preferences.getString(this,FOR)==null && Preferences.getString(this,HOM)== null)){
            foreignSpinner.setSelection(findPositionGivenCode("USD",currencies));
            homeSpinner.setSelection(findPositionGivenCode("USD",currencies));

            Preferences.setString(this,FOR,"USD");
            Preferences.setString(this,HOM,"USD");
        } else{

            foreignSpinner.setSelection(findPositionGivenCode(Preferences.getString(this,FOR),currencies));
            homeSpinner.setSelection(findPositionGivenCode(Preferences.getString(this,HOM),currencies));
        }

        calcBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String getText = editTextUsed.getText().toString();
                String check = getText.replaceAll(",","");

                if (check.isEmpty() || !(check.matches("[0-9.]*"))){
                    Toast.makeText(getApplicationContext(),"Enter digits 0-9",Toast.LENGTH_SHORT).show();
                    resultTextView.setText("");
                } else {
                    new CurrencyConverterTask().execute(URL_BASE + mKey);
                }
            }
        });

        mKey = getKey("open_key");

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()){
            case R.id.spinner:
                Preferences.setString(this,FOR,extractCodeFromCurrency((String)foreignSpinner.getSelectedItem()));
                break;
            case R.id.spinner2:
                Preferences.setString(this,HOM,extractCodeFromCurrency((String)homeSpinner.getSelectedItem()));
                break;
            default:
                break;
        }
        resultTextView.setText("");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private int findPositionGivenCode(String code, String[] curr){

        for (int i = 0; i < curr.length; i++){
            if (extractCodeFromCurrency(curr[i]).equalsIgnoreCase(code)){
                return i;
            }
        }
        return 0;
    }

    private String extractCodeFromCurrency(String s) {
        return (s).substring(0,3);
    }

    private String getKey(String keyName){
        AssetManager assetManager = this.getResources().getAssets();
        Properties properties = new Properties();
        try {
            InputStream inputStream = assetManager.open("keys.properties");
            properties.load(inputStream);
        }catch (IOException e){
            e.printStackTrace();
        }
        return properties.getProperty(keyName);
    }

    private class  CurrencyConverterTask extends AsyncTask<String,Void,JSONObject>{
        private ProgressBar progressBar;

        @Override
        protected void onPreExecute() {
            progressBar = findViewById(R.id.progressBar2);
            calcBtn.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            return new JSONParser().getJSONFromUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            double dCalculated = 0.0;
            String strForCode = extractCodeFromCurrency(currencies[foreignSpinner
                    .getSelectedItemPosition()]);
            String strHomCode = extractCodeFromCurrency(currencies[homeSpinner
                    .getSelectedItemPosition()]);
            String inputValue = editTextUsed.getText().toString();
            String strAmount = inputValue.replaceAll(",","");

            try {
                if (jsonObject == null){
                    throw new JSONException("no data available");
                }
                JSONObject jsonRates = jsonObject.getJSONObject(RATES);
                if (strHomCode.equalsIgnoreCase("USD")){
                    dCalculated = Double.parseDouble(strAmount)/jsonRates.getDouble(strForCode);
                } else if (strForCode.equalsIgnoreCase("USD")){
                    dCalculated = Double.parseDouble(strAmount) * jsonRates.getDouble(strHomCode);
                } else {
                    dCalculated = Double.parseDouble(strAmount) * jsonRates.getDouble(strHomCode) /
                            jsonRates.getDouble(strForCode);
                }
            } catch (JSONException e){
                Toast.makeText(getApplicationContext(),"No connection " + e.getMessage(),Toast.LENGTH_SHORT).show();
                resultTextView.setText("");
                e.printStackTrace();
            } catch (Exception e){
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                resultTextView.setText("");
            }
            results(dCalculated, strHomCode);
            textViewColor();
            calcBtn.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }

        private void results(double dCalculated, String strHomCode) {
            switch (strHomCode){
                case "ALL":
                    resultTextView.setText("Lek" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "AFN":
                    resultTextView.setText("؋" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "ARS":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "AWG":
                    resultTextView.setText("ƒ" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "AUD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "AZN":
                    resultTextView.setText("₼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BSD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BBD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BYN":
                    resultTextView.setText("Br" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BZD":
                    resultTextView.setText("BZ$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BMD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BOB":
                    resultTextView.setText("$b" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BAM":
                    resultTextView.setText("KM" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BWP":
                    resultTextView.setText("P" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BGN":
                    resultTextView.setText("лв" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BRL":
                    resultTextView.setText("R$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "BND":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KHR":
                    resultTextView.setText("៛" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CAD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KYD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CLP":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CNY":
                    resultTextView.setText("¥" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "COP":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CRC":
                    resultTextView.setText("₡" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "HRK":
                    resultTextView.setText("kn" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CUP":
                    resultTextView.setText("₱" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CZK":
                    resultTextView.setText("Kč" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "DKK":
                    resultTextView.setText("kr" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "DOP":
                    resultTextView.setText("RD$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "XCD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "EGP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SVC":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "EUR":
                    resultTextView.setText("€" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "FKP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "FJD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GHS":
                    resultTextView.setText("¢" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GIP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GTQ":
                    resultTextView.setText("Q" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GGP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GYD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "HNL":
                    resultTextView.setText("L" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "HKD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "HUF":
                    resultTextView.setText("Ft" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "INR":
                    resultTextView.setText("₹" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "IDR":
                    resultTextView.setText("Rp" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "IRR":
                    resultTextView.setText("﷼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "IMP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "ILS":
                    resultTextView.setText("₪" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "JMD":
                    resultTextView.setText("J$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "JPY":
                    resultTextView.setText("¥" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "JEP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KZT":
                    resultTextView.setText("лв" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KPW":
                    resultTextView.setText("₩" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KRW":
                    resultTextView.setText("₩" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "KGS":
                    resultTextView.setText("лв" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "LAK":
                    resultTextView.setText("₭" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "LBP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "LRD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MKD":
                    resultTextView.setText("ден" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MYR":
                    resultTextView.setText("RM" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MUR":
                    resultTextView.setText("₨" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MXN":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MNT":
                    resultTextView.setText("₮" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "MZN":
                    resultTextView.setText("MT" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NAD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NPR":
                    resultTextView.setText("₨" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "ANG":
                    resultTextView.setText("ƒ" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NZD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NIO":
                    resultTextView.setText("C$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NGN":
                    resultTextView.setText("₦" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "NOK":
                    resultTextView.setText("kr" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "OMR":
                    resultTextView.setText("﷼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PKR":
                    resultTextView.setText("₨" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PAB":
                    resultTextView.setText("B/." + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PYG":
                    resultTextView.setText("Gs" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PEN":
                    resultTextView.setText("S/." + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PHP":
                    resultTextView.setText("₱" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "PLN":
                    resultTextView.setText("zł" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "QAR":
                    resultTextView.setText("﷼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "RON":
                    resultTextView.setText("lei" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "RUB":
                    resultTextView.setText("₽" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SHP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SAR":
                    resultTextView.setText("﷼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "RSD":
                    resultTextView.setText("Дин." + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SCR":
                    resultTextView.setText("₨" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SGD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SBD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SOS":
                    resultTextView.setText("S" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "ZAR":
                    resultTextView.setText("R" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "LKR":
                    resultTextView.setText("₨" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SEK":
                    resultTextView.setText("kr" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "CHF":
                    resultTextView.setText("CHF" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SRD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "SYP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "TWD":
                    resultTextView.setText("NT$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "THB":
                    resultTextView.setText("฿" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "TTD":
                    resultTextView.setText("TT$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "TRY":
                    resultTextView.setText("₺" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "TVD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "UAH":
                    resultTextView.setText("₴" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "GBP":
                    resultTextView.setText("£" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "USD":
                    resultTextView.setText("$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "UYU":
                    resultTextView.setText("$U" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "UZS":
                    resultTextView.setText("лв" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "VEF":
                    resultTextView.setText("Bs" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "VND":
                    resultTextView.setText("₫" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "YER":
                    resultTextView.setText("﷼" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                case "ZWL":
                    resultTextView.setText("Z$" + DECIMAL_FORMAT.format(dCalculated));
                    break;
                default:
                    resultTextView.setText(DECIMAL_FORMAT.format(dCalculated) + " " + strHomCode);
                    break;
            }
        }
    }

    private void textViewColor(){

        String txt = resultTextView.getText().toString();
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i< txt.length(); i++){
            if (!(txt.charAt(i) >= 48 && txt.charAt(i) <= 57)){
                String s = txt.charAt(i) + "";
                SpannableString spannableString = new SpannableString(s);
                spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.dark_green)),0,s.length(),0);
                builder.append(spannableString);
            }
            if (txt.charAt(i) >= 48 && txt.charAt(i) <= 57){
                String s = txt.charAt(i) + "";
                SpannableString spannableString = new SpannableString(s);
                spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_black)),0,s.length(),0);
                builder.append(spannableString);
            }
        }
        resultTextView.setText(builder,TextView.BufferType.SPANNABLE);

    }
}