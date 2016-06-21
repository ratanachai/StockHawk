package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.ResponseReceiver;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

public class StockDetailActivity extends Activity {
    public static final String GET_STOCK_DETAIL_ACTION = "com.sam_chordas.android.stockhawk.ui.GET_STOCK_DETAIL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        // Register receiver for local intent of TEST
        IntentFilter intentFilter = new IntentFilter(StockIntentService.BROADCAST_ACTION);
        ResponseReceiver responseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);

        Intent intent = new Intent(this, StockIntentService.class);
        intent.setAction(GET_STOCK_DETAIL_ACTION);
        startService(intent);
    }
}
