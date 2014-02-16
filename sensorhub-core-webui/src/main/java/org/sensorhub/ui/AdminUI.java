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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.ui.data.MyBeanItem;
import org.sensorhub.ui.data.MyBeanItemContainer;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Item;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;


@Theme("runo")
public class AdminUI extends UI
{
    private static final long serialVersionUID = 4069325051233125115L;
    VerticalLayout configArea;
    
    public static Action ADD_MODULE_ACTION = new Action("Add Module", new ClassResource("/icons/module_add.png"));
    public static Action REMOVE_MODULE_ACTION = new Action("Remove Module", new ClassResource("/icons/module_delete.png"));
    public static Action ENABLE_MODULE_ACTION = new Action("Enable", new ClassResource("/icons/enable.png"));
    public static Action DISABLE_MODULE_ACTION = new Action("Disable", new ClassResource("/icons/disable.gif"));
    public static Resource MODULE_SETTINGS_ICON = new ClassResource("/icons/brick.png");
    
    
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
        stack.addTab(layout, "General");
        buildGeneralConfig(layout);
        
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
        
        // init config area
        configArea = new VerticalLayout();
        configArea.setMargin(true);
        splitPanel.addComponent(configArea);
    }
    
    
    protected void buildGeneralConfig(VerticalLayout layout)
    {
        // add config objects to container
        MyBeanItemContainer<ModuleConfig> container = new MyBeanItemContainer<ModuleConfig>(ModuleConfig.class);
        container.addBean(new HttpServerConfig());        
        displayModuleList(layout, container, null);
    }
    
    
    protected void buildModuleList(VerticalLayout layout, final Class<?> configType)
    {
        // add config objects to container
        List<ModuleConfig> moduleConfigs = SensorHub.getInstance().getModuleRegistry().getAvailableModules();
        MyBeanItemContainer<ModuleConfig> container = new MyBeanItemContainer<ModuleConfig>(ModuleConfig.class);
        for (ModuleConfig config: moduleConfigs)
        {
            if (configType.isAssignableFrom(config.getClass()))
                container.addBean(config);
        }
        
        displayModuleList(layout, container, configType);
    }
    
    
    @SuppressWarnings("serial")
    protected void displayModuleList(VerticalLayout layout, MyBeanItemContainer<ModuleConfig> container, final Class<?> configType)
    {
        // create table to display module list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(false);
        table.setContainerDataSource(container);
        table.setVisibleColumns(new Object[] {"name", "id", "enabled"});
        table.setColumnHeaders(new String[] {"Module Name", "UUID", "Enabled"});
        
        // item click listener to display selected module settings
        table.addItemClickListener(new ItemClickListener()
        {
            @Override
            public void itemClick(ItemClickEvent event)
            {
                openModuleInfo((MyBeanItem<ModuleConfig>)event.getItem());
            }            
        });        
        layout.addComponent(table);
        
        // context menu
        table.addActionHandler(new Handler() {
            @Override
            public Action[] getActions(Object target, Object sender)
            {
                List<Action> actions = new ArrayList<Action>(10);
                                
                if (target != null)
                {                    
                    boolean enabled = ((BeanItem<ModuleConfig>)table.getItem(target)).getBean().enabled;
                    if (enabled)
                        actions.add(DISABLE_MODULE_ACTION);
                    else
                        actions.add(ENABLE_MODULE_ACTION);
                    actions.add(REMOVE_MODULE_ACTION);
                }
                else
                {
                    if (configType != null)
                        actions.add(ADD_MODULE_ACTION);
                }
                
                return actions.toArray(new Action[0]);
            }
            @Override
            public void handleAction(Action action, Object sender, Object target)
            {
                final ModuleConfig selectedModule = (ModuleConfig)table.getValue();
                if (selectedModule == null)
                    return;
                final Item item = table.getItem(selectedModule);
                final String moduleId = selectedModule.id;
                
                if (action == ADD_MODULE_ACTION)
                {
                    // show popup to select among available module types
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(configType);
                    popup.setModal(true);
                    AdminUI.this.addWindow(popup);
                }
                else if (action == REMOVE_MODULE_ACTION)
                {
                    final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to remove module " + selectedModule.name + "?</br>All settings will be lost.");
                    popup.addCloseListener(new CloseListener() {
                        @Override
                        public void windowClose(CloseEvent e)
                        {
                            if (popup.isConfirmed())
                            {                    
                                try
                                {
                                    SensorHub.getInstance().getModuleRegistry().destroyModule(moduleId);
                                    table.removeItem(selectedModule);
                                }
                                catch (SensorHubException ex)
                                {                        
                                    Notification.show("Error", "The module could not be removed", Notification.Type.ERROR_MESSAGE);
                                }
                            }
                        }                        
                    });                    
                    
                    AdminUI.this.addWindow(popup);
                                       
                }
                else if (action == ENABLE_MODULE_ACTION)
                {
                    final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to enable module " + selectedModule.name + "?");
                    popup.addCloseListener(new CloseListener() {
                        @Override
                        public void windowClose(CloseEvent e)
                        {
                            if (popup.isConfirmed())
                            {                    
                                try 
                                {
                                    SensorHub.getInstance().getModuleRegistry().enableModule(moduleId);
                                    item.getItemProperty("enabled").setValue(true);
                                }
                                catch (SensorHubException ex)
                                {
                                    Notification.show("Error", "The module could not be enabled", Notification.Type.ERROR_MESSAGE);
                                }
                            }
                        }                        
                    });                    
                    
                    AdminUI.this.addWindow(popup);
                }
                else if (action == DISABLE_MODULE_ACTION)
                {
                    final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to disable module " + selectedModule.name + "?");
                    popup.addCloseListener(new CloseListener() {
                        @Override
                        public void windowClose(CloseEvent e)
                        {
                            if (popup.isConfirmed())
                            {                    
                                try 
                                {
                                    SensorHub.getInstance().getModuleRegistry().disableModule(moduleId);
                                    item.getItemProperty("enabled").setValue(false);
                                }
                                catch (SensorHubException ex)
                                {
                                    Notification.show("Error", "The module could not be disabled", Notification.Type.ERROR_MESSAGE);
                                }
                            }
                        }                        
                    });                    
                    
                    AdminUI.this.addWindow(popup);
                }
            }
        });
        
        layout.setSizeFull();
    }
    
    
    @SuppressWarnings("serial")
    protected void openModuleInfo(MyBeanItem<ModuleConfig> beanItem)
    {
        configArea.removeAllComponents();
        
        //SplitPanel panel = new SplitPanel();
        
        IModuleConfigFormBuilder<ModuleConfig> configForm = new GenericConfigFormBuilder();
                
        // create panel with module name
        String moduleType = configForm.getTitle(beanItem.getBean());
        Panel panel = new Panel(moduleType);
        configArea.addComponent(panel);
        
        // add generated form
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeUndefined();
        layout.setMargin(true);
        FormLayout form = new FormLayout();
        final FieldGroup fieldGroup = new FieldGroup(beanItem);
        configForm.buildForm(form, fieldGroup);
        layout.addComponent(form);
        
        // add save button
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidth(100.0f, Unit.PERCENTAGE);
        //buttonsLayout.setSizeFull();
        buttonsLayout.setMargin(true);
        buttonsLayout.setSpacing(true);
        layout.addComponent(buttonsLayout);
        
        Button saveButton = new Button("Save");
        saveButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    fieldGroup.commit();
                }
                catch (CommitException e)
                {
                    e.printStackTrace();
                }                
            }
        });
        buttonsLayout.addComponent(saveButton);
        //buttonsLayout.setComponentAlignment(saveButton, Alignment.MIDDLE_CENTER);
        
        // add cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                fieldGroup.discard();
            }
        });
        buttonsLayout.addComponent(cancelButton);
        //buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_CENTER);
        
        panel.setContent(layout);
    }
}
