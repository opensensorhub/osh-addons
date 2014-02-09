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

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.persistence.IDataStorage;
import org.sensorhub.impl.SensorHub;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


public class ModuleTypeSelectionPopup extends Window
{
    private static final long serialVersionUID = -5368554789542357015L;


    @SuppressWarnings("serial")
    public ModuleTypeSelectionPopup(Class<?> moduleType)
    {
        super("Select Module Type");
        VerticalLayout layout = new VerticalLayout();
        
        // generate table with module list
        Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(true);
        
        table.addContainerProperty("name", String.class, null);
        table.addContainerProperty("class", String.class, null);
        table.setColumnHeaders(new String[] {"Module Type", "Implementing Class"});
                
        final Map<Object, Class<?>> configMap = new HashMap<Object, Class<?>>();
        for (IModuleProvider provider: SensorHub.getInstance().getModuleRegistry().getInstalledModuleTypes())
        {
            Class<?> configClass = provider.getModuleConfigClass();
            if (configClass.equals(moduleType))
            {
                Object id = table.addItem(new Object[] {provider.getModuleTypeName(), provider.getModuleClass().getCanonicalName()}, null);
                configMap.put(id, configClass);
            }
        }
        table.addItem(new Object[] {"PERST Storage", IDataStorage.class.getCanonicalName()}, null);
        layout.addComponent(table);
        
        // add OK button
        Button okButton = new Button("OK");
        okButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                
            }
        });
        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);
        
        setContent(layout);
        center();
    }
}
