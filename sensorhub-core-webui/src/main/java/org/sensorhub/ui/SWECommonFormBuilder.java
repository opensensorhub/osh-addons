/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.vast.data.DataValue;
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
        if (dataComponent instanceof DataRecord)
        {
            DataRecord dataRecord = (DataRecord)dataComponent;
            FormLayout layout = new FormLayout();
            
            for (int i = 0; i < dataRecord.getComponentCount(); i++)
            {
                DataComponent c = dataRecord.getComponent(i);
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
            layout.addComponent(buildWidget(dataArray.getElementType()));
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
        String label = dataComponent.getLabel();
        if (label == null)
            label = dataComponent.getName();
        return label;
    }
    
}
