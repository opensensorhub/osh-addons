/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.ble;


/**
 * <p>
 * Representation of a Bluetooth GATT Characteristic Descriptor.<br/>
 * <i>This interface was originally modeled on Android's Bluetooth LE API.</i>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public interface IGattDescriptor extends IGattField
{
        
    /**
     * @return the characteristic this descriptor belongs to.
     */
    public IGattField getCharacteristic();
    
}
