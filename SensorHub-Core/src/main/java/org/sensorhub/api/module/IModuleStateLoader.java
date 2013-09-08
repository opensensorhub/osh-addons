/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.util.Map.Entry;


public interface IModuleStateLoader
{
    public Entry<String, Object> nextEntry();
    
    public Entry<String, String> nextEntryAsString();
    
    public Entry<String, Double> nextEntryAsDouble();
    
    public Entry<String, Integer> nextEntryAsInt();
    
    public String getCurrentGroup();
}
