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
open module org.apache.logging.log4j.core {
    exports org.apache.logging.log4j.core;
    exports org.apache.logging.log4j.core.appender;
    exports org.apache.logging.log4j.core.appender.db;
    exports org.apache.logging.log4j.core.appender.nosql;
    exports org.apache.logging.log4j.core.appender.rewrite;
    exports org.apache.logging.log4j.core.appender.rolling;
    exports org.apache.logging.log4j.core.appender.rolling.action;
    exports org.apache.logging.log4j.core.appender.routing;
    exports org.apache.logging.log4j.core.async;
    exports org.apache.logging.log4j.core.config;
    exports org.apache.logging.log4j.core.config.arbiters;
    exports org.apache.logging.log4j.core.config.builder;
    exports org.apache.logging.log4j.core.config.di;
    exports org.apache.logging.log4j.core.config.plugins;
    exports org.apache.logging.log4j.core.config.plugins.convert;
    exports org.apache.logging.log4j.core.config.plugins.util;
    exports org.apache.logging.log4j.core.config.plugins.validation.validators;
    exports org.apache.logging.log4j.core.config.properties;
    exports org.apache.logging.log4j.core.config.xml;
    exports org.apache.logging.log4j.core.filter;
    exports org.apache.logging.log4j.core.impl;
    exports org.apache.logging.log4j.core.jmx;
    exports org.apache.logging.log4j.core.layout;
    exports org.apache.logging.log4j.core.lookup;
    exports org.apache.logging.log4j.core.message;
    exports org.apache.logging.log4j.core.net;
    exports org.apache.logging.log4j.core.net.ssl;
    exports org.apache.logging.log4j.core.parser;
    exports org.apache.logging.log4j.core.pattern;
    exports org.apache.logging.log4j.core.script;
    exports org.apache.logging.log4j.core.selector;
    exports org.apache.logging.log4j.core.test;
    exports org.apache.logging.log4j.core.test.appender;
    exports org.apache.logging.log4j.core.test.hamcrest;
    exports org.apache.logging.log4j.core.test.junit;
    exports org.apache.logging.log4j.core.time;
    exports org.apache.logging.log4j.core.tools;
    exports org.apache.logging.log4j.core.util;

    requires transitive java.compiler;
    requires transitive java.desktop;
    requires transitive java.management;
    requires transitive java.sql;
    requires transitive java.rmi;
    requires transitive java.xml;
    requires transitive org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.test;
    requires transitive org.apache.logging.log4j.plugins;
    requires transitive org.apache.logging.log4j.plugins.test;
    requires transitive com.lmax.disruptor;
    requires transitive org.jctools.core;
    requires transitive org.osgi.framework;
    requires transitive com.conversantmedia.disruptor;
    requires transitive net.bytebuddy;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.dataformat.xml;
    requires transitive com.fasterxml.jackson.dataformat.yaml;
    requires transitive org.apache.commons.compress;
    requires transitive org.fusesource.jansi;
    requires transitive org.junit.jupiter.api;
    requires transitive org.junit.jupiter.engine;
    requires transitive org.junit.jupiter.params;
    requires transitive org.junit.platform.commons;
    requires transitive org.junit.platform.engine;
    requires transitive junit;

    uses org.apache.logging.log4j.core.util.ContextDataProvider;
    uses org.apache.logging.log4j.core.util.WatchEventService;
    provides org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory with org.apache.logging.log4j.core.message.ExtendedThreadInfoFactory;
    provides org.apache.logging.log4j.core.util.ContextDataProvider with org.apache.logging.log4j.core.impl.ThreadContextDataProvider;
    provides org.apache.logging.log4j.spi.Provider with org.apache.logging.log4j.core.impl.Log4jProvider;
    provides org.apache.logging.log4j.plugins.model.PluginService with org.apache.logging.log4j.core.plugins.Log4jPlugins,
            org.apache.logging.log4j.core.test.plugins.Log4jPlugins;
}
