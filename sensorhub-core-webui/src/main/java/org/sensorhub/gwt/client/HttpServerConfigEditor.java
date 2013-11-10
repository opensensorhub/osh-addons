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
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.gwt.client;

import org.sensorhub.impl.service.HttpServerConfig;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.ui.client.ValueBoxEditorDecorator;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;


public class HttpServerConfigEditor extends Composite implements Editor<HttpServerConfig>
{
    interface HttpServerConfigEditorUiBinder extends UiBinder<Widget, HttpServerConfigEditor> { }
    private static HttpServerConfigEditorUiBinder uiBinder = GWT.create(HttpServerConfigEditorUiBinder.class);

    
    @UiField
    ValueBoxEditorDecorator<String> httpPort;
    
    @UiField
    ValueBoxEditorDecorator<String> rootURL;
    

    public HttpServerConfigEditor()
    {
        initWidget(uiBinder.createAndBindUi(this));
    }


    @UiHandler("button")
    void onClick(ClickEvent e)
    {
        Window.alert("Hello!");
    }


}
