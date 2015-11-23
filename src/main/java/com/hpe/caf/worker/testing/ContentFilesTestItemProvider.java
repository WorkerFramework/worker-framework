package com.hpe.caf.worker.testing;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ploch on 19/11/2015.
 */
public abstract class ContentFilesTestItemProvider implements TestItemProvider {

    private final String inputPath;
    private final String expectedPath;

    public ContentFilesTestItemProvider(){
        this(SettingsProvider.defaultProvider.getSetting(SettingNames.inputFolder), SettingsProvider.defaultProvider.getSetting(SettingNames.expectedFolder));
    }

    public ContentFilesTestItemProvider(final String inputPath, final String expectedPath) {

        this.inputPath = inputPath;
        this.expectedPath = expectedPath;
    }

    @Override
    public Collection<TestItem> getItems() throws Exception {

        List<Path> files = getFiles(Paths.get(inputPath));

        List<TestItem> testItems = new ArrayList<>(files.size());
        for (Path inputFile : files) {

            String fileName = inputFile.getFileName().toString();

            Path expectedFile = Paths.get(expectedPath, fileName + ".result");

            testItems.add(createTestItem(inputFile, expectedFile));
        }
        return testItems;
    }

    protected abstract TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception;

    private static List<Path> getFiles(Path directory) throws IOException {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)){
                    fileNames.addAll(getFiles(path));
                }
                else {
                    fileNames.add(path);
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
            throw ex;
        }
        return fileNames;
    }
}
