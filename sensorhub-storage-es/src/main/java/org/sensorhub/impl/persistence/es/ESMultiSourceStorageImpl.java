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

import java.util.Collection;
import java.util.Iterator;

import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.vast.util.Bbox;

import net.opengis.gml.v32.AbstractFeature;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESMultiSourceStorageImpl extends ESObsStorageImpl implements IMultiSourceStorage<IObsStorage> {

	@Override
	public Collection<String> getProducerIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IObsStorage getDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IObsStorage addDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
