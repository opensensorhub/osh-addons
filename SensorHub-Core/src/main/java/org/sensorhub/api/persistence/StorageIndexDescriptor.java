package org.sensorhub.api.persistence;

public class StorageIndexDescriptor
{
	public enum IndexType
	{
		RTREE,
		QTREE,
		BTREE
	}
	
	public String id;
	
	public IndexType type;
	
	public String fieldPath;
}
