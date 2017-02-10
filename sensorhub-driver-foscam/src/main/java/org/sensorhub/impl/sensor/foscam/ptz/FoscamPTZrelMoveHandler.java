/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam.ptz;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * <p>
 * Helper class for handling PTZ relative movements
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamPTZrelMoveHandler
{
    Map<String, FoscamPTZrelMove> relMoveMap;
    
    
    public FoscamPTZrelMoveHandler(FoscamPTZconfig config)
    {
        this.relMoveMap = new LinkedHashMap<String, FoscamPTZrelMove>();
        
        for (FoscamPTZrelMove relMove: config.relMoves)
            relMoveMap.put(relMove.name, relMove);
    }
    
    
    public synchronized FoscamPTZrelMove getRelMove(String name)
    {
        return relMoveMap.get(name);
    }
    
    
    public synchronized Collection<String> getRelMoveNames()
    {
        return relMoveMap.keySet();
    }
}
