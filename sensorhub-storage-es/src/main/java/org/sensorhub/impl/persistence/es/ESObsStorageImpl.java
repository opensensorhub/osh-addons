/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/


package org.sensorhub.impl.persistence.es;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.util.Bbox;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESObsStorageImpl extends AbstractModule<ESStorageConfig> implements IObsStorageModule<ESStorageConfig>
{

	@Override
	public void backup(OutputStream os) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restore(InputStream is) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rollback() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sync(IStorageModule<?> storage) throws StorageException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws SensorHubException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws SensorHubException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractProcess getLatestDataSourceDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeDataSourceDescription(AbstractProcess process) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDataSourceDescription(AbstractProcess process) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDataSourceDescription(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDataSourceDescriptionHistory(double startTime, double endTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, ? extends IRecordStoreInfo> getRecordStores() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getNumRecords(String recordType) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] getRecordsTimeRange(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<double[]> getRecordsTimeClusters(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataBlock getDataBlock(DataKey key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumMatchingRecords(IDataFilter filter, long maxCount) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void storeRecord(DataKey key, DataBlock data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateRecord(DataKey key, DataBlock data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeRecord(DataKey key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int removeRecords(IDataFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumFois(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bbox getFoisSpatialExtent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> getFoiIDs(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<AbstractFeature> getFois(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeFoi(String producerID, AbstractFeature foi) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ESStorageConfig getConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
}
