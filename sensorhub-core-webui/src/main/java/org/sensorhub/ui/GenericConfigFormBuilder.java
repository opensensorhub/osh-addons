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
import org.sensorhub.api.module.ModuleConfig;
import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;


/**
 * <p>
 * Generic form builder based on Vaadin framework
 * This auto-generates widget giving types of properties. 
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Feb 1, 2014
 */
public class GenericConfigFormBuilder implements IModuleConfigFormBuilder<ModuleConfig>
{
    private static final long serialVersionUID = 1916649317564542150L;
    
    public static String ID_PROPERTY = "id";    
    
    List<Field<?>> labels = new ArrayList<Field<?>>();
    List<Field<?>> textBoxes = new ArrayList<Field<?>>();
    List<Field<?>> checkBoxes = new ArrayList<Field<?>>();
    List<Field<?>> otherWidgets = new ArrayList<Field<?>>();
    
    
    public String getTitle(ModuleConfig config)
    {
        return getPrettyName(config.getClass().getSimpleName());
    }
    
    
    @Override
    public void buildForm(FormLayout form, FieldGroup fieldGroup)
    {
        // add widget for each visible attribute
        for (Object propId: fieldGroup.getUnboundPropertyIds())
        {
            Field<?> field = fieldGroup.buildAndBind(getPrettyName((String)propId), propId);
            Property<?> prop = field.getPropertyDataSource();
            
            customizeField((String)propId, prop, field);
            
            if (field instanceof Label)
                labels.add(field);
            else if (field instanceof TextField)
                textBoxes.add(field);
            else if (field instanceof CheckBox)
                checkBoxes.add(field);
            else
                otherWidgets.add(field);
        }
        
        // add all widgets
        for (Field<?> w: labels)
            form.addComponent(w);
        for (Field<?> w: textBoxes)
            form.addComponent(w);
        for (Field<?> w: checkBoxes)
            form.addComponent(w);
        for (Field<?> w: otherWidgets)
            form.addComponent(w);
    }
    
    
    protected void customizeField(String propId, Property<?> prop, Field<?> field)
    {
        if (propId.equals(ID_PROPERTY))
            field.setReadOnly(true);
        
        if (prop.getType().equals(String.class))
            field.setWidth(250, Unit.PIXELS);
        else if (prop.getType().equals(int.class) || prop.getType().equals(Integer.class))
            field.setWidth(50, Unit.PIXELS);
        else if (prop.getType().equals(float.class) || prop.getType().equals(Float.class))
            field.setWidth(50, Unit.PIXELS);
        else if (prop.getType().equals(double.class) || prop.getType().equals(Double.class))
            field.setWidth(50, Unit.PIXELS);
    }
    
    
    protected String getPrettyName(String text)
    {
        StringBuilder buf = new StringBuilder(text);
        for (int i=0; i<buf.length()-1; i++)
        {
            char c = buf.charAt(i);
            
            if (i == 0)
            {
                char newcar = Character.toUpperCase(c);
                buf.setCharAt(i, newcar);
            }
                    
            else if (Character.isUpperCase(c) && Character.isLowerCase(buf.charAt(i+1)))
            {
                buf.insert(i, ' ');
                i++;
            }
        }
        
        return buf.toString();
    }
}
