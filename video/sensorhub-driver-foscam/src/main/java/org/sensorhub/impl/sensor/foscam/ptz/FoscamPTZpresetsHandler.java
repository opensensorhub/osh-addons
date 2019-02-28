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
 * Helper class for handling PTZ presets
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamPTZpresetsHandler
{
    Map<String, FoscamPTZpreset> presetMap;
    
    
    public FoscamPTZpresetsHandler(FoscamPTZconfig config)
    {
        this.presetMap = new LinkedHashMap<String, FoscamPTZpreset>();
        
        for (FoscamPTZpreset preset: config.presets)
            presetMap.put(preset.name, preset);
    }
    
    
    public synchronized FoscamPTZpreset getPreset(String name)
    {
        return presetMap.get(name);
    }
    
    
    public synchronized Collection<String> getPresetNames()
    {
        return presetMap.keySet();
    }
}
