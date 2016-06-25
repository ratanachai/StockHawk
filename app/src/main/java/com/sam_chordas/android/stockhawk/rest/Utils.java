package com.sam_chordas.android.stockhawk.rest;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {
  private static String LOG_TAG = Utils.class.getSimpleName();
  public static boolean showPercent = true;

  //Based on a http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
  public static boolean isNetworkAvailable(Activity activity) {
    ConnectivityManager connectivityManager
            = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
  }
  public static void networkToast(Context context) {
    Toast.makeText(context, context.getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public static boolean isStockSymbolValid(String json) {
    try{
      JSONObject jsonObj = new JSONObject(json);
      if (jsonObj != null && jsonObj.length() != 0){
        jsonObj = jsonObj.getJSONObject("query");
        int count = Integer.parseInt(jsonObj.getString("count"));
        jsonObj = jsonObj.getJSONObject("results");

        if (count == 1){
          JSONObject quoteObj = jsonObj.getJSONObject("quote");
          if (quoteObj.getString("Name").equals("null"))
            return false;

        } else if (count > 1){
          JSONArray quoteArr = jsonObj.getJSONArray("quote");
          if (quoteArr != null && quoteArr.length() != 0){
            for (int i=0; i < quoteArr.length(); i++){
              if (quoteArr.getJSONObject(i).getString("Name").equals("null"))
                return false;
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return true;
  }
  public static String[][] historialJsonToArray(String jsonStr) {
    String[][] data = null;
    try{
      JSONObject jsonObject = new JSONObject(jsonStr);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        data = new String[2][count];
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");
          data[0][0] = jsonObject.getString("Date");
          data[1][0] = jsonObject.getString("Adj_Close");

        } else{
          JSONArray resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              data[0][i] = jsonObject.getString("Date");
              data[1][i] = jsonObject.getString("Adj_Close");
            }
          }
        }
      }
    } catch (JSONException e) { Log.e(LOG_TAG, "String to JSON failed: " + e); }

    return data;
  }
  public static void trimArray(String[] arr, int freq){
    for(int i=0; i < arr.length; i++){
      if(i%freq != 0 && i != (arr.length - 1))  arr[i] = "";
      else arr[i] = arr[i].replaceFirst("\\d\\d\\d\\d\\-","");
    }
  }
  public static Float[] StringToFloatArray(String[] mAdjCloseStr) {
    Float[] mAdjClose = null;
    if (mAdjCloseStr != null && mAdjCloseStr.length != 0) {
      mAdjClose = new Float[mAdjCloseStr.length];
      for (int i = 0; i < mAdjCloseStr.length; i++){
        mAdjClose[i] = Float.parseFloat(mAdjCloseStr[i]);
      }
    }
    return mAdjClose;
  }

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    logLongString("==JSN==", JSON);
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      String changeInPercent = jsonObject.getString("ChangeinPercent");
      if (change.equals("null")) change = "+0.00";
      if (changeInPercent.equals("null")) changeInPercent = "+0.00%";

      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(changeInPercent, true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  public static float avg(Float[] mAdjClose) {
    float sum = 0.0f;
    for(int i=0; i < mAdjClose.length ; i++)
      sum = sum + mAdjClose[i];
    return sum/mAdjClose.length;
  }
  // To view very long string in logcat
  // Credit http://stackoverflow.com/questions/7606077/how-to-display-long-messages-in-logcat
  public static void logLongString(String TAG, String message) {
    int maxLogSize = 2000;
    for(int i = 0; i <= message.length() / maxLogSize; i++) {
      int start = i * maxLogSize;
      int end = (i+1) * maxLogSize;
      end = end > message.length() ? message.length() : end;
      android.util.Log.d(TAG, message.substring(start, end));
    }
  }
}
