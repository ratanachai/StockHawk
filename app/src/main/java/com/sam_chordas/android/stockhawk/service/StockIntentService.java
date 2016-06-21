package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {
  public static final String BROADCAST_ACTION = "com.sam_chordas.android.stockhawk.service.BROADCAST";
  public static final String INVALID_STOCK_SYMBOL = "com.sam_chordas.android.stockhawk.service.INVALID_STOCK_SYMBOL";
  public static final String TEST = "com.sam_chordas.android.stockhawk.service.TEST";

  public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

  @Override protected void onHandleIntent(Intent intent) {
    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");

    // Case of request multiple stock.
    if(intent.getAction().equals(MyStocksActivity.GET_STOCKS_INFO_ACTION)) {
      StockTaskService stockTaskService = new StockTaskService(this);

      // Put Symbol into bundle
      Bundle args = new Bundle();
      if (intent.getStringExtra("tag").equals("add"))
        args.putString("symbol", intent.getStringExtra("symbol"));

      // Call OnRunTask from the intent service to force it to run immediately instead of scheduling a task.
      int result = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));

      // Sendout Local Broadcast to display Toast at ResponseReceiver.java
      if (result == StockTaskService.INVALID_STOCK_SYMBOL) {
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra(INVALID_STOCK_SYMBOL, true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
      }

    // Case of request Stock Detail (history of price)
    } else if(intent.getAction().equals(StockDetailActivity.GET_STOCK_DETAIL_ACTION)) {
      Intent localIntent = new Intent(BROADCAST_ACTION).putExtra(TEST, true);
      LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
  }

}
