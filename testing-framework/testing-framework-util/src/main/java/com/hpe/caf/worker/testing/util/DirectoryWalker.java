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

import com.google.common.base.Strings;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by ploch on 06/04/2017.
 */
public class DirectoryWalker
{
    private static final FileSystem fileSystem = FileSystems.getDefault();

    public static void walkPath(Path startPath, String includeGlobPattern, String excludeGlobPattern, Consumer<Path> matchedFileCallback) throws IOException
    {
        PathMatcher includeMatcher = Strings.isNullOrEmpty(includeGlobPattern) ? fileSystem.getPathMatcher("glob:*") : fileSystem.getPathMatcher("glob:" + includeGlobPattern);
        PathMatcher excludeMatcher = Strings.isNullOrEmpty(excludeGlobPattern) ?
                null : fileSystem.getPathMatcher("glob:" + excludeGlobPattern);


        Files.walkFileTree(startPath, new Finder(includeMatcher, excludeMatcher, matchedFileCallback));
    }

    public static Stream<Path> walk(Path startPath, String includeGlobPattern, String excludeGlobPattern) throws IOException
    {
        PathMatcher includeMatcher = Strings.isNullOrEmpty(includeGlobPattern) ? fileSystem.getPathMatcher("glob:*") : fileSystem.getPathMatcher("glob:" + includeGlobPattern);
        PathMatcher excludeMatcher = Strings.isNullOrEmpty(excludeGlobPattern) ?
                null : fileSystem.getPathMatcher("glob:" + excludeGlobPattern);

        Stream<Path> pathStream = Files.walk(startPath).filter(path -> {
            return nameMatches(path, includeMatcher, excludeMatcher);
        });
        return pathStream;
    }

    public static void walkPath(Path startPath, String regexPattern, Consumer<Path> matchedFileCallback) throws IOException
    {
        //FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        Path path = Files.walkFileTree(startPath, new Finder(fileSystem.getPathMatcher("regex:" + regexPattern), null, matchedFileCallback));

    }

    private static boolean nameMatches(Path path, PathMatcher includeMatcher, PathMatcher excludeMatcher)
    {
        Path name = path.getFileName();
        if (name != null && includeMatcher.matches(name)) {
            if (excludeMatcher == null || !excludeMatcher.matches(name)) {
                return true;
            }

        }
        return false;
    }

    private static final PathMatcher includeAll = FileSystems.getDefault().getPathMatcher("glob:*");

    static class Finder extends SimpleFileVisitor<Path>
    {

        private final Consumer<Path> matchedFileCallback;
        private final PathMatcher includeMatcher;
        private final PathMatcher excludeMatcher;


        Finder(PathMatcher includeFileNameMatcher, PathMatcher excludeFileNameMatcher, Consumer<Path> matchedFileCallback)
        {
            this.includeMatcher = includeFileNameMatcher == null ? includeAll : includeFileNameMatcher;
            this.excludeMatcher = excludeFileNameMatcher;
            this.matchedFileCallback = matchedFileCallback;
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file)
        {
            Path name = file.getFileName();
            if (nameMatches(name, includeMatcher, excludeMatcher)) {
                matchedFileCallback.accept(file);
            }
            /*if (name != null && includeMatcher.matches(name)) {
                if (excludeMatcher == null || !excludeMatcher.matches(name)) {
                    matchedFileCallback.accept(file);
                }

            }*/
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs)
        {
            find(file);
            return CONTINUE;
        }



        @Override
        public FileVisitResult visitFileFailed(Path file,
                                               IOException exc)
        {
            System.err.println(exc);
            return CONTINUE;
        }
    }
}
