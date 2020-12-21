/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import java.awt.geom.Point2D;
import org.junit.Test;
import org.sensorhub.impl.SensorHub;


public class TestFakeGpsAlgo
{

    @Test
    public void test() throws Exception
    {
        SensorHub hub = new SensorHub();
        var sensor = new FakeGpsSensor();
        sensor.setParentHub(hub);
        sensor.setConfiguration(new FakeGpsConfig());
        sensor.getConfiguration().id = "blabla";
        sensor.getConfiguration().googleApiKey = "none";
        sensor.init();
        var output = sensor.dataInterface;
        
        var route = output.routes.get(0);
        route.points.add(new Point2D.Double(0.0, 0.0));
        route.points.add(new Point2D.Double(0.1, 0.0));
        route.points.add(new Point2D.Double(0.1, 0.1));
        
        route.assets.get(0).speed = -3600;
        route.assets.get(0).currentTrackPos = 2.0;
        
        for (int i = 0; i < 100; i++)
            output.sendMeasurement(route, route.assets.get(0));
    }

}
