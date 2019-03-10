package com.example.josh.currencies;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    private static SharedPreferences sharedPreferences;

    public static void setString(Context context, String locale, String code){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(locale,code);
        editor.commit();
    }

    public static String getString(Context context,String locale){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(locale,null);
    }
}
