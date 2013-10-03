package com.test.springboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class DragViewTabBar extends ViewGroup implements View.OnLongClickListener {

	private final String TAG = "DragView";
	private Context mContext;
	private int mColCount = 4;		// Indicates the max number of columns that can be used.
	private int mRow = 0;			// Indicates the number of rows. Does not include tab bar.
	private int mColumn = 0;		// Indicates the current column. Does not include tab bar.
	private int mChildSize;			// Indicates the size of each child item. Calculated based on screen width and max no. of columns.
	private int mTouchDownX, mTouchDownY = 0;
	private int mFullGridItemCount = 0;
	private int mDraggedItem = -1;
	private int mTouchDownDragged = -1;
	private View mDraggedView;
	private int mScreenHeight = 0;
	
	private HashMap<Integer, View> mViewMap;
	
	public DragViewTabBar(Context context) {
		super(context);
		mContext = context;
		init();
		Log.i(SpringboardActivity.LOGTAG, TAG + " - constructor");
	}

	private void init() {
		// Calculate child size here
		
		mViewMap = new HashMap<Integer, View>();
		
		Display display = ((SpringboardActivity) mContext).getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		mScreenHeight = display.getHeight();
		mChildSize = (int) screenWidth / mColCount;
		
		setOnLongClickListener(this);
		
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		Log.i(SpringboardActivity.LOGTAG, TAG + " - onLayout - changed: "+changed+" left: "+left+" top: "+top+" right: "+right+" bottom: "+bottom);

		for (int i = 0; i < getChildCount(); i++) {
			
			View child = mViewMap.get(i);
			Point xy = getCoordinatesFromPosition(i);
			child.measure(MeasureSpec.makeMeasureSpec(child.getLayoutParams().width, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.UNSPECIFIED));
			child.layout(xy.x, xy.y, xy.x + mChildSize, xy.y + mChildSize);
		}
		invalidate();
	}
	
	@Override
	protected boolean addViewInLayout(View child, int index, LayoutParams params) {
		Log.i(SpringboardActivity.LOGTAG, TAG + " - addViewInLayout index: "+index);
		if (getChildCount() >= 4) {
			return false;
		}
		mViewMap.put(index, child);
		return super.addViewInLayout(child, index, params);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
//		Log.i(SpringboardActivity.LOGTAG, TAG + " - onTouchEvent action: "+event.getAction());
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				mTouchDownX = (int) event.getX();
				mTouchDownY = (int) event.getY();
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				int x = (int) event.getX();
				int y = (int) event.getY();
				
				if (mDraggedItem >= 0) {
					setDraggedItemPosition(x, y);
					
					int index = getIndexFromCoordinates(x, y);
					if (index == mDraggedItem) break;
					if (index == -1) index = mColCount-1;
					
					if (index < mDraggedItem) {
						for (int i = index; i < mDraggedItem; i++) {
							moveViewToPosition(i, i+1);
						}
						for (int i = mDraggedItem-1; i >= index; i--) {
							View view = mViewMap.get(i);
							mViewMap.put(i+1, view);
						}
					} else {
						for (int i = index; i > mDraggedItem; i--) {
							moveViewToPosition(i, i-1);
						}
						for (int i = mDraggedItem+1; i <= index; i++) {
							View view = mViewMap.get(i);
							mViewMap.put(i-1, view);
						}
					}
					
					Log.i(SpringboardActivity.LOGTAG, TAG + " - onTouchEvent - ACTION_MOVE - mDraggedItem: "+mDraggedItem+" index: "+index);
					mDraggedItem = index;
				}
				
				break;
			}
			case MotionEvent.ACTION_UP: {
				
				int x = (int) event.getX();
				int y = (int) event.getY();
				
				if (mDraggedItem >= 0) {
					
					int index = getIndexFromCoordinates(x, y);
					if (index == -1) index = mColCount-1;
					
					Point xy = getCoordinatesFromPosition(index);
					mDraggedView.layout(xy.x, xy.y, xy.x+mChildSize, xy.y+mChildSize);
					mViewMap.put(index, mDraggedView);
					mDraggedView.clearAnimation();
				}
				mDraggedItem = -1;
				break;
			}
		}
		
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean onLongClick(View v) {
		
		int index = getIndexFromCoordinates(mTouchDownX, mTouchDownY);
		if (index >= 0) {
			// animate this item for enabling drag.
			mTouchDownDragged = mDraggedItem = index;
			mDraggedView = mViewMap.get(index);
			setDraggedItemPosition(mTouchDownX, mTouchDownY);
			animateDragged();
		}
		
		Log.i(SpringboardActivity.LOGTAG, TAG + " - onLongClick - dragged item: "+mDraggedItem);
		
		return false;
	}
	
	private void setDraggedItemPosition(int x, int y) {
		int halfWidth = mChildSize/2;
		mDraggedView.layout(x-halfWidth, y-halfWidth, x+halfWidth, y+halfWidth);
		invalidate();
	}
	
	private final int animTime = 150;
	private void animateDragged() {
		AnimationSet animSet = new AnimationSet(true);
		ScaleAnimation scale = new ScaleAnimation(.667f, 1, .667f, 1, mChildSize * 3 / 4, mChildSize * 3 / 4);
		scale.setDuration(animTime);
		AlphaAnimation alpha = new AlphaAnimation(1, .5f);
		alpha.setDuration(animTime);
		animSet.addAnimation(scale);
		animSet.addAnimation(alpha);
		animSet.setFillEnabled(true);
		animSet.setFillAfter(true);
		mDraggedView.clearAnimation();
		mDraggedView.startAnimation(animSet);
	}
	
	private void animateNavigation(View v, int fromPos, int toPos) {
		Point oldXY = getCoordinatesFromPosition(fromPos);
		Point newXY = getCoordinatesFromPosition(toPos);
		Point oldOffset = new Point(oldXY.x - v.getLeft(), oldXY.y - v.getTop());
		Point newOffset = new Point(newXY.x - v.getLeft(), newXY.y - v.getTop());
		
		TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x,
															  Animation.ABSOLUTE, newOffset.x,
															  Animation.ABSOLUTE, oldOffset.y,
															  Animation.ABSOLUTE, newOffset.y);
		translate.setDuration(animTime);
		translate.setFillEnabled(true);
		translate.setFillAfter(true);
		v.clearAnimation();
		v.startAnimation(translate);
	}
	
	private void moveViewToPosition(int fromPos, int toPos) {
		Log.i(SpringboardActivity.LOGTAG, TAG + " - moveViewToPosition - fromPos: "+fromPos+" toPos: "+toPos);
		View swapView = mViewMap.get(fromPos);
//		Point xy = getCoordinatesFromPosition(toPos);
		animateNavigation(swapView, fromPos, toPos);
//		swapView.layout(xy.x, xy.y, xy.x+mChildSize, xy.y+mChildSize);
		invalidate();
	}
	
	private Point getCoordinatesFromPosition(int index) {
		Point xy =  new Point();
		
		int col = index % mColCount;
        int row = index / mColCount;
//        Log.i(SpringboardActivity.LOGTAG, TAG + " - getCoordinatesFromPosition col: "+col+" row: "+row);
        
        int topMargin = mScreenHeight - mChildSize;
        
        xy.x = mChildSize * col;
        xy.y = topMargin - 24 - 24;
		
		return xy;
	}
	
	// Returns the index of the item that is being pressed based on the 
	// co-ordinates of the touch position.
	// Doesnt return index if one of the tab bar items is clicked.
	private int getIndexFromCoordinates(int x, int y) {
		
		for (int i = 0; i < mColCount; i++) {
			Point xy = getCoordinatesFromPosition(i);
			if ((x > xy.x) && (x < (xy.x+mChildSize))) {
				if ((y > xy.y) && (y < (xy.y+mChildSize))) {
					return i;
				}
			}
		}
		
		return -1;
	}
	
	public int getChildSize() {
		return mChildSize;
	}
}
