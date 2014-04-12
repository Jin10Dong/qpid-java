/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class ConfiguredObjectAttributeOrStatistic<C extends ConfiguredObject, T>
{

    protected final String _name;
    protected final Class<T> _type;
    protected final AttributeValueConverter<T> _converter;
    protected final Method _getter;

    ConfiguredObjectAttributeOrStatistic(final Method getter)
    {

        _getter = getter;
        _type = (Class<T>) getTypeFromMethod(getter);
        _name = getNameFromMethod(getter, _type);
        _converter = AttributeValueConverter.getConverter(_type, getter.getGenericReturnType());

    }

    private static String getNameFromMethod(final Method m, final Class<?> type)
    {
        String methodName = m.getName();
        String baseName;

        if(type == Boolean.class )
        {
            if((methodName.startsWith("get") || methodName.startsWith("has")) && methodName.length() >= 4)
            {
                baseName = methodName.substring(3);
            }
            else if(methodName.startsWith("is") && methodName.length() >= 3)
            {
                baseName = methodName.substring(2);
            }
            else
            {
                throw new IllegalArgumentException("Method name " + methodName + " does not conform to the required pattern for ManagedAttributes");
            }
        }
        else
        {
            if(methodName.startsWith("get") && methodName.length() >= 4)
            {
                baseName = methodName.substring(3);
            }
            else
            {
                throw new IllegalArgumentException("Method name " + methodName + " does not conform to the required pattern for ManagedAttributes");
            }
        }

        String name = baseName.length() == 1 ? baseName.toLowerCase() : baseName.substring(0,1).toLowerCase() + baseName.substring(1);
        name = name.replace('_','.');
        return name;
    }

    private static Class<?> getTypeFromMethod(final Method m)
    {
        Class<?> type = m.getReturnType();
        if(type.isPrimitive())
        {
            if(type == Boolean.TYPE)
            {
                type = Boolean.class;
            }
            else if(type == Byte.TYPE)
            {
                type = Byte.class;
            }
            else if(type == Short.TYPE)
            {
                type = Short.class;
            }
            else if(type == Integer.TYPE)
            {
                type = Integer.class;
            }
            else if(type == Long.TYPE)
            {
                type = Long.class;
            }
            else if(type == Float.TYPE)
            {
                type = Float.class;
            }
            else if(type == Double.TYPE)
            {
                type = Double.class;
            }
            else if(type == Character.TYPE)
            {
                type = Character.class;
            }
        }
        return type;
    }

    public String getName()
    {
        return _name;
    }

    public Class<T> getType()
    {
        return _type;
    }

    public T getValue(C configuredObject)
    {
        try
        {
            return (T) _getter.invoke(configuredObject);
        }
        catch (IllegalAccessException e)
        {
            Object o = configuredObject.getAttribute(_name);
            return _converter.convert(o, configuredObject);
        }
        catch (InvocationTargetException e)
        {
            Object o = configuredObject.getAttribute(_name);
            return _converter.convert(o, configuredObject);
        }

    }

    public Method getGetter()
    {
        return _getter;
    }
}
