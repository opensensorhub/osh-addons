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

import com.vaadin.data.Validator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;


public class HttpServerConfigForm extends GenericConfigForm
{
    private static final long serialVersionUID = -7803356484824238642L;
    
    
    protected Field<?> buildWidget(java.lang.reflect.Field f, Object obj)
    {
        String fname = f.getName();
        
        try
        {
            if (fname.equals("httpPort"))
            {
                TextField tb = new TextField(fname);
                tb.setWidth(50, Unit.PIXELS);
                tb.setValue(Integer.toString(f.getInt(obj)));
                return tb;
            }
        }
        catch (Exception e)
        {            
        }
            
        return super.buildWidget(f, obj);
    }
    
    
    @Override
    protected void validateFieldValue(Field<?> w)
    {
        // retrieve corresponding java attribute
        java.lang.reflect.Field f = mapComponentToField.get(w);
        
        if (f.getName().equals("httpPort"))
        {
            try
            {
                int portNum = 0;
                portNum = Integer.parseInt(w.getValue().toString());
                if (portNum > 10000 || portNum <= 80)
                    throw new Exception();
            }
            catch (Exception e)
            {
                throw new Validator.InvalidValueException("Port number must be an integer number greater than 80 and lower than 65535");
            }
        }
    }
}
