package com.ratanachai.android.stockhawk.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.ratanachai.android.stockhawk.R;
import com.ratanachai.android.stockhawk.data.QuoteColumns;
import com.ratanachai.android.stockhawk.data.QuoteProvider;
import com.ratanachai.android.stockhawk.rest.QuoteCursorAdapter;
import com.ratanachai.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.ratanachai.android.stockhawk.rest.Utils;
import com.ratanachai.android.stockhawk.service.StockIntentService;
import com.ratanachai.android.stockhawk.service.StockTaskService;
import com.ratanachai.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>{

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  BroadcastReceiver mBroadcastReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my_stocks);
    mContext = this;

    Boolean isConnected = Utils.isNetworkAvailable(this);

    // The intent service is for executing immediate pulls from the Yahoo API, because
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null) {

      // 1. "init" is to Fetch pre-defined stocks only if it is the first run
      SharedPreferences prefs = getSharedPreferences("com.ratanachai.android.stockhawk", MODE_PRIVATE);
      if (prefs.getBoolean("init", true)) {
        prefs.edit().putBoolean("init", false).apply();
        mServiceIntent.putExtra("tag", "init");

      // 2. "periodic" is for every onCreate() and timed schedule (in TaskService)
      }else {
        mServiceIntent.putExtra("tag", "periodic");
      }

      // Run it
      if (isConnected)
        startService(mServiceIntent);
      else
        Utils.networkToast(this);

    // RestoreSavedState
    } else {
      mEmptyViewText = savedInstanceState.getString("empty_view_text");
      if(mEmptyViewText != null)
        showEmptyView(mEmptyViewText);
    }

    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override
              public void onItemClick(View v, int position) {
                TextView tv = (TextView)v.findViewById(R.id.stock_symbol);
                Intent intent = new Intent(v.getContext(), StockDetailActivity.class);
                intent.putExtra("symbol", tv.getText());
                intent.putExtra("bid_price", ((TextView)v.findViewById(R.id.bid_price)).getText());
                intent.putExtra("change", ((TextView)v.findViewById(R.id.change)).getText());
                startActivity(intent);
              }
            })
    );
    recyclerView.setAdapter(mCursorAdapter);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (Utils.isNetworkAvailable((Activity) mContext)){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {

                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { input.toString().toUpperCase() }, null);

                  if (c.getCount() != 0) {
                    // Found stock in DB, so toast and do nothing
                    Toast.makeText(MyStocksActivity.this, getString(R.string.stock_already_saved),
                            Toast.LENGTH_LONG).show();
                    return;

                  } else if (input.toString().matches("^(\\s+|)$")) {
                    // Input Empty or String of white spaces, so toast and do nothing
                    Toast.makeText(MyStocksActivity.this, getString(R.string.no_stock_added_try_again),
                            Toast.LENGTH_SHORT).show();
                    return;

                  } else {
                    // Add the stock to DB
                    mServiceIntent.putExtra("tag", "add");
                    mServiceIntent.putExtra("symbol", input.toString().toUpperCase());
                    startService(mServiceIntent);
                  }
                }
              })
              .show();
        } else {
          Utils.networkToast(mContext);
        }
      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();

    // Create a periodic task to pull stocks once every hour after the app has been opened.
    // This is so Widget data stays up to date.
    if (isConnected){
      //long period = 3600L;
      long period = 60L;
      long flex = 10L;
      String periodicTag = "periodic";

      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);

    } else if (mEmptyViewText == null){
        showEmptyView(getString(R.string.data_might_outdated));
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mEmptyViewText != null)
      outState.putString("empty_view_text", mEmptyViewText);
  }

  @Override
  public void onResume() {
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    // Things to do when receive local broadcast in Receiver()
    mBroadcastReceiver = new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(StockTaskService.ACTION_INVALID_SYMBOL))
          Toast.makeText(context, R.string.invalid_stock_no_added, Toast.LENGTH_SHORT).show();

        else if(intent.getAction().equals(StockTaskService.ACTION_DATA_EMPTY)){
          showEmptyView(getString(R.string.please_enter_some_stock));

        }else if(intent.getAction().equals(StockTaskService.ACTION_DATA_ADDED)){
          hideEmptyView();
        }

      }
    };

    // Register receiver for local intent of INVALID_STOCK_SYMBOL and DATA_EMPTY
    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
    lbm.registerReceiver(mBroadcastReceiver, new IntentFilter(StockTaskService.ACTION_INVALID_SYMBOL));
    lbm.registerReceiver(mBroadcastReceiver, new IntentFilter(StockTaskService.ACTION_DATA_EMPTY));
    lbm.registerReceiver(mBroadcastReceiver, new IntentFilter(StockTaskService.ACTION_DATA_ADDED));

  }

  @Override
  protected void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
        QuoteColumns.ISCURRENT + " = ?",
        new String[]{"1"},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }

}
