package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sam_chordas.android.stockhawk.service.StockTaskService;

/**
 * Created by keng on 30/05/16.
 */
public class OneStockWidgetProvider extends AppWidgetProvider {
    private static final String LOG_TAG = OneStockWidgetProvider.class.getSimpleName();
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, OneStockWidgetIntentService.class));
        Log.v(LOG_TAG, "onUpdate()");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(StockTaskService.ACTION_DATA_UPDATED)) {
            context.startService(new Intent(context, OneStockWidgetIntentService.class));
            Log.v(LOG_TAG, "onReceive(ACTION_DATA_UPDATED)");
        }else{
            Log.v(LOG_TAG, "onReceive(SOMETHING ELSE)");
        }

    }
}
