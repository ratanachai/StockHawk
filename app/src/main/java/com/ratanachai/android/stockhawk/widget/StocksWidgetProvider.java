package com.ratanachai.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.support.v4.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.ratanachai.android.stockhawk.R;
import com.ratanachai.android.stockhawk.service.StockTaskService;
import com.ratanachai.android.stockhawk.ui.MyStocksActivity;
import com.ratanachai.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by keng on 30/05/16.
 */
public class StocksWidgetProvider extends AppWidgetProvider {
    private static final String LOG_TAG = StocksWidgetProvider.class.getSimpleName();
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_stocks);

            // Widget ID needed to determine Widget width in getViewAt
            Intent fillData = new Intent(context, StocksWidgetService.class);
            fillData.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            // Set up the RemoteView with collection data (ListView)
            views.setRemoteAdapter(R.id.widget_listview, fillData);
            views.setEmptyView(R.id.widget_listview, R.id.widget_empty);

            // Click on header to launch main activity
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_header, pendingIntent);

            // Click on list_item to launch detail activity
            Intent clickIntentTemplate = new Intent(context, StockDetailActivity.class);
            // Doing all these so that hitting back on DetailActivity will go back to MainActivity
            // Not going back to launcher app rightaway. (Something users probably expect).
            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_listview, clickPendingIntentTemplate);

            // a11y: Set ContentDescription
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                setRemoteContentDescription(context, views);

            // Set the RemoteView for the Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(Context context, RemoteViews views) {
        // The only item that need contentDescription right now
        views.setContentDescription(R.id.widget_header, context.getString(R.string.widget_header_action_desc));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        // Invalidate its old Collection data, so it can be repopulated
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_listview);
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
