package com.jingz.app.slice;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class IndoorMapView extends View {
	private static final String MAP_PATH_PREFIX = "/sdcard/slice-demo";
	
	private static final int SCALE_LEVEL_22 = 22;
	private static final int SCALE_LEVEL_23 = 23;
	private static final int SCALE_LEVEL_24 = 24;

	private static final int TILE_SIZE = 256;
	
	private Bitmap mMap;
	
	private boolean mFirstMeaseured = false;

	private int mStartX;
	private int mStartY;
	private int mScaleLevel = SCALE_LEVEL_24;

	private Vector<Bitmap> mTiles = new Vector<Bitmap>();

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
		// mMap = loadMapByLevel(SCALE_LEVEL_24);
	}

	private Bitmap[] loadMapByLevel(int level) {
		String path = MAP_PATH_PREFIX + "/" + level + ".jpg";
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap originalMap = BitmapFactory.decodeFile(path, options);
		
		int width = originalMap.getWidth();
		int height = originalMap.getHeight();
		
		int cols = width / TILE_SIZE + ((width % TILE_SIZE == 0) ? 0 : 1);
		int rows = height / TILE_SIZE + ((height % TILE_SIZE == 0) ? 0 : 1);
		
		rebuildTiles(rows, cols);
		
		
		return null;	
	}

	private void releaseTiles() {
		
		for (Bitmap b : mTiles) {
			b.recycle();
		}
		
		mTiles.clear();
	}
	
	private void rebuildTiles() {
		
	}
	
	private static final int __10_MASK = ((1 << 20) - 1);
	
	private static int generateId(int x, int y, int level) {
		return (x & __10_MASK) | ((y & __10_MASK) << 10) | ((level & 0xff) << 20);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!mFirstMeaseured) {
			mFirstMeaseured = true;
			
			int viewWidth = getMeasuredWidth();
			int viewHeight = getMeasuredHeight();
			int mapWidth = mMap.getWidth();
			int mapHeight = mMap.getHeight();
			
			mStartX = (viewWidth - mapWidth) / 2;
			mStartY = (viewHeight - mapHeight) / 2;
			
			invalidate();
			return;
		}
		
		drawMap(canvas);
	}

	private void drawMap(Canvas canvas) {
		if (mMap == null) {
			return;
		}
		
		canvas.drawBitmap(mMap, mStartX, mStartY, null);
	}

	
}
