/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Configuration class for PERST basic storage
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class MVStorageConfig extends org.sensorhub.api.persistence.ObsStorageConfig
{
    
    @DisplayInfo(desc="Memory cache size for page chunks, in KB")
    public int memoryCacheSize = 5*1024;
    
    
    @DisplayInfo(desc="Size of the auto-commit write buffer, in KB")
    public int autoCommitBufferSize = 1024;
    
        
    @DisplayInfo(desc="Set to compress underlying file storage")
    public boolean useCompression = false;
}
