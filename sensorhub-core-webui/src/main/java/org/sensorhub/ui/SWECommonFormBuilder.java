/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import org.vast.cdm.common.DataComponent;
import org.vast.data.DataArray;
import org.vast.data.DataChoice;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import org.vast.sweCommon.SweConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;


public class SWECommonFormBuilder
{
    boolean showArrayTable;
    
    
    public Panel buildForm(DataComponent dataComponent)
    {
        FormLayout layout = new FormLayout();
        
        
        
        
        
        Panel panel = new Panel();
        panel.setContent(layout);
        return panel;
    }
    
    
    protected Component buildWidget(DataComponent dataComponent)
    {
        if (dataComponent instanceof DataGroup)
        {
            DataGroup dataGroup = (DataGroup)dataComponent;
            FormLayout layout = new FormLayout();
            
            for (int i = 0; i < dataGroup.getComponentCount(); i++)
            {
                DataComponent c = dataGroup.getComponent(i);
                Component w = buildWidget(c);
                layout.addComponent(w);
            }
            
            Panel panel = new Panel();
            panel.setCaption(getPrettyName(dataComponent));
            panel.setContent(layout);
            return panel;
        }
        
        else if (dataComponent instanceof DataArray)
        {
            DataArray dataArray = (DataArray)dataComponent;
            VerticalLayout layout = new VerticalLayout();
            layout.addComponent(new Label(getPrettyName(dataArray)));
            layout.addComponent(buildWidget(dataArray.getArrayComponent()));
            return layout;
        }
        
        else if (dataComponent instanceof DataChoice)
        {
            
        }
        
        else if (dataComponent instanceof DataValue)
        {
            
        }
        
        return null;
    }
    
    
    protected String getPrettyName(DataComponent dataComponent)
    {
        String label = (String)dataComponent.getProperty(SweConstants.NAME);
        if (label == null)
            label = dataComponent.getName();
        return label;
    }
    
}
