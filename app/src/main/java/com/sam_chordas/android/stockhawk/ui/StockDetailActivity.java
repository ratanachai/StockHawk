package com.sam_chordas.android.stockhawk.ui;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import java.util.Arrays;
import java.util.Collections;

public class StockDetailActivity extends Activity {
    public static final String GET_STOCK_DETAIL_ACTION = "com.sam_chordas.android.stockhawk.ui.GET_STOCK_DETAIL";
    private String[] mDate;
    private float[] mAdjClose;
    BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        // Call intentService to gcmTaskService
        Intent intent = new Intent(this, StockIntentService.class);
        intent.putExtra("tag", "historical");
        intent.putExtra("symbol", getIntent().getStringExtra("symbol"));
        intent.setAction(GET_STOCK_DETAIL_ACTION);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register receiver for local intent of response, to get Response from GcmService
        IntentFilter intentFilter = new IntentFilter(StockTaskService.ACTION_SHOW_HISTORICAL);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {

                Bundle data = intent.getExtras();
                mDate = data.getStringArray("date");
                mAdjClose = Utils.StringToFloatArray(data.getStringArray("adj_close"));
                Collections.reverse(Arrays.asList(mDate));
                Collections.reverse(Arrays.asList(mAdjClose));
                Utils.trimArray(mDate, data.getInt("count")/4);
//                Log.v("Date x Data", Integer.toString(mDate.length) + " x " + Integer.toString(mAdjClose.length));

                // Draw a chart
                LineChartView lineChart = (LineChartView) findViewById(R.id.line_chart);
                LineSet dataset = new LineSet(mDate, mAdjClose);
                dataset.setColor(Color.GREEN);
                lineChart.setAxisColor(Color.GRAY).setLabelsColor(Color.GRAY).setStep(50);
                lineChart.addData(dataset);
                lineChart.show();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }
}
