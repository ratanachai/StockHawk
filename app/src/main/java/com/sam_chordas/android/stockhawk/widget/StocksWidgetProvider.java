package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by keng on 30/05/16.
 */
public class StocksWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int id : appWidgetIds) {
            int layoutId = R.layout.widget_stocks;
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
            appWidgetManager.updateAppWidget(id, views);
        }
    }
}
