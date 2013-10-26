package com.autonavi.indoor;

public final class IndoorPoint {
	final private double x;
	final private double y;
	final private int floor;
	
	public IndoorPoint(double x, double y, int floor) {
		this.x = x;
		this.y = y;
		this.floor = floor;
	}

	
	
	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public int getFloor() {
		return floor;
	}

	public IndoorPoint copy() {
		return new IndoorPoint(x, y, floor);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + floor;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndoorPoint other = (IndoorPoint) obj;
		if (floor != other.floor)
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IndoorPoint [x=" + x + ", y=" + y + ", floor=" + floor + "]";
	}

	
}
