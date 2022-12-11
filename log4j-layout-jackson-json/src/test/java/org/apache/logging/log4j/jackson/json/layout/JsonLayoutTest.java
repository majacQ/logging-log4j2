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
package org.apache.logging.log4j.jackson.json.layout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.layout.LogEventFixtures;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.jackson.AbstractJacksonLayout;
import org.apache.logging.log4j.jackson.json.Log4jJsonObjectMapper;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the JsonLayout class.
 */
@UsingAnyThreadContext
public class JsonLayoutTest {
    private static class TestClass {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }

    private static final String DQUOTE = "\"";

    @AfterAll
    public static void cleanupClass() {
        LoggerContext.getContext().getInjector().removeBinding(ConfigurationFactory.KEY);
    }

    @BeforeAll
    public static void setupClass() {
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.getInjector().registerBinding(ConfigurationFactory.KEY, Lazy.lazy(BasicConfigurationFactory::new)::value);
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger rootLogger = this.ctx.getRootLogger();

    private void checkAt(final String expected, final int lineIndex, final List<String> list) {
        final String trimedLine = list.get(lineIndex).trim();
        assertEquals(trimedLine, expected, "Incorrect line index " + lineIndex + ": " + Strings.dquote(trimedLine));
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.equals(expected)) {
                return;
            }
        }
        fail("Cannot find " + expected + " in " + list);
    }

    private void checkMapEntry(final String key, final String value, final boolean compact, final String str,
            final boolean contextMapAslist) {
        this.toPropertySeparator(compact);
        if (contextMapAslist) {
            // {"key":"KEY", "value":"VALUE"}
            final String expected = String.format("{\"key\":\"%s\",\"value\":\"%s\"}", key, value);
            assertTrue(str.contains(expected), "Cannot find contextMapAslist " + expected + " in " + str);
        } else {
            // "KEY":"VALUE"
            final String expected = String.format("\"%s\":\"%s\"", key, value);
            assertTrue(str.contains(expected), "Cannot find contextMap " + expected + " in " + str);
        }
    }

    private void checkProperty(final String key, final String value, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        // {"key":"MDC.B","value":"B_Value"}
        final String expected = String.format("\"%s\"%s\"%s\"", key, propSep, value);
        assertTrue(str.contains(expected), "Cannot find " + expected + " in " + str);
    }

    private void checkPropertyName(final String name, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        assertTrue(str.contains(DQUOTE + name + DQUOTE + propSep), str);
    }

    private void checkPropertyNameAbsent(final String name, final boolean compact, final String str) {
        final String propSep = this.toPropertySeparator(compact);
        assertFalse(str.contains(DQUOTE + name + DQUOTE + propSep), str);
    }

    private String prepareJsonForObjectMessageAsJsonObjectTests(final int value, final boolean objectMessageAsJsonObject) {
        final TestClass testClass = new TestClass();
        testClass.setValue(value);
        // @formatter:off
        final Log4jLogEvent expected = Log4jLogEvent.newBuilder()
            .setLoggerName("a.B")
            .setLoggerFqcn("f.q.c.n")
            .setLevel(Level.DEBUG)
            .setMessage(new ObjectMessage(testClass))
            .setThreadName("threadName")
            .setTimeMillis(1).build();
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setCompact(true)
                .setObjectMessageAsJsonObject(objectMessageAsJsonObject)
                .build();
        // @formatter:off
        return layout.toSerializable(expected);
    }

    private String prepareJsonForStacktraceTests(final boolean stacktraceAsString) {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setCompact(true)
                .setIncludeStacktrace(true)
                .setStacktraceAsString(stacktraceAsString)
                .build();
        // @formatter:off
        return layout.toSerializable(expected);
    }

    @Test
    public void testAdditionalFields() throws Exception {
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setIncludeStacktrace(false)
                .setAdditionalFields(new KeyValuePair[] {
                    new KeyValuePair("KEY1", "VALUE1"),
                    new KeyValuePair("KEY2", "${java:runtime}"), })
                .setCharset(StandardCharsets.UTF_8)
                .setConfiguration(ctx.getConfiguration())
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.contains("\"KEY1\":\"VALUE1\""), str);
        assertTrue(str.contains("\"KEY2\":\"" + new JavaLookup().getRuntime() + "\""), str);
    }

    @Test
    public void testMutableLogEvent() throws Exception {
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setIncludeStacktrace(false)
                .setAdditionalFields(new KeyValuePair[] {
                        new KeyValuePair("KEY1", "VALUE1"),
                        new KeyValuePair("KEY2", "${java:runtime}"), })
                .setCharset(StandardCharsets.UTF_8)
                .setConfiguration(ctx.getConfiguration())
                .build();
        Log4jLogEvent logEvent = LogEventFixtures.createLogEvent();
        final MutableLogEvent mutableEvent = new MutableLogEvent();
        mutableEvent.initFrom(logEvent);
        final String strLogEvent = layout.toSerializable(logEvent);
        final String strMutableEvent = layout.toSerializable(mutableEvent);
        assertEquals(strLogEvent, strMutableEvent, strMutableEvent);
    }

    private void testAllFeatures(final boolean locationInfo, final boolean compact, final boolean eventEol,
            final String endOfLine, final boolean includeContext, final boolean contextMapAslist, final boolean includeStacktrace)
            throws Exception {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(locationInfo)
                .setProperties(includeContext)
                .setPropertiesAsList(contextMapAslist)
                .setComplete(false)
                .setCompact(compact)
                .setEventEol(eventEol)
                .setEndOfLine(endOfLine)
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(includeStacktrace)
                .build();
        // @formatter:off
        final String str = layout.toSerializable(expected);
        this.toPropertySeparator(compact);
        if (endOfLine == null) {
            // Just check for \n since \r might or might not be there.
            assertEquals(!compact || eventEol, str.contains("\n"), str);
        }
        else {
            assertEquals(!compact || eventEol, str.contains(endOfLine), str);
            assertEquals(compact && eventEol, str.endsWith(endOfLine), str);
        }
        assertEquals(locationInfo, str.contains("source"), str);
        assertEquals(includeContext, str.contains("contextMap"), str);
        final Log4jLogEvent actual = new Log4jJsonObjectMapper(contextMapAslist, includeStacktrace, false, false).readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, locationInfo, includeContext, includeStacktrace);
        if (includeContext) {
            this.checkMapEntry("MDC.A", "A_Value", compact, str, contextMapAslist);
            this.checkMapEntry("MDC.B", "B_Value", compact, str, contextMapAslist);
        }
        //
        assertNull(actual.getThrown());
        // make sure the names we want are used
        this.checkPropertyName("instant", compact, str);
        this.checkPropertyName("thread", compact, str); // and not threadName
        this.checkPropertyName("level", compact, str);
        this.checkPropertyName("loggerName", compact, str);
        this.checkPropertyName("marker", compact, str);
        this.checkPropertyName("name", compact, str);
        this.checkPropertyName("parents", compact, str);
        this.checkPropertyName("message", compact, str);
        this.checkPropertyName("thrown", compact, str);
        this.checkPropertyName("cause", compact, str);
        this.checkPropertyName("commonElementCount", compact, str);
        this.checkPropertyName("localizedMessage", compact, str);
        if (includeStacktrace) {
            this.checkPropertyName("extendedStackTrace", compact, str);
            this.checkPropertyName("class", compact, str);
            this.checkPropertyName("method", compact, str);
            this.checkPropertyName("file", compact, str);
            this.checkPropertyName("line", compact, str);
            this.checkPropertyName("exact", compact, str);
            this.checkPropertyName("location", compact, str);
            this.checkPropertyName("version", compact, str);
        } else {
            this.checkPropertyNameAbsent("extendedStackTrace", compact, str);
        }
        this.checkPropertyName("suppressed", compact, str);
        this.checkPropertyName("loggerFqcn", compact, str);
        this.checkPropertyName("endOfBatch", compact, str);
        if (includeContext) {
            this.checkPropertyName("contextMap", compact, str);
        } else {
            this.checkPropertyNameAbsent("contextMap", compact, str);
        }
        this.checkPropertyName("contextStack", compact, str);
        if (locationInfo) {
            this.checkPropertyName("source", compact, str);
        } else {
            this.checkPropertyNameAbsent("source", compact, str);
        }
        // check some attrs
        this.checkProperty("loggerFqcn", "f.q.c.n", compact, str);
        this.checkProperty("loggerName", "a.B", compact, str);
    }

    @Test
    public void testContentType() {
        final AbstractJacksonLayout layout = JsonLayout.createDefaultLayout();
        assertEquals("application/json; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final AbstractJacksonLayout layout = JsonLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    public void testEscapeLayout() throws Exception {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        final Configuration configuration = rootLogger.getContext().getConfiguration();
        // set up appender
        final boolean propertiesAsList = false;
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setConfiguration(configuration)
                .setLocationInfo(true)
                .setProperties(true)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(true)
                .setCompact(false)
                .setEventEol(false)
                .setIncludeStacktrace(true)
                .build();
        // @formatter:on
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("Here is a quote ' and then a double quote \"");

        appender.stop();

        final List<String> list = appender.getMessages();

        this.checkAt("[", 0, list);
        this.checkAt("{", 1, list);
        this.checkContains("\"level\" : \"DEBUG\",", list);
        this.checkContains("\"message\" : \"Here is a quote ' and then a double quote \\\"\",", list);
        this.checkContains("\"loggerFqcn\" : \"" + AbstractLogger.class.getName() + "\",", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    public void testExcludeStacktrace() throws Exception {
        this.testAllFeatures(false, false, false, null, false, false, false);
    }

    @Test
    public void testLocationOnCustomEndOfLine() throws Exception {
        this.testAllFeatures(true, true, true, "CUSTOM_END_OF_LINE", true, false, true);
    }

    @Test
    public void testIncludeNullDelimiterFalse() throws Exception {
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setCompact(true)
                .setIncludeNullDelimiter(false)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertFalse(str.endsWith("\0"));
    }

    @Test
    public void testIncludeNullDelimiterTrue() throws Exception {
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setCompact(true)
                .setIncludeNullDelimiter(true)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.endsWith("\0"));
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        final Configuration configuration = rootLogger.getContext().getConfiguration();
        // set up appender
        // Use [[ and ]] to test header and footer (instead of [ and ])
        final boolean propertiesAsList = false;
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setConfiguration(configuration)
                .setLocationInfo(true)
                .setProperties(true)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(true)
                .setCompact(false)
                .setEventEol(false)
                .setHeader("[[".getBytes(Charset.defaultCharset()))
                .setFooter("]]".getBytes(Charset.defaultCharset()))
                .setIncludeStacktrace(true)
                .build();
        // @formatter:on
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("starting mdc pattern test");

        this.rootLogger.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        this.rootLogger.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        this.rootLogger.error("finished mdc pattern test", new NullPointerException("test"));

        appender.stop();

        final List<String> list = appender.getMessages();

        this.checkAt("[[", 0, list);
        this.checkAt("{", 1, list);
        this.checkContains("\"loggerFqcn\" : \"" + AbstractLogger.class.getName() + "\",", list);
        this.checkContains("\"level\" : \"DEBUG\",", list);
        this.checkContains("\"message\" : \"starting mdc pattern test\",", list);
        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    public void testLayoutLoggerName() throws Exception {
        final boolean propertiesAsList = false;
        // @formatter:off
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(true)
                .build();
        // @formatter:on
        // @formatter:off
        final Log4jLogEvent expected = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLoggerFqcn("f.q.c.n")
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("M"))
                .setThreadName("threadName")
                .setTimeMillis(1).build();
        // @formatter:on
        final String str = layout.toSerializable(expected);
        assertTrue(str.contains("\"loggerName\":\"a.B\""), str);
        final Log4jLogEvent actual = new Log4jJsonObjectMapper(propertiesAsList, true, false, false).readValue(str, Log4jLogEvent.class);
        assertEquals(expected.getLoggerName(), actual.getLoggerName());
        assertEquals(expected, actual);
    }

    @Test
    public void testLayoutMessageWithCurlyBraces() throws Exception {
        final boolean propertiesAsList = false;
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(true)
                .build();
        final Log4jLogEvent expected = Log4jLogEvent.newBuilder()
                .setLoggerName("a.B")
                .setLoggerFqcn("f.q.c.n")
                .setLevel(Level.DEBUG)
                .setMessage(new ParameterizedMessage("Testing {}", new TestObj()))
                .setThreadName("threadName")
                .setTimeMillis(1).build();
        final String str = layout.toSerializable(expected);
        final String expectedMessage = "Testing " + TestObj.TO_STRING_VALUE;
        assertTrue(str.contains("\"message\":\"" + expectedMessage + '"'), str);
        final Log4jLogEvent actual = new Log4jJsonObjectMapper(propertiesAsList, true, false, false).readValue(str, Log4jLogEvent.class);
        assertEquals(expectedMessage, actual.getMessage().getFormattedMessage());
    }

    // Test for LOG4J2-2345
    @Test
    public void testReusableLayoutMessageWithCurlyBraces() throws Exception {
        final boolean propertiesAsList = false;
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(true)
                .build();
        Message message = ReusableMessageFactory.INSTANCE.newMessage("Testing {}", new TestObj());
        try {
            final Log4jLogEvent expected = Log4jLogEvent.newBuilder()
                    .setLoggerName("a.B")
                    .setLoggerFqcn("f.q.c.n")
                    .setLevel(Level.DEBUG)
                    .setMessage(message)
                    .setThreadName("threadName")
                    .setTimeMillis(1).build();
            MutableLogEvent mutableLogEvent = new MutableLogEvent();
            mutableLogEvent.initFrom(expected);
            final String str = layout.toSerializable(mutableLogEvent);
            final String expectedMessage = "Testing " + TestObj.TO_STRING_VALUE;
            assertTrue(str.contains("\"message\":\"" + expectedMessage + '"'), str);
            final Log4jLogEvent actual = new Log4jJsonObjectMapper(propertiesAsList, true, false, false).readValue(str, Log4jLogEvent.class);
            assertEquals(expectedMessage, actual.getMessage().getFormattedMessage());
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    // Test for LOG4J2-2312 LOG4J2-2341
    @Test
    public void testLayoutRingBufferEventReusableMessageWithCurlyBraces() throws Exception {
        final boolean propertiesAsList = false;
        final AbstractJacksonLayout layout = JsonLayout.newBuilder()
                .setLocationInfo(false)
                .setProperties(false)
                .setPropertiesAsList(propertiesAsList)
                .setComplete(false)
                .setCompact(true)
                .setEventEol(false)
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(true)
                .build();
        Message message = ReusableMessageFactory.INSTANCE.newMessage("Testing {}", new TestObj());
        try {
            RingBufferLogEvent ringBufferEvent = new RingBufferLogEvent();
            ringBufferEvent.setValues(
                    null, "a.B", null, "f.q.c.n", Level.DEBUG, message,
                    null, new SortedArrayStringMap(), ThreadContext.EMPTY_STACK, 1L,
                    "threadName", 1, null, new SystemClock(), new DummyNanoClock());
            final String str = layout.toSerializable(ringBufferEvent);
            final String expectedMessage = "Testing " + TestObj.TO_STRING_VALUE;
            assertThat(str, containsString("\"message\":\"" + expectedMessage + '"'));
            final Log4jLogEvent actual = new Log4jJsonObjectMapper(propertiesAsList, true, false, false).readValue(str, Log4jLogEvent.class);
            assertEquals(expectedMessage, actual.getMessage().getFormattedMessage());
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    static class TestObj {
        static final String TO_STRING_VALUE = "This is my toString {} with curly braces";
        @Override
        public String toString() {
            return TO_STRING_VALUE;
        }
    }

    @Test
    public void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false, null, false, false, true);
    }

    @Test
    public void testLocationOnCompactOnEventEolOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, true, null, true, false, true);
    }

    @Test
    public void testLocationOnCompactOnEventEolOnMdcOnMdcAsList() throws Exception {
        this.testAllFeatures(true, true, true, null, true, true, true);
    }

    @Test
    public void testLocationOnCompactOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, false, null, true, false, true);
    }

    @Test
    public void testObjectMessageAsJsonObject() {
            final String str = prepareJsonForObjectMessageAsJsonObjectTests(1234, true);
            assertTrue(str.contains("\"message\":{\"value\":1234}"), str);
    }

    @Test
    public void testObjectMessageAsJsonString() {
            final String str = prepareJsonForObjectMessageAsJsonObjectTests(1234, false);
        assertTrue(str.contains("\"message\":\"" + this.getClass().getCanonicalName() + "$TestClass@"), str);
    }

    @Test
    public void testStacktraceAsNonString() throws Exception {
        final String str = prepareJsonForStacktraceTests(false);
        assertTrue(str.contains("\"extendedStackTrace\":["), str);
    }

    @Test
    public void testStacktraceAsString() throws Exception {
        final String str = prepareJsonForStacktraceTests(true);
        assertTrue(str.contains("\"extendedStackTrace\":\"java.lang.NullPointerException"), str);
    }

    private String toPropertySeparator(final boolean compact) {
        return compact ? ":" : " : ";
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2749">LOG4J2-2749</a>
     */
    @Test
    public void testEmptyValuesAreIgnored() {
        final AbstractJacksonLayout layout = JsonLayout
                .newBuilder()
                .setAdditionalFields(new KeyValuePair[] {
                        new KeyValuePair("empty", "${ctx:empty:-}")
                })
                .setConfiguration(ctx.getConfiguration())
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertFalse(str.contains("\"empty\""), str);
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-3358">LOG4J2-3358</a>
     */
    @Test
    public void jsonLayout_should_substitute_lookups() {

        // Create the layout.
        KeyValuePair[] additionalFields = {
                KeyValuePair
                        .newBuilder()
                        .setKey("who")
                        .setValue("${ctx:WHO}")
                        .build()
        };
        JsonLayout layout = JsonLayout
                .newBuilder()
                .setConfiguration(new DefaultConfiguration())
                .setAdditionalFields(additionalFields)
                .build();

        // Create a log event containing `WHO` key in MDC.
        StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("WHO", "mduft");
        LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setContextData(contextData)
                .build();

        // Verify the `WHO` key.
        String serializedLogEvent = layout.toSerializable(logEvent);
        assertThat(serializedLogEvent, containsString("\"who\" : \"mduft\""));

    }
}
