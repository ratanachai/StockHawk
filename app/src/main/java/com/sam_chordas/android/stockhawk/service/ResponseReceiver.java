package com.sam_chordas.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by keng on 9/06/16.
 */
public class ResponseReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = ResponseReceiver.class.getSimpleName() ;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getExtras().getBoolean(StockIntentService.INVALID_STOCK_SYMBOL)){
            Toast.makeText(context, "Invalid stock symbol. No stock added.", Toast.LENGTH_SHORT).show();
        }

    }
}
