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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.MethodProperty;


/**
 * <p>
 * Custom bean item to also generate properties for public fields
 * (i.e. even without getter and setter methods)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @param <BeanType> Type of java bean wrapped by this class
 * @since Nov 24, 2013
 */
@SuppressWarnings("serial")
public class MyBeanItem<BeanType> implements Item
{
    BeanType bean;
    Map<Object, Property<?>> properties = new LinkedHashMap<Object, Property<?>>();
    
    
    public MyBeanItem(BeanType bean)
    {
        this(bean, new String[0]);
    }


    public MyBeanItem(BeanType bean, String... propertyIds)
    {
        this.bean = bean;
        
        // use reflection to generate properties from class attributes
        addProperties("", bean);
    }
    
    
    protected void addProperties(String prefix, Object bean)
    {
        //System.out.println(bean.getClass());
        addFieldProperties(prefix, bean);
        addMethodProperties(prefix, bean);
    }
    
    
    protected void addFieldProperties(String prefix, Object bean)
    {
        for (Field f: getFields(bean.getClass(), Modifier.PUBLIC))
        {
            String fullName = prefix + f.getName();
            Class<?> fieldType = f.getType();
            
            // case of simple types
            if (isSimpleType(f))
            {
                //System.out.println("field " + fullName);
                addItemProperty(fullName, new FieldProperty(bean, f));
            }
            
            // case of collections
            else if (Collection.class.isAssignableFrom(fieldType))
            {
                try
                {
                    MyBeanItemContainer<Object> container = new MyBeanItemContainer<Object>(Object.class);
                    Collection<?> listObj = (Collection<?>)f.get(bean);
                    if (listObj != null)
                    {
                        for (Object o: listObj)
                            container.addBean(o);
                    }
                    
                    addItemProperty(fullName, new ContainerProperty(bean, f, container));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            
            // case of maps
            /*else if (Map.class.isAssignableFrom(fieldType))
            {
                try
                {
                    Class<Object> beanType = Object.class;//f.getType().getTypeParameters()
                    MyBeanItemContainer<Object> container = new MyBeanItemContainer<Object>(beanType);
                    Map<?,?> mapObj = (Map<?,?>)f.get(bean);
                    for (Object o: mapObj.values())
                        container.addBean(o);
                    addItemProperty(fullName, new ContainerProperty(bean, f, container));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }*/
            
            // case of nested objects
            else
            {
                try
                {
                    Object complexVal = f.get(bean);
                    if (complexVal != null)
                        addProperties(fullName + ".", complexVal);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    @SuppressWarnings("rawtypes")
    protected void addMethodProperties(String prefix, Object bean)
    {
        Map<String, Field> fieldMap = new HashMap<String, Field>();
        for (Field f: getFields(bean.getClass(), Modifier.PROTECTED | Modifier.PRIVATE))
            fieldMap.put(f.getName(), f);
        
        for (PropertyDescriptor prop: getGettersAndSetters(bean.getClass()))
        {
            if (!fieldMap.containsKey(prop.getName()))
                continue;
            
            String fullName = prefix + prop.getName();
            
            //System.out.println(prop.getName() + ", get=" + prop.getReadMethod() + ", set=" + prop.getWriteMethod() + ", hidden=" + prop.isHidden());
            addItemProperty(fullName, new MethodProperty(prop.getPropertyType(), bean, prop.getReadMethod(), prop.getWriteMethod()));
        }
    }
    
    
    protected boolean isSimpleType(Field f)
    {
        if (f.getType().isPrimitive())
            return true;
        
        if (Number.class.isAssignableFrom(f.getType()))
            return true;
        
        if (f.getType() == String.class)
            return true;
        
        return false;
    }
    
    
    public BeanType getBean()
    {
        return bean;
    }
    
    
    protected List<Field> getFields(Class<?> beanClass, int modifier)
    {
        List<Field> selectedFields = new ArrayList<Field>(50);
        collectFields(selectedFields, beanClass, modifier);
        return selectedFields;
    }
    
    
    protected void collectFields(List<Field> selectedFields, Class<?> clazz, int modifier)
    {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null)
            collectFields(selectedFields, superClass, modifier);
        
        for (Field f: clazz.getDeclaredFields())
        {
            if ((f.getModifiers() & modifier) != 0)
                selectedFields.add(f);
        }
    }
    
    
    protected PropertyDescriptor[] getGettersAndSetters(Class<?> beanClass)
    {
        try
        {
            return Introspector.getBeanInfo(beanClass).getPropertyDescriptors();
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }


    @Override
    public Property<?> getItemProperty(Object id)
    {
        return properties.get(id);
    }


    @Override
    public Collection<?> getItemPropertyIds()
    {
        return properties.keySet();
    }


    @Override
    @SuppressWarnings("rawtypes")
    public boolean addItemProperty(Object id, Property property) throws UnsupportedOperationException
    {
        properties.put(id, property);
        return true;
    }


    @Override
    public boolean removeItemProperty(Object id) throws UnsupportedOperationException
    {
        return (properties.remove(id) != null);
    }

}
