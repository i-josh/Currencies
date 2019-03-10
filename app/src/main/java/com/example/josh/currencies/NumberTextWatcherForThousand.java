package com.example.josh.currencies;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;

public class NumberTextWatcherForThousand implements TextWatcher {
    EditText editText;

    public NumberTextWatcherForThousand(EditText editText){
        this.editText = editText;
    }
    @Override
    public void beforeTextChanged(CharSequence s ,int start, int count, int after){}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count){}

    @Override
    public void afterTextChanged(Editable s){
        editText.removeTextChangedListener(this);
        try{
            String originalString = s.toString();

            Long longVal;
            if (originalString.contains(",")){
                originalString = originalString.replaceAll(",","");
            }
            longVal = Long.parseLong(originalString);

            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            formatter.applyPattern("#,###,###,###");
            String formattedString = formatter.format(longVal);

            editText.setText(formattedString);
            editText.setSelection(editText.getText().length());
        } catch (NumberFormatException nfe){
            nfe.printStackTrace();
        }
        editText.addTextChangedListener(this);
    }

}


