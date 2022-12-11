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
package org.apache.logging.log4j.core.appender.db;

  <<<<<<< LOG4J2-1949
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
  =======
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
  >>>>>>> new-iso-date-time-formats
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
  <<<<<<< LOG4J2-1949
import static org.mockito.Mockito.verify;
  =======
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
  >>>>>>> new-iso-date-time-formats

import java.io.Serializable;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

public class AbstractDatabaseManagerTest {
    // this stub is provided because mocking constructors is hard
    private static class StubDatabaseManager extends AbstractDatabaseManager {

        protected StubDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }

        @Override
        protected boolean commitAndClose() {
            return true;
        }

        @Override
        protected void connectAndStart() {
            // noop
        }

        @Override
        protected boolean shutdownInternal() throws Exception {
            return true;
        }

        @Override
        protected void startupInternal() throws Exception {
            // noop
        }

        @Override
        protected void writeInternal(final LogEvent event, final Serializable serializable) {
            // noop
        }

    }

    private AbstractDatabaseManager manager;

    public void setUp(final String name, final int buffer) {
        manager = spy(new StubDatabaseManager(name, buffer));
    }

    @Test
    public void testBuffering01() throws Exception {
        setUp("name", 0);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();
        reset(manager);

        manager.write(event1, null);
        then(manager).should().writeThrough(same(event1), (Serializable) isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event1), (Serializable) isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);

        manager.write(event2, null);
        then(manager).should().writeThrough(same(event2), (Serializable) isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event2), (Serializable) isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);

        manager.write(event3, null);
        then(manager).should().writeThrough(same(event3), (Serializable) isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event3), (Serializable) isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);
    }

    @Test
    public void testBuffering02() throws Exception {
        setUp("name", 4);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);
        final LogEvent event4 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);
        final LogEvent event4copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);
        when(event4.toImmutable()).thenReturn(event4copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.write(event4, null);

        then(manager).should().connectAndStart();
        verify(manager, times(5)).isBuffered(); // 4 + 1 in flush()
        then(manager).should().writeInternal(same(event1copy), (Serializable) isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), (Serializable) isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), (Serializable) isNull());
        then(manager).should().buffer(event3);
        then(manager).should().writeInternal(same(event4copy), (Serializable) isNull());
        then(manager).should().buffer(event4);
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testBuffering03() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.flush();

        then(manager).should().connectAndStart();
        verify(manager, times(4)).isBuffered();
        then(manager).should().writeInternal(same(event1copy), (Serializable) isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), (Serializable) isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), (Serializable) isNull());
        then(manager).should().buffer(event3);
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testBuffering04() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.shutdown();

        then(manager).should().connectAndStart();
        verify(manager, times(4)).isBuffered();
        then(manager).should().writeInternal(same(event1copy), (Serializable) isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), (Serializable) isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), (Serializable) isNull());
        then(manager).should().buffer(event3);
        then(manager).should().commitAndClose();
        then(manager).should().shutdownInternal();
        then(manager).shouldHaveNoMoreInteractions();
    }
    
    @Test
    public void testBufferFlushOnBeforeFailoverAppenderStop() throws Exception {
        setUp("name", 10);
        
        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);
        
        manager.startup();
        manager.write(event1);
        manager.write(event2);
        manager.write(event3);
        
        manager.onBeforeFailoverAppenderStop();
        verify(manager).writeInternal(event1);
        verify(manager).writeInternal(event2);
        verify(manager).writeInternal(event3);
    }

    @Test
  <<<<<<< LOG4J2-1949
    public void testOnBeforeFailoverAppenderStopExceptionWithBuffer() throws Exception {
        int bufferSize = 10;
        setUp("name", bufferSize);
        manager.startup();

        final List<LogEvent> expectedEvents = new ArrayList<>();
        for (int i = 0; i < bufferSize - 1; i++) {
            LogEvent event = mock(LogEvent.class);
            expectedEvents.add(event);
            manager.write(event);
        }
        
        verify(manager, never()).writeInternal(any(LogEvent.class));

        final List<LogEvent> events = manager.onBeforeFailoverAppenderStopException();
        assertEquals("exception events do not match expected", expectedEvents, events);

        //buffer should be cleared, test by refilling and verify that flush was not called
        for (int i = 0; i < bufferSize - 1; i++) {
            LogEvent event = mock(LogEvent.class);
            manager.write(event);
        }

        verify(manager, never()).writeInternal(any(LogEvent.class));
    }
    
    @Test
    public void testOnBeforeFailoverAppenderStopExceptionWithoutBuffer() throws Exception {
        setUp("name", 0);
        
        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);
        
        manager.startup();
        manager.write(event1);
        manager.write(event2);
        manager.write(event3);
        
        assertTrue("onBeforeStopConfigurationException returned unexpected events", manager.onBeforeFailoverAppenderStopException().isEmpty());
    }
    
    @Test
    public void testOnFailover() throws Exception {
        int bufferSize = 10;
        setUp("name", bufferSize);
        manager.startup();

        final List<LogEvent> expectedEvents = new ArrayList<>();
        for (int i = 0; i < bufferSize - 1; i++) {
            LogEvent event = mock(LogEvent.class);
            expectedEvents.add(event);
            manager.write(event);
        }
        final LogEvent causalEvent = mock(LogEvent.class);
        final RuntimeException exception = new RuntimeException("test");
        doThrow(exception).when(manager).connectAndStart();
        expectedEvents.add(causalEvent);
        
        Exception caught = null;
        try {
            manager.write(causalEvent);
        } catch (Exception e) {
            caught = e;
        }
        assertNotNull("write did not throw expected exception", caught);
        assertEquals("exception thrown by write did not equal the expected one", exception, caught);
        
        verify(manager, never()).writeInternal(any(LogEvent.class));

        final List<LogEvent> events = manager.onFailover(causalEvent);
        assertEquals("exception events do not match expected", expectedEvents, events);

        //buffer should be cleared, test by refilling and verify that flush was not called
        reset(manager);
        for (int i = 0; i < bufferSize - 1; i++) {
            LogEvent event = mock(LogEvent.class);
            manager.write(event);
        }

        verify(manager, never()).writeInternal(any(LogEvent.class));
    }
    
    // this stub is provided because mocking constructors is hard
    private static class StubDatabaseManager extends AbstractDatabaseManager {
  =======
    public void testStartupShutdown01() throws Exception {
        setUp("testName01", 0);
  >>>>>>> new-iso-date-time-formats

        assertEquals("The name is not correct.", "testName01", manager.getName());
        assertFalse("The manager should not have started.", manager.isRunning());

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue("The manager should be running now.", manager.isRunning());

        manager.shutdown();
        then(manager).should().shutdownInternal();
        assertFalse("The manager should not be running anymore.", manager.isRunning());
    }

    @Test
    public void testStartupShutdown02() throws Exception {
        setUp("anotherName02", 0);

        assertEquals("The name is not correct.", "anotherName02", manager.getName());
        assertFalse("The manager should not have started.", manager.isRunning());

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue("The manager should be running now.", manager.isRunning());

        manager.releaseSub(-1, null);
        then(manager).should().shutdownInternal();
        assertFalse("The manager should not be running anymore.", manager.isRunning());
    }

    @Test
    public void testToString01() {
        setUp("someName01", 0);

        assertEquals("The string is not correct.", "someName01", manager.toString());
    }

    @Test
    public void testToString02() {
        setUp("bufferSize=12, anotherKey02=coolValue02", 12);

        assertEquals("The string is not correct.", "bufferSize=12, anotherKey02=coolValue02", manager.toString());
    }
}
