/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.common;

import org.sensorhub.api.processing.IProcess;
import org.sensorhub.api.processing.ProcessConfig;


/**
 * <p><b>Title:</b>
 * AbstractSimpleProcess
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base abstract class for simple core processes
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Aug 30, 2013
 */
public abstract class AbstractSimpleProcess implements IProcess
{
    private ProcessConfig config;
    
    
    public AbstractSimpleProcess()
    {
        super();
    }

    
    @Override
    public void init(ProcessConfig config)
    {
        this.config = config;
    }

    
    @Override
    public ProcessConfig getConfiguration()
    {
        return this.config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    } 
}