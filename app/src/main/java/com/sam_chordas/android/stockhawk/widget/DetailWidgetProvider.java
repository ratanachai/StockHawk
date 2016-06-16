package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

/**
 * Created by keng on 30/05/16.
 */
public class DetailWidgetProvider extends AppWidgetProvider {
    private static final String LOG_TAG = DetailWidgetProvider.class.getSimpleName();
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int wid : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_detail);
            // Set up the collection
            views.setRemoteAdapter(R.id.widget_listview, new Intent(context, DetailWidgetService.class));
            appWidgetManager.updateAppWidget(wid, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(StockTaskService.ACTION_DATA_UPDATED)){
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] wids = manager.getAppWidgetIds(new ComponentName(context, getClass()));
            manager.notifyAppWidgetViewDataChanged(wids, R.id.widget_listview);
        }

    }
}
