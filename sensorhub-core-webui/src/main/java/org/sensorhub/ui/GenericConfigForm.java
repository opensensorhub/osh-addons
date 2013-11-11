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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.module.ModuleConfig;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;


public class GenericConfigForm implements IModuleConfigForm<ModuleConfig>
{
    private static final long serialVersionUID = 1916649317564542150L;
    
    FormLayout form;
    ModuleConfig config;
    Map<Field<?>, java.lang.reflect.Field> mapComponentToField = new LinkedHashMap<Field<?>, java.lang.reflect.Field>();
    
    
    @SuppressWarnings("serial")
    @Override
    public void buildForm(FormLayout form, ModuleConfig config)
    {
        this.form = form;
        this.config = config;
        
        for (java.lang.reflect.Field f: getVisibleFields(config))
        {
            final Field<?> w = buildWidget(f, config);
            if (w == null)
                continue;
            
            w.addValidator(new Validator() {
                @Override
                public void validate(Object value) throws InvalidValueException
                {
                    validateFieldValue(w);                 
                }
            });
            
            mapComponentToField.put(w, f);
            form.addComponent(w);
        }
        
        Button saveButton = new Button("Save");
        saveButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                save();                
            }
        });
        form.addComponent(saveButton);
    }
    
    
    @Override
    public void save()
    {
        // forst check than all form fields have valid values
        for (Field<?> w: mapComponentToField.keySet())
            if (!w.isValid())
                return;
        
        // then save them one by one to config
        for (Field<?> w: mapComponentToField.keySet())
            setFieldValue(w);
    }
    
    
    protected List<java.lang.reflect.Field> getVisibleFields(Object obj)
    {
        List<java.lang.reflect.Field> selectedFields = new ArrayList<java.lang.reflect.Field>(50);
        collectFields(selectedFields, obj.getClass());
        return selectedFields;
    }
    
    
    protected void collectFields(List<java.lang.reflect.Field> selectedFields, Class<?> clazz)
    {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null)
            collectFields(selectedFields, superClass);
        
        for (java.lang.reflect.Field f: clazz.getDeclaredFields())
        {
            if ((f.getModifiers() & Modifier.PUBLIC) != 0)
                selectedFields.add(f);
        }
    }
    
    
    protected Field<?> buildWidget(java.lang.reflect.Field f, Object obj)
    {
        Class<?> fieldType = f.getType();
        String fname = f.getName();
        String prettyName = getPrettyName(f);
        
        // skip module class since use should not be able to set it
        if (fname.equals("moduleClass"))
            return null;
        
        try
        {
            if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class))
            {
                CheckBox cb = new CheckBox(prettyName);
                cb.setValue(f.getBoolean(obj));
                return cb;
            }
            
            if (fieldType.equals(int.class) || fieldType.equals(Integer.class))
            {
                TextField tf = new TextField(prettyName);
                tf.setWidth(50, Unit.PIXELS);
                tf.addValidator(new IntegerRangeValidator("Value must be a positive integer", 0, Integer.MAX_VALUE));
                return tf;
            }
            
            if (fieldType.equals(String.class))
            {
                TextField tf = new TextField(prettyName);
                tf.setWidth(250, Unit.PIXELS);
                tf.setValue((String)f.get(obj));
                if (fname.equals("id"))
                    tf.setReadOnly(true);
                return tf;
            }        
            
            return new TextField(prettyName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    
    protected void validateFieldValue(Field<?> w)
    {
        // do nothing in generic implementation
    }


    protected void setFieldValue(Field<?> w)
    {
        // retrieve corresponding java attribute
        java.lang.reflect.Field f = mapComponentToField.get(w);
        
        try
        {
            if (f.getType().equals(boolean.class))
                f.setBoolean(config, (Boolean)w.getValue());
            else if (f.getType().equals(byte.class))
                f.setByte(config, (Byte)w.getValue());
            else if (f.getType().equals(int.class))
                f.setInt(config, (Integer)w.getValue());
            else if (f.getType().equals(long.class))
                f.setLong(config, (Long)w.getValue());
            else if (f.getType().equals(float.class))
                f.setFloat(config, (Float)w.getValue());
            else if (f.getType().equals(double.class))
                f.setDouble(config, (Double)w.getValue());
            else if (f.getType().equals(String.class))
                f.set(config, (String)w.getValue());
            // else TODO
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Error while setting field value", e);
        }
    }
    
    
    protected String getPrettyName(java.lang.reflect.Field f)
    {
        StringBuilder buf = new StringBuilder(f.getName());
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
