/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;


public interface IModuleStateSaver
{
    public void put(String key, Object value);
    
    public void put(String key, double value);
    
    public void put(String key, int value);
    
    public void startGroup(String name);
    
    public void endGroup();
    
    public void close();
}
