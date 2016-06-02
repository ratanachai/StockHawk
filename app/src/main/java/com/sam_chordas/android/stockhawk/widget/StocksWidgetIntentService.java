package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by keng on 1/06/16.
 */
public class StocksWidgetIntentService extends IntentService {
    private static String LOG_TAG = StocksWidgetIntentService.class.getSimpleName();

    public StocksWidgetIntentService() { super(StocksWidgetIntentService.class.getName()); }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager= AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                StocksWidgetProvider.class));

        for(int wid : appWidgetIds) {
            int layoutId = R.layout.widget_stocks;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Add the data to the RemoteViews
//            String description = "FB = -1000%";
            DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            String description = df.format(Calendar.getInstance().getTime());
            Log.v(LOG_TAG, description);
            views.setTextViewText(R.id.widget_textview, description);

            // Create and Intent to Launch MyStocksActivity
            Intent launchIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_stocks, pendingIntent);

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                setRemoteContentDescription(views, description);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(wid, views);
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_textview, description);
    }
}
