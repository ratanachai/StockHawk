package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by keng on 16/06/16, refer to
 * https://github.com/udacity/Advanced_Android_Development/compare/7.03_Choose_Your_Size...7.04_Integrating_the_Detail_Widget
 */
public class StocksWidgetService extends RemoteViewsService {
    public final String LOG_TAG = StocksWidgetService.class.getSimpleName();
    private static final String[] QUOTES_COLUMNS = {
        QuoteDatabase.QUOTES + "." + QuoteColumns._ID,
        QuoteColumns.SYMBOL,
        QuoteColumns.BIDPRICE,
        QuoteColumns.PERCENT_CHANGE,
        QuoteColumns.CHANGE,
        QuoteColumns.ISUP };
    static final int INDEX_QUOTES_ID = 0;
    static final int INDEX_QUOTES_SYMBOL = 1;
    static final int INDEX_QUOTES_BIDPRICE = 2;
    static final int INDEX_QUOTES_PERCENT_CHANGE = 3;
    static final int INDEX_QUOTES_CHANGE = 4;
    static final int INDEX_QUOTES_ISUP = 5;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null)
                    data.close();
                // This method is called by app that hosts the widget (Launcher)
                // But our ContentProvider is not exported so Launcher doesn't have access to the data
                // Thus we make the method calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        QUOTES_COLUMNS,
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position))
                    return null;
                // Set data into each views
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_stocks_listitem);
                views.setTextViewText(R.id.stock_symbol, data.getString(INDEX_QUOTES_SYMBOL));
                views.setTextViewText(R.id.bid_price, data.getString(INDEX_QUOTES_BIDPRICE));
                views.setTextViewText(R.id.change, data.getString(INDEX_QUOTES_PERCENT_CHANGE));
                // Set Color for Pill background
                if (data.getInt(INDEX_QUOTES_ISUP) == 1)
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                else
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_stocks_listitem);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_QUOTES_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
