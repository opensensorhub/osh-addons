/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import org.sensorhub.api.module.IModule;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * IProcess
 * </p>
 *
 * <p><b>Description:</b><br/>
 * TODO: Base interface for all data processing modules run on the system
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface IProcess extends IModule<ProcessConfig>
{
    /**
     * @return inputs descriptor
     */
    public DataComponent getInputList();


    /**
     * @return outputs descriptor
     */
    public DataComponent getOutputList();
    
    
    /**
     * @return parameters descriptor
     */
    public DataComponent getParameterList();
}
