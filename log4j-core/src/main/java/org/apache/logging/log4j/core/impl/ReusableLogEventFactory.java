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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.ThreadNameCachingStrategy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.util.StringMap;

import java.util.List;

/**
 * Garbage-free LogEventFactory that reuses a single mutable log event.
 * @since 2.6
 */
public class ReusableLogEventFactory implements LogEventFactory {
    private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();

    private static final ThreadLocal<MutableLogEvent> mutableLogEventThreadLocal = new ThreadLocal<>();

    private final ContextDataInjector injector;
    private final Clock clock;
    private final NanoClock nanoClock;

    @Inject
    public ReusableLogEventFactory(final ContextDataInjector injector, final Clock clock, final NanoClock nanoClock) {
        this.injector = injector;
        this.clock = clock;
        this.nanoClock = nanoClock;
    }

    /**
     * Creates a log event.
     *
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param message The Message.
     * @param properties Properties to be added to the log event.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    @Override
    public LogEvent createEvent(final String loggerName, final Marker marker,
            final String fqcn, final Level level, final Message message,
            final List<Property> properties, final Throwable t) {
        return createEvent(loggerName, marker, fqcn, null, level, message, properties, t);
    }

    /**
     * Creates a log event.
     *
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param message The Message.
     * @param properties Properties to be added to the log event.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    @Override
    public LogEvent createEvent(final String loggerName, final Marker marker,
                                final String fqcn, final StackTraceElement location, final Level level, final Message message,
                                final List<Property> properties, final Throwable t) {
        MutableLogEvent result = getOrCreateMutableLogEvent();
        result.reserved = true;
        // No need to clear here, values are cleared in release when reserved is set to false.
        // If the event was dirty we'd create a new one.

        result.setLoggerName(loggerName);
        result.setMarker(marker);
        result.setLoggerFqcn(fqcn);
        result.setLevel(level == null ? Level.OFF : level);
        result.setMessage(message);
        result.initTime(clock, nanoClock);
        result.setThrown(t);
        result.setSource(location);
        result.setContextData(injector.injectContextData(properties, (StringMap) result.getContextData()));
        result.setContextStack(ThreadContext.getDepth() == 0 ? ThreadContext.EMPTY_STACK : ThreadContext.cloneStack());// mutable copy

        if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
            result.setThreadName(Thread.currentThread().getName()); // Thread.getName() allocates Objects on each call
            result.setThreadPriority(Thread.currentThread().getPriority());
        }
        return result;
    }

    private static MutableLogEvent getOrCreateMutableLogEvent() {
        MutableLogEvent result = mutableLogEventThreadLocal.get();
        return result == null || result.reserved ? createInstance(result) : result;
    }

    private static MutableLogEvent createInstance(MutableLogEvent existing) {
        MutableLogEvent result = new MutableLogEvent();

        // usually no need to re-initialize thread-specific fields since the event is stored in a ThreadLocal
        result.setThreadId(Thread.currentThread().getId());
        result.setThreadName(Thread.currentThread().getName()); // Thread.getName() allocates Objects on each call
        result.setThreadPriority(Thread.currentThread().getPriority());
        if (existing == null) {
            mutableLogEventThreadLocal.set(result);
        }
        return result;
    }

    /**
     * Switches the {@code reserved} flag off if the specified event is a MutableLogEvent, otherwise does nothing.
     * This flag is used internally to verify that a reusable log event is no longer in use and can be reused.
     * @param logEvent the log event to make available again
     * @since 2.7
     */
    public static void release(final LogEvent logEvent) { // LOG4J2-1583
        if (logEvent instanceof MutableLogEvent) {
            final MutableLogEvent mutableLogEvent = (MutableLogEvent) logEvent;
            mutableLogEvent.clear();
            mutableLogEvent.reserved = false;
        }
    }
}
