package com.sam_chordas.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by keng on 9/06/16.
 */
public class ResponseReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = ResponseReceiver.class.getSimpleName() ;


    @Override public void onReceive(Context context, Intent intent) {

        // Receive local broadcast from IntentService, to display toast
        if(intent.getExtras().getBoolean(StockIntentService.INVALID_STOCK_SYMBOL)){
            Toast.makeText(context, R.string.invalid_stock_no_added, Toast.LENGTH_SHORT).show();
        } else if(intent.getExtras().getBoolean(StockIntentService.TEST)){
            Toast.makeText(context, "TEST", Toast.LENGTH_SHORT).show();
        }

    }
}
