package com.jingz.app.slice;

import java.io.File;
import java.util.Vector;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.widget.OverScroller;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class IndoorMapView extends View {

	private static final String TAG = IndoorMapView.class.getSimpleName();
	
	private static final String MAP_PATH_PREFIX = "/sdcard/slice-demo/";

	private static final int SCALE_LEVEL_22 = 22;
	private static final int SCALE_LEVEL_23 = 23;
	private static final int SCALE_LEVEL_24 = 24;

	public static final int TILE_SIZE = 256;
	
	private static final float REAL_SCALE_LEVEL_MAX = 2.0f;
	private static final float REAL_SCALE_LEVEL_INIT = 1.0f;
	private static final float REAL_SCALE_LEVEL_MIN = 0.5f;

	private boolean mFirstDraw = true;
	public int mScaleLevel;
	
	private float mRealZoomLevel;
	private float mOnScaleLevel = 1.0f;
	
	private SlicedMap mMap;
	private Vector<Tile> mTiles;
	
	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	private OverScroller mScroller;
	
	private TileLoader mTileLoader;

	Paint mFramePaint;

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
		mTileLoader = new TileLoader();
		mFramePaint = new Paint();
		mFramePaint.setStyle(Style.FILL);
		mFramePaint.setColor(0xff0000ff);
		mScaleLevel = SCALE_LEVEL_23;
		mScroller = new OverScroller(getContext());
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
					initMap();
					invalidate();
				}
			});
			
			return;
		}

		int pivotX = getMeasuredWidth() / 2 + getScrollX();
		int pivotY = getMeasuredHeight() / 2 + getScrollY();
		canvas.scale(mOnScaleLevel, mOnScaleLevel, pivotX, pivotY);

		//		if (mMap != null) {
//			canvas.drawRect(mMap.frame, mFramePaint);
//		}
		
		if (mTiles != null) {
			for (int i = 0; i < mTiles.size(); i++) {
				Tile tile = mTiles.get(i);
				if (tile.bitmap != null) {
					canvas.drawBitmap(tile.bitmap, tile.x, tile.y, null);
				} else {
					Rect placeHolder = new Rect(tile.x, tile.y, tile.x + TILE_SIZE, tile.y + TILE_SIZE);
					canvas.drawRect(placeHolder, mFramePaint);
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = mScaleGestureDetector.onTouchEvent(event);
		retVal = mGestureDetector.onTouchEvent(event) || retVal;
		
		return retVal || super.onTouchEvent(event);
	}

	private void initMap() {
		scrollTo(0, 0);
		String path = MAP_PATH_PREFIX + mScaleLevel + ".jpg";
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
		
		mMap.curX = curX;
		mMap.curY = curY;
		mTiles = mMap.getVisibleTiles();
	}
	
	private void updateMap() {
		if (mMap != null) {
			updateMap(mMap.curX, mMap.curY);
		}
	}
	
	private int mCurX;
	private int mCurY;
	private boolean mIsFlinging = false;
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		
		if (!mIsFlinging) {
			return;
		}
		
		int distanceX = mScroller.getCurrX() - mCurX;
		int distanceY = mScroller.getCurrY() - mCurY;
		mCurX = mScroller.getCurrX();
		mCurY = mScroller.getCurrY();
		
		if (mScroller.computeScrollOffset()) {
			doScroll(distanceX, distanceY, true);
		} else {
			mIsFlinging = false;
			// doScroll(distanceX, distanceY, true);
		}
	}
	
	@Override
    protected final int computeHorizontalScrollRange() {
		return mMap.width;
    }
	
	@Override
	protected final int computeVerticalScrollRange() {
    	return mMap.height;
	}

	private void doScroll(int distanceX, int distanceY, boolean needUpdate) {
		if (mMap != null && distanceX < 0) {
			if ((getScrollX() + (int) distanceX) < mMap.frame.left) {
				distanceX = mMap.frame.left - getScrollX();
			}
		}

		if (mMap != null && distanceX > 0) {
			if ((getScrollX() + (int) distanceX + getMeasuredWidth() - 1) > mMap.frame.right) {
				distanceX = mMap.frame.right - getScrollX()
						- getMeasuredWidth() + 1;
			}
		}

		if (mMap != null && distanceY < 0) {
			if ((getScrollY() + (int) distanceY) < mMap.frame.top) {
				distanceY = mMap.frame.top - getScrollY();
			}
		}

		if (mMap != null && distanceY > 0) {
			if ((getScrollY() + (int) distanceY + getMeasuredHeight() - 1) > mMap.frame.bottom) {
				distanceY = mMap.frame.bottom - getScrollY()
						- getMeasuredHeight() + 1;
			}
		}

		scrollBy((int) distanceX, (int) distanceY);
		mMap.curX += distanceX;
		mMap.curY += distanceY;
		
		if (needUpdate) {
			updateMap();
		}
		
		invalidate();
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
			
			if (areaToRender[0] == left && areaToRender[1] == top
					&& areaToRender[2] == right && areaToRender[3] == bottom) {
				return tiles;
			}
			
			clearTiles();
			areaToRender[0] = left;
			areaToRender[1] = top;
			areaToRender[2] = right;
			areaToRender[3] = bottom;
			
			for (int i = top; i <= bottom; i++ ) {
				for (int j = left; j <= right; j++) {
					int tileId = generateId(j, i, mScaleLevel);
					int tileX = getScrollX() - (curX % TILE_SIZE) + (j - left) * TILE_SIZE;
					int tileY = getScrollY() - (curY % TILE_SIZE) + (i - top) * TILE_SIZE;
					
					Tile tile = mTileLoader.loadTile(tileId, tileX, tileY);
					if (tile != null) {
						tiles.add(tile);
					}
				}
			}
			
			return tiles;
		}
		
		private static final int __10_MASK = ((1 << 20) - 1);
		
		private int generateId(int x, int y, int level) {
			return (x & __10_MASK) | ((y & __10_MASK) << 10) | ((level & 0xff) << 20);
		}
		
		private void clearTiles() {
			if (tiles == null) {
				return;
			}
			
			tiles.clear();		
		}
	}
	
	private class TileLoader {
		
		LruCache<Integer, Tile> mMemoryCache = new LruCache<Integer, Tile>(80);
		
		/**
		 * Load a tile indicated by tileId.
		 * Use cache to do this with higher efficient
		 * @param tileId
		 * @return the Tile instance
		 */
		public Tile loadTile(int tileId, int x, int y) {
			Tile tile = null;
			
			tile = mMemoryCache.get(tileId);
			
			if (tile == null) {
				String path = MAP_PATH_PREFIX + "slices/" + mScaleLevel + "/" + tileId + ".jpg";
				File tileFile = new File(path);
				
				if (!tileFile.exists()) {
					Log.d(TAG, path + " is not an available tile.");
					return null;
				}
				
				tile = new Tile();
				tile.bitmap = null;
				mMemoryCache.put(tileId, tile);
				
				// load tile bitmap from external storage asynchronously
				LoadTileBitmapTask loadBitmapTask = new LoadTileBitmapTask(tile, path);
				loadBitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			
			if (tile != null) {
				tile.x = x;
				tile.y = y;
			}
			
			return tile;
		}
	}
	
	private class LoadTileBitmapTask extends AsyncTask<Void, Void, Void> {
		private Tile mTile;
		private String mImagePath;

		public LoadTileBitmapTask(Tile tile, String path) {
			if (tile == null) {
				throw new IllegalArgumentException();
			}
			
			mTile = tile;
			mImagePath = path;
		}
		
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "Asynchronously loading bitmap: " + mImagePath);
			mTile.bitmap = BitmapFactory.decodeFile(mImagePath);
			postInvalidate();
			return null;
		}
		
	}
	
	private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			int dx = (int) distanceX;
			int dy = (int) distanceY;
			
			if (mMap != null) {
				if (mMap.width <= getMeasuredWidth()) {
					dx = 0;
				}
				
				if (dx < 0) {
					if ((getScrollX() + (int) dx) < mMap.frame.left) {
						dx = mMap.frame.left - getScrollX();
					}
				}
				
				if (dx > 0) {
					if ((getScrollX() + (int) dx + getMeasuredWidth() - 1) > mMap.frame.right) {
						dx = mMap.frame.right - getScrollX() - getMeasuredWidth() + 1 ;
					}
				}
				
				if (mMap.height <= getMeasuredHeight()) {
					dy = 0;
				}
				
				if (dy < 0) {
					if ((getScrollY() + (int) dy) < mMap.frame.top) {
						dy = mMap.frame.top - getScrollY();
					}
				}
				
				if (dy > 0) {
					if ((getScrollY() + (int) dy + getMeasuredHeight() - 1) > mMap.frame.bottom) {
						dy = mMap.frame.bottom - getScrollY() - getMeasuredHeight() + 1 ;
					}
				}
			}
			
			scrollBy((int) dx, (int) dy);
			mMap.curX += dx;
			mMap.curY += dy;
			updateMap();
			return true;
		}

		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			
//			mIsFlinging = true;
//			mCurX = getScrollX();
//			mCurY = getScrollY();
//
//			mScroller.forceFinished(true);
////			mScroller.fling(
////					getScrollX(), 
////					getScrollY(), 
////					(int) -velocityX, 
////					(int) -velocityY, 
////					0, mMap.width - getMeasuredWidth(), 
////					0, mMap.height - getMeasuredHeight(), 
////					getMeasuredWidth() / 4, 
////					getMeasuredHeight() / 4);
//			
//			mScroller.startScroll(mCurX, mCurY, (int) -velocityX / 10, (int) -velocityY / 10, 1000);
//			ViewCompat.postInvalidateOnAnimation(IndoorMapView.this);
//			return true;
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			mIsFlinging = false;
			if (!mScroller.isFinished()) {
				mScroller.forceFinished(true);
			}
			return true;
		}
		
	};
	
	private SimpleOnScaleGestureListener mScaleGestureListener = new SimpleOnScaleGestureListener() {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mOnScaleLevel *= detector.getScaleFactor();
			invalidate();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mOnScaleLevel = 1.0f;
			mRealZoomLevel = getRealScaleLevel(mScaleLevel);
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			mScaleLevel = getScaleLevel(mOnScaleLevel * mRealZoomLevel);
			mOnScaleLevel = 1.0f;
			mFirstDraw = true;
			invalidate();
		}
		
	};

	private float getRealScaleLevel(int scaleLevel) {
		if (scaleLevel == SCALE_LEVEL_22) { return REAL_SCALE_LEVEL_MIN; }
		if (scaleLevel == SCALE_LEVEL_23) { return REAL_SCALE_LEVEL_INIT; }
		if (scaleLevel == SCALE_LEVEL_24) { return REAL_SCALE_LEVEL_MAX; }
		
		throw new IllegalArgumentException("The parameter must be one of 22, 23 or 24.");
	}

	private int getScaleLevel(float scaleFactor) {
		final float upperLimit = (REAL_SCALE_LEVEL_MAX + REAL_SCALE_LEVEL_INIT) / 2;
		final float lowerLimit = (REAL_SCALE_LEVEL_MIN + REAL_SCALE_LEVEL_INIT) / 2;
		
		if (scaleFactor >= upperLimit) {
			return SCALE_LEVEL_24;
		} else if (scaleFactor >= lowerLimit && scaleFactor < upperLimit) {
			return SCALE_LEVEL_23;
		} else if (scaleFactor < lowerLimit) {
			return SCALE_LEVEL_22;
		}
		
		return SCALE_LEVEL_23;
	}

}
