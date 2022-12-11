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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class LoggingTest {

    private static final String CONFIG = "log4j2-config.xml";

    @Test
    @LoggerContextSource(CONFIG)
    public void testParent() {
        final Logger logger = Logger.getLogger("org.apache.test.logging.Test");
        final Category parent = logger.getParent();
        assertNotNull(parent, "No parent Logger");
        assertEquals("org.apache.test.logging", parent.getName(), "Incorrect parent logger");
    }

}
