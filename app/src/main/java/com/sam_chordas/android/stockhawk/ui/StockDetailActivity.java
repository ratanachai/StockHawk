package com.sam_chordas.android.stockhawk.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
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

public class StockDetailActivity extends BaseActivity {
    public static final String GET_STOCK_DETAIL_ACTION = "com.sam_chordas.android.stockhawk.ui.GET_STOCK_DETAIL";
    private String[] mDate;
    private Float[] mAdjClose;
    private String mFromDate, mToDate;
    BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        Intent in = getIntent(); // Intent from MyStocksActivity class

        if(savedInstanceState == null
                || !savedInstanceState.containsKey("chart_label")
                || !savedInstanceState.containsKey("chart_data")){

            // Call intentService to gcmTaskService
            if(Utils.isNetworkAvailable(this)) {
                Intent intent = new Intent(this, StockIntentService.class);
                intent.putExtra("tag", "historical");
                intent.putExtra("symbol", in.getStringExtra("symbol"));
                intent.setAction(GET_STOCK_DETAIL_ACTION);
                startService(intent);
            }else {
                Utils.networkToast(this);
                showEmptyView(getString(R.string.data_might_outdated));
            }
        }else{
            mDate = savedInstanceState.getStringArray("chart_label");
            mAdjClose = ArrayUtils.toObject(savedInstanceState.getFloatArray("chart_data"));
            mFromDate = savedInstanceState.getString("chart_from_date");
            mToDate = savedInstanceState.getString("chart_to_date");
            drawChartShowData();
        }

        // Set TextViews in Today Section
        View listItem = findViewById(R.id.list_item_quote);
        TextView symbol = ((TextView)listItem.findViewById(R.id.stock_symbol));
        TextView bidPriceTv = (TextView)listItem.findViewById(R.id.bid_price);
        TextView change = (TextView)listItem.findViewById(R.id.change);
        symbol.setText(in.getStringExtra("symbol"));
        bidPriceTv.setText(in.getStringExtra("bid_price"));
        change.setText(in.getStringExtra("change"));
        String readLine = in.getStringExtra("symbol") + in.getStringExtra("bid_price") + ". " +
                in.getStringExtra("change"); //Put full-stop so TalkBack can read minus/plus in change
        listItem.setContentDescription(readLine);
        if (Float.parseFloat(in.getStringExtra("change").replace("%","")) < 0)
            change.setBackgroundDrawable(getResources().getDrawable(R.drawable.percent_change_pill_red));
        else
            change.setBackgroundDrawable(getResources().getDrawable(R.drawable.percent_change_pill_green));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mDate != null && mDate.length !=0)
            outState.putStringArray("chart_label", mDate);
        if(mAdjClose != null && mAdjClose.length !=0)
            outState.putFloatArray("chart_data", ArrayUtils.toPrimitive(mAdjClose));
        if(mToDate != null)
            outState.putString("chart_to_date", mToDate);
        if(mFromDate != null)
            outState.putString("chart_from_date", mFromDate);
        if(mEmptyViewText != null)
            outState.putString("empty_view_text", mEmptyViewText);
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
                // 3. Save up Date for Screen Reader (Talkback)
                // 4. Trim out label to just 5 label
                // 5. Draw Chart, show min/max/avg
                Bundle data = intent.getExtras();
                mDate = data.getStringArray("date");
                mAdjClose = Utils.StringToFloatArray(data.getStringArray("adj_close"));
                Collections.reverse(Arrays.asList(mDate));
                Collections.reverse(Arrays.asList(mAdjClose));
                mFromDate = new String(mDate[0]);
                mToDate = new String(mDate[mDate.length - 1]);
                Utils.trimArray(mDate, data.getInt("count")/4);
                drawChartShowData();
                hideEmptyView();

            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);

    }

    private void drawChartShowData() {
        // Get data ready
        String fromToLabel = " from " + mDate[0] + " to " + mDate[mDate.length - 1];
        float min = Collections.min(Arrays.asList(mAdjClose));
        float max = Collections.max(Arrays.asList(mAdjClose));
        float avg = Utils.avg(mAdjClose);

        // Show min/max/avg
        ((TextView)findViewById(R.id.chart_label)).append(fromToLabel);
        ((TextView)findViewById(R.id.min)).setText(String.format("%.2f", min));
        ((TextView)findViewById(R.id.max)).setText(String.format("%.2f", max));
        ((TextView)findViewById(R.id.avg)).setText(String.format("%.2f", avg));
        findViewById(R.id.min_max_wrapper).setVisibility(View.VISIBLE);

        // Draw a chart
        LineChartView lineChart = (LineChartView) findViewById(R.id.line_chart);
        LineSet dataset = new LineSet(mDate, ArrayUtils.toPrimitive(mAdjClose));
        dataset.setColor(Color.GREEN).setThickness(2.5f);
        lineChart.setAxisColor(Color.GRAY).setLabelsColor(Color.GRAY).setStep(Math.round(max/4.0f));
        lineChart.addData(dataset);
        lineChart.show();
        findViewById(R.id.chart_wrapper).setVisibility(View.VISIBLE);
        findViewById(R.id.chart_wrapper).setContentDescription(getString(R.string.desc_chart) +
                " from" + mFromDate + " to " + mToDate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }
}
