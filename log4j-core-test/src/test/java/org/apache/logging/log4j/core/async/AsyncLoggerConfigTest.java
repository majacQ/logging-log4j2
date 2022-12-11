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
package org.apache.logging.log4j.core.async;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

@Tag("async")
@SetSystemProperty(key = Log4jProperties.CONFIG_LOCATION, value = "AsyncLoggerConfigTest.xml")
public class AsyncLoggerConfigTest {

    @Test
    public void testAdditivity() throws Exception {
        final File file = new File("target", "AsyncLoggerConfigTest.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "Additive logging: 2 for the price of 1!";
        log.info(msg);
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        reader.close();
        file.delete();
        assertNotNull(line1, "line1");
        assertNotNull(line2, "line2");
        assertTrue(line1.contains(msg), "line1 correct");
        assertTrue(line2.contains(msg), "line2 correct");

        final String location = "testAdditivity";
        assertTrue(line1.contains(location) || line2.contains(location), "location");
    }

    @Test
    public void testIncludeLocationDefaultsToFalse() {
        final LoggerConfig rootLoggerConfig =
                AsyncLoggerConfig.RootLogger.createLogger(
                        null, Level.INFO, null, new AppenderRef[0], null, new DefaultConfiguration(), null);
        assertFalse(rootLoggerConfig.isIncludeLocation(), "Include location should default to false for async loggers");

        final LoggerConfig loggerConfig =
                AsyncLoggerConfig.createLogger(
                        false, Level.INFO, "com.foo.Bar", null, new AppenderRef[0], null, new DefaultConfiguration(),
                        null);
        assertFalse(loggerConfig.isIncludeLocation(), "Include location should default to false for async loggers");
    }
}
