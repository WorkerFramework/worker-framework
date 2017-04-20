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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ploch on 14/04/2017.
 */
public class ClassInspector
{
    public static <T> Iterable<Property> getProperties(Class<T> type, Property parent)
    {
        Set<Property> properties = new HashSet<>();

        Field[] fields = type.getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            boolean isArray = fieldType.isArray();
            if (isArray) {
                fieldType = fieldType.getComponentType();
            }

         //   Property<?> property1 = FieldConverter.createProperty(fieldType, field);

            Property property = new Property(fieldName, fieldType, isArray, parent);
            properties.add(property);

            if (fieldType.isPrimitive()) {
                continue;
            }

            if (fieldType.isEnum()) continue;

            Package fieldTypePackage = fieldType.getPackage();
            if (fieldTypePackage.getName().startsWith("java.lang")) {
                continue;
            }
            getProperties(fieldType, property);
        }
        return properties;
    }


}
