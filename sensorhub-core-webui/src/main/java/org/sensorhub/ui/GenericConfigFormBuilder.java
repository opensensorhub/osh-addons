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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.api.IModuleConfigFormBuilder;
import org.sensorhub.ui.data.ContainerProperty;
import org.sensorhub.ui.data.FieldProperty;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.Runo;


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
    public static String ID_PROPERTY = "id";    
    
    List<Field<?>> labels = new ArrayList<Field<?>>();
    List<Field<?>> textBoxes = new ArrayList<Field<?>>();
    List<Field<?>> checkBoxes = new ArrayList<Field<?>>();
    List<Component> otherWidgets = new ArrayList<Component>();
    
    
    public String getTitle(ModuleConfig config)
    {
        return getPrettyName(config.getClass().getSimpleName());
    }
    
    
    @Override
    public void buildForm(FormLayout form, FieldGroup fieldGroup)
    {
        reset();
        
        // add widget for each visible attribute
        for (Object propId: fieldGroup.getUnboundPropertyIds())
        {
            Property<?> prop = fieldGroup.getItemDataSource().getItemProperty(propId);
            
            if (prop instanceof ContainerProperty)
            {
                String label = ((ContainerProperty)prop).getLabel();
                if (label == null)
                    label = getPrettyName((String)propId);
                
                /*Table table = new Table();
                table.setCaption(label);
                table.setSizeFull();
                table.setPageLength(5);
                table.setHeight(50, Unit.POINTS);
                table.setSelectable(true);
                table.setEditable(true);
                table.setColumnReorderingAllowed(false);
                table.setContainerDataSource(((ContainerProperty)prop).getValue());
                table.setBuffered(true);
                table.setStyleName(Runo.TABLE_SMALL);
                otherWidgets.add(table);*/
                
                if (!((ContainerProperty)prop).getValue().getItemIds().isEmpty())
                {
                    Object firstItemId = ((ContainerProperty)prop).getValue().getItemIds().iterator().next();
                    FormLayout subform = new FormLayout();
                    subform.setCaption(label);
                    FieldGroup newFieldGroup = new FieldGroup(((ContainerProperty)prop).getValue().getItem(firstItemId));
                    new GenericConfigFormBuilder().buildForm(subform, newFieldGroup);
                    otherWidgets.add(subform);
                }
            }
            else
            {
                Field<?> field = null;
                
                try
                {
                    String label = null;
                    if (prop instanceof FieldProperty)
                        label = ((FieldProperty)prop).getLabel();
                    if (label == null)
                        label = getPrettyName((String)propId);
                    field = fieldGroup.buildAndBind(label, propId);
                }
                catch (Exception e)
                {
                    System.err.println("No UI generator for field " + propId);
                    continue;
                }
                
                //Property<?> prop = field.getPropertyDataSource();            
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
        }
        
        // add all widgets
        for (Field<?> w: labels)
            form.addComponent(w);
        for (Field<?> w: textBoxes)
            form.addComponent(w);
        for (Field<?> w: checkBoxes)
            form.addComponent(w);
        for (Component w: otherWidgets)
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
        StringBuilder buf = new StringBuilder(text.substring(text.lastIndexOf('.')+1));
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
    
    
    protected void reset()
    {
        labels.clear();
        textBoxes.clear();
        checkBoxes.clear();
        otherWidgets.clear();
    }
}
