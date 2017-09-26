package org.sensorhub.impl.sensor.mesh;

import java.util.ArrayList;
import java.util.List;

public class MeshRecord
{
	long timeUtc;
	List<MeshPoint> points = new ArrayList<>();

	class MeshPoint {
		public MeshPoint(float lat, float lon, float val) {
			this.lat = lat;
			this.lon = lon;
			this.value = val;
		}

		float lat;
		float lon;
		float value;
	}
	
	public void addMeshPoint(MeshPoint pt) {
		points.add(pt);
	}
	
	public float [] getLats() {
		float [] lats = new float[points.size()];
		int i=0;
		for (MeshPoint pt: points) {
			lats[i++] = pt.lat;
		}
		return lats;
	}
	
	public float [] getLons() {
		float [] lons = new float[points.size()];
		int i=0;
		for (MeshPoint pt: points) {
			lons[i++] = pt.lon;
		}
		return lons;
	}

	public float [] getValues() {
		float [] meshs = new float[points.size()];
		int i=0;
		for (MeshPoint pt: points) {
			meshs[i++] = pt.value;
		}
		return meshs;
	}

}
