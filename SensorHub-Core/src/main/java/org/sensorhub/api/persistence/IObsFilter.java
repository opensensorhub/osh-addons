/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.List;
import org.vast.util.TimeExtent;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p><b>Title:</b>
 * ObsFilter
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Simple structure for defining filtering criteria when retrieving observations from storage.
 * These criteria correspond to properties of the O&M model.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface IObsFilter extends IDataFilter
{
    /**
     * Sampling time period from which to select observation
     */
    public TimeExtent getSamplingTimeRange();
    
    
    /**
     * List of features of interest
     */
    public List<String> getFoiIds();
    
    
    /**
     * List of data components/observed properties to retrieve from storage
     */
    public List<String> getObservedProperties();
    
    
    /**
     * Region of interest    
     */
    public Polygon getRoi();
}
