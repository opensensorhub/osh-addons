/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
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
