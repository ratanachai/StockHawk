package com.sam_chordas.android.stockhawk.ui;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;

public class StockDetailActivity extends Activity {
    public static final String GET_STOCK_DETAIL_ACTION = "com.sam_chordas.android.stockhawk.ui.GET_STOCK_DETAIL";
    private String[] mDate;
    private Float[] mAdjClose;
    BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        // Call intentService to gcmTaskService
        Intent in = getIntent(); // Intent from MyStocksActivity class
        Intent intent = new Intent(this, StockIntentService.class);
        intent.putExtra("tag", "historical");
        intent.putExtra("symbol", in.getStringExtra("symbol"));
        intent.setAction(GET_STOCK_DETAIL_ACTION);
        startService(intent);

        // Set TextViews in layout
        ((TextView)findViewById(R.id.stock_symbol)).setText(in.getStringExtra("symbol"));
        TextView bidPriceTv = (TextView)findViewById(R.id.bid_price);
        TextView change = (TextView)findViewById(R.id.change);
        bidPriceTv.setText(in.getStringExtra("bid_price"));
        change.setText(in.getStringExtra("change"));
        if (Float.parseFloat(in.getStringExtra("change").replace("%","")) < 0)
            change.setBackgroundDrawable(getResources().getDrawable(R.drawable.percent_change_pill_red));
        else
            change.setBackgroundDrawable(getResources().getDrawable(R.drawable.percent_change_pill_green));

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register receiver for local intent of response, to get Response from GcmService
        IntentFilter intentFilter = new IntentFilter(StockTaskService.ACTION_SHOW_HISTORICAL);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {

                // Get data ready for Chart
                // 1. Change data to array of float
                // 2. Reverse both label and data array
                // 3. Trim out label to just 5 label
                // 4. Get Min, Avg, Max
                Bundle data = intent.getExtras();
                mDate = data.getStringArray("date");
                mAdjClose = Utils.StringToFloatArray(data.getStringArray("adj_close"));
                Collections.reverse(Arrays.asList(mDate));
                Collections.reverse(Arrays.asList(mAdjClose));
                Utils.trimArray(mDate, data.getInt("count")/4);
                float min = Collections.min(Arrays.asList(mAdjClose));
                float max = Collections.max(Arrays.asList(mAdjClose));
                float avg = Utils.avg(mAdjClose);
                Log.v("min, avg, max", min +", " + avg + ", " + max);
//                Log.v("Date x Data", Integer.toString(mDate.length) + " x " + Integer.toString(mAdjClose.length));

                // Draw a chart
                LineChartView lineChart = (LineChartView) findViewById(R.id.line_chart);
                LineSet dataset = new LineSet(mDate, ArrayUtils.toPrimitive(mAdjClose));
                dataset.setColor(Color.GREEN).setThickness(2.5f);
                lineChart.setAxisColor(Color.GRAY).setLabelsColor(Color.GRAY).setStep(Math.round(max/4.0f));
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
