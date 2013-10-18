package com.jingz.app.slice;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

public class IndoorMapView extends View {

	private static final String TAG = IndoorMapView.class.getSimpleName();
	
	private static final String MAP_PATH_PREFIX = "/sdcard/slice-demo/";

	private static final int SCALE_LEVEL_22 = 22;
	private static final int SCALE_LEVEL_23 = 23;
	private static final int SCALE_LEVEL_24 = 24;

	public static final int TILE_SIZE = 256;

	private boolean mFirstDraw = true;
	public int mScaleLevel;
	
	private SlicedMap mMap;
	private Vector<Tile> mTiles;
	
	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	Paint mFramePaint;

	private boolean mScrolled = false;
	
	public IndoorMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public IndoorMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public IndoorMapView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mFramePaint = new Paint();
		mFramePaint.setStyle(Style.FILL);
		mFramePaint.setColor(0x550000ff);
		mScaleLevel = SCALE_LEVEL_24;
		mGestureDetector = new GestureDetector(getContext(), mGestureListener);
		mScaleGestureDetector = new ScaleGestureDetector(getContext(), mScaleGestureListener);
	}
	

	@Override
	protected void onDraw(Canvas canvas) {
		if (mFirstDraw) {
			mFirstDraw = false;
			post(new Runnable() {

				@Override
				public void run() {
					initMap(SCALE_LEVEL_24);
					invalidate();
				}
			});
			
			return;
		}

		
		Log.d(TAG, "Draw frame.");
		if (mMap != null) {
			canvas.drawRect(mMap.frame, mFramePaint);
		}
		
		Log.d(TAG, "Draw tiles.");
		if (mTiles != null) {
			for (int i = 0; i < mTiles.size(); i++) {
				Tile tile = mTiles.get(i);
				if (tile.bitmap != null) {
					canvas.drawBitmap(tile.bitmap, tile.x, tile.y, null);
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = mGestureDetector.onTouchEvent(event);
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (mScrolled) {
				mScrolled = false;
				if (mMap != null) {
					updateMap();
					invalidate();
				}
			}
			
			return true;
		}
		
		return retVal || super.onTouchEvent(event);
	}

	private void initMap(int level) {
		String path = MAP_PATH_PREFIX + level + ".jpg";
		Log.d(TAG, "Original map: " + path);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		mMap = new SlicedMap(options.outWidth, options.outHeight, this);
		updateMap((mMap.width - getMeasuredWidth()) / 2,
				(mMap.height - getMeasuredHeight()) / 2);
	}
	
	private void updateMap(int curX, int curY) {
		if (mMap == null) {
			return;
		}
		
		if (curX < 0) 				{ curX = 0; }
		if (curX >= mMap.width) 	{ curX = mMap.width - 1; }
		if (curY < 0) 				{ curY = 0; }
		if (curY >= mMap.height) 	{ curX = mMap.height - 1; }
		
		Log.d(TAG, "updateMap: curX=" + curX + ", curY=" + curY);
		mMap.curX = curX;
		mMap.curY = curY;
		mTiles = mMap.getVisibleTiles();
	}
	
	private void updateMap() {
		if (mMap != null) {
			updateMap(mMap.curX, mMap.curY);
		}
	}

	private static class Tile {
		public int x;
		public int y;
		public Bitmap bitmap;
	}

	private class SlicedMap {

		public final int width;
		public final int height;
		public final int rows;
		public final int columns;
		public int curX = 0;
		public int curY = 0;
		
		public Rect frame;
		
		Vector<Tile> tiles = new Vector<Tile>();
		
		public int[] areaToRender = { -1, -1, -1, -1 };
		
		private View mView;

		public SlicedMap(int originalWidth, int originalHeight, View view) {
			mView = view;

			rows = originalHeight / TILE_SIZE + ((originalHeight % TILE_SIZE == 0) ? 0 : 1);
			columns = originalWidth / TILE_SIZE + ((originalWidth % TILE_SIZE == 0) ? 0 : 1);
			
			width = columns * TILE_SIZE;
			height = rows * TILE_SIZE;
			
			int left = (getMeasuredWidth() - width) / 2;
			int top = (getMeasuredHeight() - height) / 2;
		
			frame = new Rect(left, top, left + width - 1, top + height - 1);
		}

		public Vector<Tile> getVisibleTiles() {
			if (mView == null) {
				return null;
			}
			
			int viewLeft = curX;
			int viewTop = curY;
			int viewRight = (viewLeft + mView.getMeasuredWidth() - 1);
			int viewBottom = (viewTop + mView.getMeasuredHeight() - 1);
			
			
			int left = viewLeft / TILE_SIZE;
			int top = viewTop / TILE_SIZE;
			int right = viewRight / TILE_SIZE;
			int bottom = viewBottom / TILE_SIZE;
			
			// Log.d(TAG, "Left: " + left + ", Top: " + top + ", Right: " + right + ", Bottom: " + bottom);
			
			if (areaToRender[0] == left && areaToRender[1] == top
					&& areaToRender[2] == right && areaToRender[3] == bottom) {
				Log.d(TAG, "Still use the same tile list.");
				return tiles;
			}
			
			
			tiles.clear();
			for (int i = top; i <= bottom; i++ ) {
				for (int j = left; j <= right; j++) {
					int tileId = generateId(j, i, mScaleLevel);
					String tilePath = MAP_PATH_PREFIX + "slices/" + mScaleLevel + "/" + tileId + ".jpg";
					//Log.d(TAG, "Add tile: " + tilePath);
					Tile tile = new Tile();
					tile.x = getScrollX() -(curX % TILE_SIZE) + (j - left) * TILE_SIZE;
					tile.y = getScrollY() -(curY % TILE_SIZE) + (i - top) * TILE_SIZE;
					tile.bitmap = BitmapFactory.decodeFile(tilePath);
					//Log.d(TAG, "Tile: x=" + tile.x + ", y=" + tile.y + ", bitmap=" + tile.bitmap);
					tiles.add(tile);
				}
			}
			
			return tiles;
		}
		
		private static final int __10_MASK = ((1 << 20) - 1);
		
		private int generateId(int x, int y, int level) {
			return (x & __10_MASK) | ((y & __10_MASK) << 10) | ((level & 0xff) << 20);
		}
	}
	
	private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			
			Log.d(TAG, "Scroll by: x=" + distanceX + ", y=" + distanceY);
			Log.d(TAG, "Scrolld: scrollX=" + getScrollX() + ", scrollY=" + getScrollY());
			
			mScrolled = true;
			if (mMap != null && distanceX < 0) {
				if ((getScrollX() + (int) distanceX) < mMap.frame.left) {
					distanceX = mMap.frame.left - getScrollX();
				}
			}
			
			if (mMap != null && distanceX > 0) {
				if ((getScrollX() + (int) distanceX + getMeasuredWidth() - 1) > mMap.frame.right) {
					distanceX = mMap.frame.right - getScrollX() - getMeasuredWidth() + 1 ;
				}
			}
			
			if (mMap != null && distanceY < 0) {
				if ((getScrollY() + (int) distanceY) < mMap.frame.top) {
					distanceY = mMap.frame.top - getScrollY();
				}
			}
			
			if (mMap != null && distanceY > 0) {
				if ((getScrollY() + (int) distanceY + getMeasuredHeight() - 1) > mMap.frame.bottom) {
					distanceY = mMap.frame.bottom - getScrollY() - getMeasuredHeight() + 1 ;
				}
			}
			
			scrollBy((int) distanceX, (int) distanceY);
			mMap.curX += distanceX;
			mMap.curY += distanceY;
			
			return true;
		}

		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return true;
		}
		
	};
	private OnScaleGestureListener mScaleGestureListener;
	
	private static class ImageLoader {
		
	}
}
