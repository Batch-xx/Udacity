package it.jaschke.alexandria.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by brian.batchelor on 11/27/2015.
 */
public  class Helper {

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context
                                                  .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork  = connectivityManager.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
