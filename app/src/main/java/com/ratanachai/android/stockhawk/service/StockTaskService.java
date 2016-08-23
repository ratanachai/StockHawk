package com.ratanachai.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.ratanachai.android.stockhawk.data.QuoteColumns;
import com.ratanachai.android.stockhawk.data.QuoteProvider;
import com.ratanachai.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;
  public static final String ACTION_DATA_EMPTY =
          "com.ratanachai.android.stockhawk.service.ACTION_DATA_EMPTY";
  public static final String ACTION_DATA_ADDED =
          "com.ratanachai.android.stockhawk.service.ACTION_DATA_ADDED";
  public static final String ACTION_DATA_UPDATED =
          "com.ratanachai.android.stockhawk.service.ACTION_DATA_UPDATED";
  public static final String ACTION_SHOW_HISTORICAL =
          "com.ratanachai.android.stockhawk.service.ACTION_SHOW_HISTORICAL";
  public static final String ACTION_INVALID_SYMBOL =
          "com.ratanachai.android.stockhawk.service.ACTION_INVALID_SYMBOL";

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }

  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null) { mContext = this; }
    StringBuilder urlStringBuilder = new StringBuilder();

    // Start building up Query String to Yahoo Finance
    // Case 1: select * from yahoo.finance.quotes where symbol in ("YHOO","AAPL","GOOG","MSFT")
    // Case 2: select * from yahoo.finance.historicaldata where startDate = "2009-09-11"
    // and endDate = "2010-03-10" and symbol = "YHOO"
    String query = params.getTag().equals("historical") ?
            "select * from yahoo.finance.historicaldata where startDate = \"?\" and " +
                    "endDate = \"?\" and symbol = " :
            "select * from yahoo.finance.quotes where symbol in (";

    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
    } catch (UnsupportedEncodingException e) {e.printStackTrace();}

    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null, null, null);


      // 0) Build Query for these preset Stock Symbol, if first_run and DB is empty
      if (params.getTag().equals("init") &&
              (initQueryCursor == null || initQueryCursor.getCount() == 0)) {

        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

      // 1) Early return if nothing in DB to update
      } else if (initQueryCursor == null || initQueryCursor.getCount() == 0) {

        // Nothing in DB, neither in RecycleView, so tell Activity to show empty_view
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_DATA_EMPTY));
        return GcmNetworkManager.RESULT_FAILURE;

      // 2) Build Query for update of existing Symbols in the DB
      } else if (initQueryCursor != null && initQueryCursor.getCount() != 0){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {e.printStackTrace();}
      }

    // 3) Build Query for new stock user want to add
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e) {e.printStackTrace();}

    // 4) Build Query for historical data for Stock Detail screen
    } else if (params.getTag().equals("historical")) {
      isUpdate = false;
      String stockInput = params.getExtras().getString("symbol");

      // Set Start and End date as Today and a year before
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -1); // Yesterday
      String endDate = dateFormat.format(cal.getTime());
      cal.add(Calendar.MONTH, -3); // A year before yesterday
      String startDate = dateFormat.format(cal.getTime());
      urlStringBuilder.replace(110, 113, startDate);
      urlStringBuilder.replace(143, 146, endDate);

      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\"", "UTF-8"));
      } catch (UnsupportedEncodingException e) {e.printStackTrace();}

    }
    // Finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    // Fetch the JSON from Yahoo using okhttp
    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      Log.v(LOG_TAG, urlString);

      // Fetch Data from Yahoo
      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;

        if (urlString.matches(".*historical.*")) {
          // Parse JSON into Array of 2xN
          String[][] historialData = Utils.historialJsonToArray(getResponse);

          // Send out the response to StockDetail Activity
          Intent localIntent = new Intent(ACTION_SHOW_HISTORICAL);
          Bundle bundle = new Bundle();
          bundle.putStringArray("date", historialData[0]);
          bundle.putStringArray("adj_close", historialData[1]);
          bundle.putInt("count", historialData[0].length);
          localIntent.putExtras(bundle);
          LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        } else if (Utils.isStockSymbolValid(getResponse)){
          try {
            ContentValues contentValues = new ContentValues();
            // update ISCURRENT to 0 (false) so new data is current
            if (isUpdate){
              contentValues.put(QuoteColumns.ISCURRENT, 0);
              mContext.getContentResolver().update(
                      QuoteProvider.Quotes.CONTENT_URI, contentValues, null, null);
            }
            mContext.getContentResolver().applyBatch(
                    QuoteProvider.AUTHORITY, Utils.quoteJsonToContentVals(getResponse));

            // Now there's something in DB, so tell Activity to show view, hide empty_view
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_DATA_ADDED));

          } catch (RemoteException | OperationApplicationException e){
            Log.e(LOG_TAG, "Error applying batch insert", e);
          }

        } else {
          // Stock Symbol Invalid: Sendout Local Broadcast to MyStocksActivity
          LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_INVALID_SYMBOL));
        }

      } catch (IOException e){ e.printStackTrace(); }
    }
    updateWidgets(mContext); //Update on every data fetch
    return result;
  }

  public static void updateWidgets(Context context) {
    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED).setPackage(context.getPackageName());
    context.sendBroadcast(dataUpdatedIntent);
  }
}
