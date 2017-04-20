/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.testing.prototypes;

import java.util.ArrayList;

/**
 * Created by ploch on 14/04/2017.
 */
public class Property<T>
{
    private final String name;
    private final Class<T> type;
    private final boolean isArray;
    private final Property parent;
    private final ArrayList<Property> properties = new ArrayList<>();

    public Property(String name, Class<T> type, boolean isArray, Property parent)
    {
        this.name = name;
        this.type = type;
        this.isArray = isArray;
        this.parent = parent;
        if (parent != null) {
            this.parent.properties.add(this);
        }
    }

    /**
     * Getter for property 'name'.
     *
     * @return Value for property 'name'.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Getter for property 'type'.
     *
     * @return Value for property 'type'.
     */
    public Class<?> getType()
    {
        return type;
    }

    /**
     * Getter for property 'parent'.
     *
     * @return Value for property 'parent'.
     */
    public Property getParent()
    {
        return parent;
    }

    /**
     * Getter for property 'properties'.
     *
     * @return Value for property 'properties'.
     */
    public ArrayList<Property> getProperties()
    {
        return properties;
    }

    /**
     * Getter for property 'array'.
     *
     * @return Value for property 'array'.
     */
    public boolean isArray()
    {
        return isArray;
    }
}
