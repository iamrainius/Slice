package com.autonavi.indoor;

public abstract class Map {
	
	protected double mGeoWidth;
	protected double mGeoHeight;
	
	public double getGeoWidth() {
		return mGeoWidth;
	}
	
	public void setGeoWidth(double w) {
		mGeoWidth = w;
	}
	
	public double getGeoHeight() {
		return mGeoHeight;
	}
	
	public void setGeoHeight(double h) {
		mGeoHeight = h;
	}
	
}
