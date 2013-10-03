package com.test.springboard;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SpringboardActivity extends Activity {

	public static final String LOGTAG = "Springboard";
	private final String TAG = "SpringboardActivity";
	
	private RelativeLayout mRlRoot;
	/*private RelativeLayout mRlFullGrid;
	private RelativeLayout mRlTabBar;
	private DragView mDragView;
	private DragViewTabBar mDragViewTabBar;*/
	private DraggableView mDragView;
	private LayoutInflater mInflater;
	
//	private ArrayList<View> mGridItems;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.springboard);
        
        Log.i(LOGTAG, TAG + " - onCreate");

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mRlRoot = (RelativeLayout) findViewById(R.id.sb_RlRoot);
//        mDragView = new DragView(this);
        mDragView = new DraggableView(this);
//        mDragViewTabBar = new DragViewTabBar(this);
        mRlRoot.addView(mDragView);
//        mRlRoot.addView(mDragViewTabBar);
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	Log.i(LOGTAG, TAG + " - onStart");
    	
    	addItemsToGridView();
//    	addItemsToTabBar();
    }
    
    private void addItemsToGridView() {
    	mDragView.setGridViewCount(10);
    	for (int i = 0; i < 14; i++) {
    		View view = mInflater.inflate(R.layout.one_item, null);
    		ImageView ivIcon = (ImageView) view.findViewById(R.id.ivIcon);
    		TextView tvIcon = (TextView) view.findViewById(R.id.tvIcon);
    		tvIcon.setText("EG "+i);
    		int childSize = mDragView.getChildSize();
    		ivIcon.setBackgroundResource(R.drawable.chrome_icon);
    		LinearLayout.LayoutParams ivParams = (LinearLayout.LayoutParams) ivIcon.getLayoutParams();
    		ivParams.width = childSize / 2;
    		ivParams.height = childSize / 2;
    		ivIcon.setLayoutParams(ivParams);
    		mDragView.addViewToLayout(view, i, new LayoutParams(childSize, childSize));
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.springboard, menu);
        return true;
    }
}
