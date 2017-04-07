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
package com.github.workerframework;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Strings;
import com.hpe.caf.worker.document.DocumentWorkerFieldChanges;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.impl.FieldsImpl;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.hpe.caf.worker.textextract.*;
import com.hpe.caf.worker.textextract.context.DocumentContextProvider;
import com.hpe.caf.worker.textextract.process.OperationExecutor;
import com.hpe.caf.worker.textextract.process.OperationRegisterFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        try {
            testFiles(args[0], args[1]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testFiles(String filesPath, String sdkPath) throws Exception
    {
        Path testFilesLocation = Paths.get(filesPath).toAbsolutePath();
        System.out.println("Using location for files: " + testFilesLocation.toString());
        List<Path> files = getFiles(testFilesLocation, false);

        if (Strings.isNullOrEmpty(sdkPath)) {
            sdkPath = "KeyViewSDK\\WINDOWS_X86_64\\bin";

        }
        Path sdkFullPath = Paths.get(sdkPath).toAbsolutePath();

        sdkPath = sdkFullPath.toString();
        System.out.println("Using SDK located: " + sdkPath);
        if (!Files.exists(sdkFullPath)) {
            System.out.println("SDK folder not found");
            return;
        }
        for (Path file : files) {
            testDocumentAndSaveResult(file, sdkPath);
        }
    }

    private static List<Path> getFiles(Path directory, boolean includeSubFolders) throws IOException
    {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {

                if (Files.isDirectory(path)){
                    if (includeSubFolders) {
                        fileNames.addAll(getFiles(path, includeSubFolders));
                    }
                }
                else {

                    if (!path.getFileName().toString().endsWith(".result")) {
                        fileNames.add(path);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
            throw ex;
        }
        return fileNames;
    }

    public static void testDocumentAndSaveResult(Path testFilePath, String sdkLocation) throws Exception
    {
        TestServices services = TestServices.createDefault();

        KeyviewConfiguration keyviewConfig = new KeyviewConfiguration();
        Path sdkPath = Paths.get(sdkLocation).toAbsolutePath().normalize();
        keyviewConfig.setLibPath(sdkPath.toString());
        //keyviewConfig.setLibPath("C:\\git\\caf\\worker-textextract\\KeyViewSDK\\WINDOWS_X86_64\\bin");

        TextExtractWorkerConfiguration extractWorkerConfiguration = new TextExtractWorkerConfiguration();
        extractWorkerConfiguration.setExtractChildren(true);
        extractWorkerConfiguration.setExtractMetadata(true);
        extractWorkerConfiguration.setExtractText(true);
        extractWorkerConfiguration.setGenerateHashes(true);
        extractWorkerConfiguration.setContentSizeThreshold(1024);
        extractWorkerConfiguration.setDiskBackedSize(102400);
        extractWorkerConfiguration.setExtractToMemoryLimit(1024);
        extractWorkerConfiguration.setExtractPath("C:\\temp\\kv");

        services.getConfigurationSource().addConfiguration(keyviewConfig).addConfiguration(extractWorkerConfiguration);

        // String testFileLocation = WorkerTextExtractTestConstants.ZIP_FILE_PATH;
        //   Path testFilePath = Paths.get(testFileLocation);

        String reference = services.getDataStore().store(testFilePath, null);

        Document document = DocumentBuilder.configure()
                .withServices(services)
                .withFields()
                .addFieldValue(WorkerTextExtractConstants.CustomData.STORAGE_REFERENCE, reference)
                .addFieldValue(WorkerTextExtractConstants.CustomData.ORIGINAL_FILENAME, testFilePath.getFileName().toString()).documentBuilder().build();

        OperationExecutor executor = new OperationExecutor(OperationRegisterFactory.createDefault());
        DocumentContextProvider contextProvider = new DocumentContextProvider();

        //     DocumentContext context = contextProvider.createContext(document);

        //    executor.performOperation(OperationNames.EXTRACT_FILE_TYPE, context);
        //   executor.<DocumentInfo>performOperation(OperationNames.EXTRACT_CHILDREN, context, t ->  process(t));


        WorkerTextExtractFactory factory = new WorkerTextExtractFactory();
        DocumentWorker documentWorker = factory.createDocumentWorker(document.getApplication());
        try {

            documentWorker.processDocument(document);


            YAMLMapper mapper = new YAMLMapper();
            FieldsImpl fields = (FieldsImpl) document.getFields();
            Map<String, DocumentWorkerFieldChanges> fieldsChanges = fields.getChanges();

            mapper.writeValue(new File(testFilePath.toAbsolutePath().toString() + ".result"), fieldsChanges);

           // InMemoryDataStore dataStore = (InMemoryDataStore) services.getDataStore();



            //  Map<String, byte[]> dataMap = dataStore.getDataMap();
          /*  for (Map.Entry<String, byte[]> assetEntry : dataMap.entrySet()) {
                *//*Path path = Paths.get(testFilePath.getParent().toAbsolutePath().toString(), assetEntry.getKey());
                Files.createDirectory(path);*//*
                FileUtils.writeByteArrayToFile(new File(testFilePath.toAbsolutePath().toString() + "." + assetEntry.getKey()), assetEntry.getValue());
            }
            System.out.println(dataMap.size());*/
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    private static void process(DocumentInfo documentInfo)
    {
        for (Map.Entry<String, Set<String>> entry : documentInfo.getMetadata().entrySet()) {
            System.out.println("Field: " + entry.getKey());
            Set<String> values = entry.getValue();
            for (String value : values) {
                System.out.println("\t" + value);
            }
            System.out.println("---------------");
        }


    }
}
