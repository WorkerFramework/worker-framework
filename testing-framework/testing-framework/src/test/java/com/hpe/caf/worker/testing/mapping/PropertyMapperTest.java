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
package com.hpe.caf.worker.testing.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerTask;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.hpe.caf.worker.keyview.KeyviewWorkerTask;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.azeckoski.reflectutils.ClassFields;
import org.azeckoski.reflectutils.ReflectUtils;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by ploch on 14/04/2017.
 */
public class PropertyMapperTest
{

    @Test
    public void testInspector() throws Exception
    {
        //Iterable<Property> properties = ClassInspector.getProperties(EntityExtractWorkerTask.class, null);

        //System.out.print(properties.toString());
    }

    @Test
    public void name() throws Exception
    {
        BinaryHashWorkerTask task = new BinaryHashWorkerTask();

        BinaryHashWorkerTask random = EnhancedRandom.random(BinaryHashWorkerTask.class);
        DocumentWorkerTask task1 = EnhancedRandom.random(DocumentWorkerTask.class);

        task.sourceData = ReferencedData.getReferencedData("abc");
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.convertValue(task, Map.class);

        for (Object o : map.entrySet()) {
            System.out.print(1);
        }
    }

    @Test
    public void propertiesTest() throws Exception
    {
        Class<BinaryHashWorkerTask> taskClass = BinaryHashWorkerTask.class;



        ReflectUtils utils = ReflectUtils.getInstance();

        utils.setFieldFindMode(ClassFields.FieldFindMode.ALL);
        KeyviewWorkerTask random = EnhancedRandom.random(KeyviewWorkerTask.class);

        Map<String, Object> task = utils.map(random, 999, null, false, false, "task");
        processClass(taskClass);
        /*Field[] declaredFields = taskClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {

            String name = declaredField.getName();
            Class<?> type = declaredField.getType();
            System.out.println(name + ": " + type.getName());
        }*/
    }

    private void processClass(Class clazz)
    {
        /*PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clazz);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            Class<?> propertyType = propertyDescriptor.getPropertyType();
            System.out.println(name + ": " + propertyType.getName());
            processClass(clazz);
        }*/

        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            String name = declaredField.getName();
            Class<?> type = declaredField.getType();
            System.out.println(name + ": " + type.getName());
            processClass(type);
        }
    }

    private void processClass2(Class clazz, ReflectUtils utils)
    {
        /*PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clazz);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            Class<?> propertyType = propertyDescriptor.getPropertyType();
            System.out.println(name + ": " + propertyType.getName());
            processClass(clazz);
        }*/



        Map<String, Class<?>> fieldTypes = utils.getFieldTypes(clazz);
        for (Map.Entry<String, Class<?>> entry : fieldTypes.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue().getName());
            processClass2(entry.getValue(), utils);
        }
    }
}
