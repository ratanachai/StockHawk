package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.db.chart.model.BarSet;
import com.db.chart.view.BarChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

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

        // Call intentService to gcmTaskService
        Intent intent = new Intent(this, StockIntentService.class);
        intent.putExtra("tag", "historical");
        intent.putExtra("symbol", getIntent().getStringExtra("symbol"));
        intent.setAction(GET_STOCK_DETAIL_ACTION);
        startService(intent);

        // Draw a chart
        BarChartView barChart = (BarChartView) findViewById(R.id.bar_chart);
        BarSet dataset = new BarSet(mLabels, mValues);
        dataset.setColor(Color.GREEN);
        barChart.setAxisColor(Color.GRAY);
        barChart.setLabelsColor(Color.GRAY);
        barChart.addData(dataset);
        barChart.show();
    }
}
