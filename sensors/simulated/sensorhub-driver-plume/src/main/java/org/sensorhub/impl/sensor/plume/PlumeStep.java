package org.sensorhub.impl.sensor.plume;

public class PlumeStep {
	double time;
	int numParticles;
	double[][]  points;
	double [] points1d;

	public PlumeStep(double time, int numParticles, double[][] points) {
		this.time = time;
		this.numParticles = numParticles;
		this.points = points;
	}

	public PlumeStep(double time, int numParticles, double[] points) {
		this.time = time;
		this.numParticles = numParticles;
		this.points1d = points;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public int getNumParticles() {
		return numParticles;
	}

	public void setNumParticles(int numParticles) {
		this.numParticles = numParticles;
	}

	public double[][] getPoints() {
		return points;
	}

	public void setPoints(double[][] points) {
		this.points = points;
	}
	
	public double[] getPoints1d() {
		return points1d;
	}

}
