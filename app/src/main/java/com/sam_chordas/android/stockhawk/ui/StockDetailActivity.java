package com.sam_chordas.android.stockhawk.ui;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

public class StockDetailActivity extends Activity {
    public static final String GET_STOCK_DETAIL_ACTION = "com.sam_chordas.android.stockhawk.ui.GET_STOCK_DETAIL";

    private final String[] mLabels= {"First", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "Last"};
    private final float[] mValues = {2.5f, 3.7f, 4f, 8f, 4.5f, 4f, 5f, 7f, 10f, 14f,
            12f, 6f, 7f, 8f, 9f, 8f, 9f, 8f, 7f, 6f,
            5f, 4f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 11f,
            12f, 14, 13f, 10f ,9f, 8f, 7f, 5f, 4f, 6f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        // Register receiver for local intent of response, to get Response from GcmService
        IntentFilter intentFilter = new IntentFilter(StockTaskService.ACTION_SHOW_HISTORICAL);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                TextView tv = (TextView)findViewById(R.id.response);
                tv.setText(intent.getStringExtra("json_response"));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        // Call intentService to gcmTaskService
        Intent intent = new Intent(this, StockIntentService.class);
        intent.putExtra("tag", "historical");
        intent.putExtra("symbol", getIntent().getStringExtra("symbol"));
        intent.setAction(GET_STOCK_DETAIL_ACTION);
        startService(intent);

        // Draw a chart
        LineChartView lineChart = (LineChartView) findViewById(R.id.line_chart);
        LineSet dataset = new LineSet(mLabels, mValues);
        dataset.setColor(Color.GREEN);
        lineChart.setAxisColor(Color.GRAY);
        lineChart.setLabelsColor(Color.GRAY);
        lineChart.addData(dataset);
        lineChart.show();
    }
}
