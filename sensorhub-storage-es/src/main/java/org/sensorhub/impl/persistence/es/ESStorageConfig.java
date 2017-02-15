/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import org.sensorhub.api.config.DisplayInfo;

/**
 * <p>
 * Configuration class for ES basic storage
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESStorageConfig extends org.sensorhub.api.persistence.ObsStorageConfig {
	@DisplayInfo(desc="Scroll timeout, in ms")
    public int scrollTimeOut = 6000;
	
	@DisplayInfo(desc="Max of hits will be returned for each scroll")
    public int maxScrollHits = 2;
}
