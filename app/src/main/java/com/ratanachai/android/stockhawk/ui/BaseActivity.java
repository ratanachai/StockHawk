package com.ratanachai.android.stockhawk.ui;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.ratanachai.android.stockhawk.R;

/**
 * Created by keng on 28/06/16.
 */
public class BaseActivity extends AppCompatActivity {
    protected String mEmptyViewText;

    // Show empty_view, Hide recycler_view
    protected void showEmptyView(String text){
        TextView tv = (TextView)findViewById(R.id.empty_view);
        tv.setText(text);
        tv.setVisibility(View.VISIBLE);
        mEmptyViewText = text;
    }

    // Show recycler_view, Hide empty_view
    protected void hideEmptyView(){
        findViewById(R.id.empty_view).setVisibility(View.GONE);
        mEmptyViewText = null;
    }

}
