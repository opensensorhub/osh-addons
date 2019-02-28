/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;


public class V4LCameraParams implements Cloneable
{
    public boolean doCapture;
    public String imgFormat = "YUYV";
    public int imgWidth = 640;
    public int imgHeight = 480;
    public int frameRate = 30;
    
    
    @Override
    protected V4LCameraParams clone()
    {
        try
        {
            return (V4LCameraParams)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }
}
