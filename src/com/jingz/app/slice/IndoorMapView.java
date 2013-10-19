package com.jingz.app.slice;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
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
		mScaleLevel = SCALE_LEVEL_24;
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

		if (mTiles != null) {
			for (int i = 0; i < mTiles.size(); i++) {
				Tile tile = mTiles.get(i);
				canvas.drawBitmap(tile.bitmap, tile.x, tile.y, null);
			}
		}
	}

	private void initMap(int level) {
		String path = MAP_PATH_PREFIX + level + ".jpg";
		Log.d(TAG, "Original map: " + path);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap originalMap = BitmapFactory.decodeFile(path, options);
		
		int width = options.outWidth;
		int height = options.outHeight;
		
		mMap = new SlicedMap(width, height, this);
		mMap.curX = (mMap.width - getMeasuredWidth()) / 2;
		mMap.curY = (mMap.height - getMeasuredHeight()) / 2;
		mTiles = mMap.getVisibleTiles();
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
		
		private View mView;

		public SlicedMap(int originalWidth, int originalHeight, View view) {
			mView = view;

			rows = originalHeight / TILE_SIZE + ((originalHeight % TILE_SIZE == 0) ? 0 : 1);
			columns = originalWidth / TILE_SIZE + ((originalWidth % TILE_SIZE == 0) ? 0 : 1);
			
			width = columns * TILE_SIZE;
			height = rows * TILE_SIZE;
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
			
			Log.d(TAG, "Left: " + left + ", Top: " + top + ", Right: " + right + ", Bottom: " + bottom);
			
			Vector<Tile> tiles = new Vector<Tile>();
			for (int i = top; i <= bottom; i++ ) {
				for (int j = left; j <= right; j++) {
					int tileId = generateId(j, i, mScaleLevel);
					String tilePath = MAP_PATH_PREFIX + "slices/" + mScaleLevel + "/" + tileId + ".jpg";
					//Log.d(TAG, "Add tile: " + tilePath);
					Tile tile = new Tile();
					tile.x = -(curX % TILE_SIZE) + (j - left) * TILE_SIZE;
					tile.y = -(curY % TILE_SIZE) + (i - top) * TILE_SIZE;
					tile.bitmap = getBitmap(tilePath);
					Log.d(TAG, "Tile: x=" + tile.x + ", y=" + tile.y + ", bitmap=" + tile.bitmap);
					tiles.add(tile);
				}
			}
			
			return tiles;
		}
		
		private Bitmap getBitmap(String tilePath) {
			return BitmapFactory.decodeFile(tilePath);
		}

		private static final int __10_MASK = ((1 << 20) - 1);
		
		private int generateId(int x, int y, int level) {
			return (x & __10_MASK) | ((y & __10_MASK) << 10) | ((level & 0xff) << 20);
		}
	}
}
