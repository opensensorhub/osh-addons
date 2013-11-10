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

package org.sensorhub.gwt.client;

import org.sensorhub.impl.service.HttpServerConfig;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class SensorHub_WebUI implements EntryPoint
{
    interface Binder extends UiBinder<DockLayoutPanel, SensorHub_WebUI> { }
    private static final Binder binder = GWT.create(Binder.class);

    interface Driver extends SimpleBeanEditorDriver<HttpServerConfig, HttpServerConfigEditor> {} 
    Driver driver = GWT.create(Driver.class);
    
    
    @UiField HttpServerConfigEditor config;

    
    /**
     * This is the entry point method.
     */
    public void onModuleLoad()
    {
        DockLayoutPanel outer = binder.createAndBindUi(this);

        // get rid of scrollbars, and clear out the window's built-in margin
        Window.enableScrolling(false);
        Window.setMargin("0px");
        
        // add the outer panel to the RootLayoutPanel, so that it will be displayed.
        RootLayoutPanel root = RootLayoutPanel.get();
        root.add(outer);
        root.forceLayout();
        
        driver.initialize(config);
        driver.edit(new HttpServerConfig());
        
    }
        
}
