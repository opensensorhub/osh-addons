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
import java.util.List;
import java.util.Map;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServerConfig;
import com.vaadin.annotations.Theme;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;


@Theme("runo")
public class AdminUI extends UI
{
    private static final long serialVersionUID = 4069325051233125115L;
    FormLayout form;
    
    
    @Override
    protected void init(VaadinRequest request)
    {
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setMinSplitPosition(300.0f, Unit.PIXELS);
        splitPanel.setMaxSplitPosition(80.0f, Unit.PERCENTAGE);
        splitPanel.setSplitPosition(500.0f, Unit.PIXELS);
        setContent(splitPanel);
        
        // build left stack
        Accordion stack = new Accordion();
        stack.setHeight(100.0f, Unit.PERCENTAGE);
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        stack.addTab(layout, "General");
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Sensors");
        buildModuleList(layout, SensorConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Storage");
        buildModuleList(layout, StorageConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Services");
        buildModuleList(layout, ServiceConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Processing");
        buildModuleList(layout, ProcessConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Network");
        splitPanel.addComponent(stack);
        
        // build test config form
        form = new FormLayout();
        form.setMargin(true);
        splitPanel.addComponent(form);
        
        new HttpServerConfigForm().buildForm(form, new HttpServerConfig());
    }
    
    
    @SuppressWarnings("serial")
    protected void buildModuleList(Layout layout, Class<?> moduleType)
    {
        Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(true);
        
        table.addContainerProperty("name", String.class, null);
        table.addContainerProperty("id", String.class, null);
        table.addContainerProperty("status", Boolean.class, null);
        table.setColumnHeaders(new String[] {"Module Name", "UUID", "Status"});
        
        final Map<String, ModuleConfig> configMap = new HashMap<String, ModuleConfig>();
        List<ModuleConfig> moduleConfigs = ModuleRegistry.getInstance().getAvailableModules();
        for (ModuleConfig config: moduleConfigs)
        {
            if (config.getClass().equals(moduleType))
            {
                configMap.put(config.id, config);
                table.addItem(new Object[] {config.name, config.id, config.enabled}, config.id);
            }
        }
        
        table.addItemClickListener(new ItemClickListener()
        {
            @Override
            public void itemClick(ItemClickEvent event)
            {
                form.removeAllComponents();
                String uuid = (String)event.getItem().getItemProperty("id").getValue();
                new GenericConfigForm().buildForm(form, configMap.get(uuid));
            }            
        });
        
        layout.addComponent(table);
    }
    
    
    protected void openModuleConfig(ModuleConfig config)
    {
        
    }
}
