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
package com.hpe.caf.worker.testing.util;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Created by ploch on 18/04/2017.
 */
public class DirectoryWalkerTest
{
    @Test
    public void testWalkPath() throws Exception
    {
        Path testDataRootPath = TestFilesUtil.getTestDataPath("dir-walker");
        ArrayList<String> files = new ArrayList<>();


        DirectoryWalker.walkPath(testDataRootPath, "*.*", "*.test.{descriptor,expectation}", path -> {
            Path relativePath = testDataRootPath.relativize(path);
            files.add(relativePath.toString());
        });
/*        StringBuilder sb = new StringBuilder();

        for (String file : files) {
            System.out.print("");

            String s = "\"" + file.replace("\\", "\\\\") + "\", ";
            sb.append(s);
        }
        System.out.println(sb.toString());*/

        assertThat(files, contains("flat\\test1.txt", "flat\\test2.txt", "in-folders\\test-1\\test1.txt", "in-folders\\test-2\\test2.txt"));
    }

    @Test
    public void testWalkStream() throws Exception
    {
        Path testDataRootPath = TestFilesUtil.getTestDataPath("dir-walker");
        List<String> files = DirectoryWalker.walk(testDataRootPath, "*.*", "*.test.{descriptor,expectation}")
                .filter(path -> !Files.isDirectory(path))
                .map(path -> testDataRootPath.relativize(path).toString()).collect(Collectors.toList());

        assertThat(files, contains("flat\\test1.txt", "flat\\test2.txt", "in-folders\\test-1\\test1.txt", "in-folders\\test-2\\test2.txt"));
    }
}
