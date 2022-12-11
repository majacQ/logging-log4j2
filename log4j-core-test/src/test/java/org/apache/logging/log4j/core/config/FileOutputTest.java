/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.config;


import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import org.junit.jupiter.api.condition.DisabledOnOs;

@DisabledOnOs(WINDOWS) // FIXME: Fix status logger to close files so this will pass on windows.
@CleanUpFiles({"target/status.log", "target/test.log"})
public class FileOutputTest {

    @Test
    @LoggerContextSource("classpath:log4j-filetest.xml")
    public void testConfig() throws IOException {
        final Path logFile = Paths.get("target", "status.log");
        assertTrue(Files.exists(logFile), "Status output file does not exist");
        assertTrue(Files.size(logFile) > 0, "File is empty");
    }

}
