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

package org.sensorhub.ui.data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.VaadinPropertyDescriptor;


/**
 * <p>
 * Extended bean item to also generate properties for public fields
 * (i.e. even without getter and setter methods)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 24, 2013
 */
@SuppressWarnings("serial")
public class MyBeanItem<BT> extends BeanItem<BT>
{

    public MyBeanItem(BT bean)
    {
        this(bean, new String[0]);
    }


    public MyBeanItem(BT bean, String... propertyIds)
    {
        super(bean);
        
        // clear all properties added by parent
        //for (Object id: this.getItemPropertyIds())
        //    this.removeItemProperty(id);
        
        // recreate bean information
        Map<String, VaadinPropertyDescriptor<BT>> pds = getPropertyDescriptors((Class<BT>)bean.getClass());
        
        if (propertyIds.length == 0)
        {
            for (VaadinPropertyDescriptor<BT> pd: pds.values())
                addItemProperty(pd.getName(), pd.createProperty(bean));
        }
        else
        {
            for (Object id : propertyIds)
            {
                VaadinPropertyDescriptor<BT> pd = pds.get(id);
                if (pd != null)
                    addItemProperty(pd.getName(), pd.createProperty(bean));
            }
        }
    }


    protected static <BT> Map<String, VaadinPropertyDescriptor<BT>> getPropertyDescriptors(final Class<BT> beanClass)
    {
        final Map<String, VaadinPropertyDescriptor<BT>> pdMap = new LinkedHashMap<String, VaadinPropertyDescriptor<BT>>();

        // try to introspect, if it fails, we just have an empty Item
        try
        {
            List<Field> javaFields = getVisibleFields(beanClass);
            for (Field f: javaFields)
            {
                VaadinPropertyDescriptor<BT> vaadinPropertyDescriptor = new FieldPropertyDescriptor<BT>(f.getName(), f.getType(), f);
                pdMap.put(f.getName(), vaadinPropertyDescriptor);
            }
        }
        catch (Exception ignored)
        {
        }

        return pdMap;
    }
    
    
    protected static <BT> List<Field> getVisibleFields(Class<BT> beanClass)
    {
        List<Field> selectedFields = new ArrayList<Field>(50);
        collectFields(selectedFields, beanClass);
        return selectedFields;
    }
    
    
    protected static <BT> void collectFields(List<Field> selectedFields, Class<?> clazz)
    {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null)
            collectFields(selectedFields, superClass);
        
        for (Field f: clazz.getDeclaredFields())
        {
            // skip complex sub objects for now
            // TODO handle complex properties
            if (f.getType() == List.class)
                return;
            if (f.getType() == Map.class)
                return;
            
            if ((f.getModifiers() & Modifier.PUBLIC) != 0)
                selectedFields.add(f);
        }
    }

}
