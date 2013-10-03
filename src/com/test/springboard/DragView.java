package com.test.springboard;

import java.util.HashMap;

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
import android.widget.TextView;

public class DragView extends ViewGroup implements View.OnLongClickListener {

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
	private int mPreviousSwapPosition = -1;
	private int mScreenHeight;
	private View mDraggedView;
	
	private HashMap<Integer, View> mGridMap;
	private HashMap<Integer, View> mTrayMap;
	
	public DragView(Context context) {
		super(context);
		
		mContext = context;
		init();
		Log.i(SpringboardActivity.LOGTAG, TAG + " - constructor");
	}

	private void init() {
		// Calculate child size here
		
		mGridMap = new HashMap<Integer, View>();
		mTrayMap = new HashMap<Integer, View>();
		
		Display display = ((SpringboardActivity) mContext).getWindowManager().getDefaultDisplay();
		int screenWidth = display.getWidth();
		mScreenHeight = display.getHeight();
		mChildSize = (int) screenWidth / mColCount;
		
		setOnLongClickListener(this);
		
	}
	
	@Override
	protected boolean addViewInLayout(View child, int index, LayoutParams params) {
		return super.addViewInLayout(child, index, params);
	}
	
	/**
	 * Adds the given view to the layout and to the map
	 * index should be linear and should not be zeroed on 
	 * adding views to tray. The parent is a single layout. 
	 * Its upto this class to differentiate the tray views
	 * and manipulate them accordingly.
	 * @param isTray - whether the icon has to go to tray or grid
	 * @param child - the view to be added to DragView
	 * @param index - index position of the view to be added to DragView
	 * @param params - params of the view to be added to DragView
	 */
	public void addViewToLayout(boolean isTray, View child, int index, LayoutParams params) {
		Log.i(SpringboardActivity.LOGTAG, TAG + " - addViewToLayout index: "+index+" isTray: "+isTray);
		if (isTray) {
			mTrayMap.put(index - mGridMap.size(), child);
		} else {
			mGridMap.put(index, child);
		}
		addViewInLayout(child, index, params);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		Log.i(SpringboardActivity.LOGTAG, TAG + " - onLayout - changed: "+changed+" left: "+left+" top: "+top+" right: "+right+" bottom: "+bottom);

		for (int i = 0; i < getGridViewCount(); i++) {
			View child = mGridMap.get(i);
			Point xy = getCoordinatesFromPosition(i);
			child.measure(MeasureSpec.makeMeasureSpec(child.getLayoutParams().width, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.UNSPECIFIED));
			child.layout(xy.x, xy.y, xy.x + mChildSize, xy.y + mChildSize);
		}
		
		for (int j = 0; j < getTrayViewCount(); j++) {
			View child = mTrayMap.get(j);
			Point xy = getCoordinatesFromPosition(j+getGridViewCount());
			child.measure(MeasureSpec.makeMeasureSpec(child.getLayoutParams().width, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.UNSPECIFIED));
			child.layout(xy.x, xy.y, xy.x + mChildSize, xy.y + mChildSize);
		}
		
		invalidate();
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
					
					if (mDraggedItem < getGridViewCount()) 
					{
						Log.i(SpringboardActivity.LOGTAG, TAG + " Grid Area - index: "+index+" mDraggedItem: "+mDraggedItem);
						if (index == -1) index = mGridMap.size()-1;
						
						if (index < getGridViewCount()) { 
							if (index < mDraggedItem) {
								for (int i = index; i < mDraggedItem; i++) {
									moveViewToPosition(i, i+1);
								}
								for (int i = mDraggedItem-1; i >= index; i--) {
									View view = mGridMap.get(i);
									mGridMap.put(i+1, view);
								}
							} else {
								for (int i = index; i > mDraggedItem; i--) {
									moveViewToPosition(i, i-1);
								}
								for (int i = mDraggedItem+1; i <= index; i++) {
									View view = mGridMap.get(i);
									mGridMap.put(i-1, view);
								}
							}
//							Log.i(SpringboardActivity.LOGTAG, TAG + " - onTouchEvent - ACTION_MOVE - mDraggedItem: "+mDraggedItem+" index: "+index);
							mGridMap.put(index, mDraggedView);
							mDraggedItem = index;
						} else {
							// User moved icon into app tray.
							// Swap icons here 
							
							if (mPreviousSwapPosition != index) {
								swapViews(index);
							}
						}
					} 
					else if (mDraggedItem >= getGridViewCount()) 
					{
						Log.i(SpringboardActivity.LOGTAG, TAG + " Tray Area - index: "+index+" mDraggedItem: "+mDraggedItem);
						if (index == -1) break;
						
						if (index >= getGridViewCount()) {
							// User moving around the items within App Tray.
							// Re-arrange icons using the same logic as grid
							if (index < mDraggedItem) {
								for (int i = index; i < mDraggedItem; i++) {
									moveViewToPosition(i, i+1);
								}
								int dragged = mDraggedItem - getGridViewCount();
								for (int i = dragged-1; i >= index-getGridViewCount(); i--) {
									View view = mTrayMap.get(i);
									mTrayMap.put(i+1, view);
								}
							} else {
								for (int i = index; i > mDraggedItem; i--) {
									moveViewToPosition(i, i-1);
								}
								int dragged = mDraggedItem - getGridViewCount();
								for (int i = dragged+1; i <= index-getGridViewCount(); i++) {
									View view = mTrayMap.get(i);
									mTrayMap.put(i-1, view);
								}
							}
							
//							Log.i(SpringboardActivity.LOGTAG, TAG + " - onTouchEvent - ACTION_MOVE - mDraggedItem: "+mDraggedItem+" index: "+index);
							mTrayMap.put(index-getGridViewCount(), mDraggedView);
							mDraggedItem = index;
						} else {
							// User moving the icon into Grid.
							// Swap icons only if the dragged icon is placed within 
							// bounds of another icon in grid
							
							if (mPreviousSwapPosition != index) {
								swapViews(index);
							}
						}
					}
				}
				
				break;
			}
			case MotionEvent.ACTION_UP: {
				
				int x = (int) event.getX();
				int y = (int) event.getY();
				
				if (mDraggedItem >= 0) {
					
					int index = getIndexFromCoordinates(x, y);
					if (index == -1) index = mGridMap.size()-1;
					
					Point xy = getCoordinatesFromPosition(index);
					mDraggedView.layout(xy.x, xy.y, xy.x+mChildSize, xy.y+mChildSize);
					/*if (mDraggedItem < getGridViewCount()) {
						mGridMap.put(index, mDraggedView);
					} else {
						mTrayMap.put(index-getGridViewCount(), mDraggedView);
					}*/
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
			if (index < getGridViewCount()) {
				mDraggedView = mGridMap.get(index);
			} else {
				mDraggedView = mTrayMap.get(index - getGridViewCount());
			}
			setDraggedItemPosition(mTouchDownX, mTouchDownY);
			animateDragged();
		}
		
		Log.i(SpringboardActivity.LOGTAG, TAG + " - onLongClick - dragged item: "+mDraggedItem+" gridViewCount: "+getGridViewCount());
		
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
	
	private void swapViews(int toPos) {
		Log.i(SpringboardActivity.LOGTAG, TAG + " - swapViews - draggedItem: "+mDraggedItem+" toPos: "+toPos+" mPreviousSwapPosition: "+mPreviousSwapPosition);
		if (mPreviousSwapPosition != -1) {
			moveViewToPosition(mDraggedItem, mPreviousSwapPosition);
			if (mDraggedItem < getGridViewCount()) {
				swapTwoViewsInMap(mPreviousSwapPosition, mDraggedItem);
			} else {
				swapTwoViewsInMap(mDraggedItem-getGridViewCount(), mPreviousSwapPosition);
			}
		} else {
			moveViewToPosition(mDraggedItem, toPos);
		}
		moveViewToPosition(toPos, mDraggedItem);
		if (mDraggedItem < getGridViewCount()) {
			// swap from grid map to tray map
			int trayPos = toPos - getGridViewCount();
			swapTwoViewsInMap(trayPos, mDraggedItem);
		} else {
			// swap from tray map to grid map
			int trayPos = mDraggedItem - getGridViewCount();
			swapTwoViewsInMap(trayPos, toPos);
		}
		mPreviousSwapPosition = toPos;
	}
	
	private void swapTwoViewsInMap(int trayPos, int gridPos) {
		View view = mTrayMap.get(trayPos);
		mTrayMap.put(trayPos, mGridMap.get(gridPos));
		mGridMap.put(gridPos, view);
	}
	
	private void moveViewToPosition(int fromPos, int toPos) {
//		Log.i(SpringboardActivity.LOGTAG, TAG + " - moveViewToPosition - fromPos: "+fromPos+" toPos: "+toPos);
		View swapView;
		if (fromPos < getGridViewCount()) {
			swapView = mGridMap.get(fromPos);
		} else {
			swapView = mTrayMap.get(fromPos-getGridViewCount());
		}
		animateNavigation(swapView, fromPos, toPos);
		invalidate();
	}
	
	private Point getCoordinatesFromPosition(int index) {
		Point xy =  new Point();
		int col = index % mColCount;
        int row = index / mColCount;
        
		if (index < getGridViewCount()) {
			xy.x = mChildSize * col;
	        xy.y = mChildSize * row;
		} else {
			xy.x = mChildSize * (index - getGridViewCount());
			int topMargin = mScreenHeight - mChildSize;
			xy.y = topMargin - 24 - 24;
		}
		
		return xy;
	}
	
	// Returns the index of the item that is being pressed based on the 
	// co-ordinates of the touch position.
	// Doesnt return index if one of the tab bar items is clicked.
	private int getIndexFromCoordinates(int x, int y) {
		
		for (int i = 0; i < getGridViewCount(); i++) {
			Point xy = getCoordinatesFromPosition(i);
			if ((x > xy.x) && (x < (xy.x+mChildSize))) {
				if ((y > xy.y) && (y < (xy.y+mChildSize))) {
					return i;
				}
			}
		}
		
		for (int j = 0; j < getTrayViewCount(); j++) {
			Point xy = getCoordinatesFromPosition(j+getGridViewCount());
			if ((x > xy.x) && (x < (xy.x+mChildSize))) {
				if ((y > xy.y) && (y < (xy.y+mChildSize))) {
					return j+getGridViewCount();
				}
			}
		}
		
		return -1;
	}
	
	// Getter setter methods below for convenience.
	public int getChildSize() {
		return mChildSize;
	}
	
	public int getGridViewCount() {
		return mGridMap.size();
	}
	
	public int getTrayViewCount() {
		return mTrayMap.size();
	}
	
	public void printAllItemsInTrayView() {
		for (int i = 0; i < getTrayViewCount(); i++) {
			View view = mTrayMap.get(i);
			TextView tv = (TextView) view.findViewById(R.id.tvIcon);
			Log.i(SpringboardActivity.LOGTAG, TAG + " - print - i: " + i + " tv text: "+tv.getText().toString());
		}
	}
}
