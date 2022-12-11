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
import org.apache.logging.log4j.core.script.ScriptManagerFactory;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.script.factory.ScriptManagerFactoryImpl;
import org.apache.logging.log4j.script.plugins.Log4jPlugins;

module org.apache.logging.log4j.script {
    exports org.apache.logging.log4j.script;
    exports org.apache.logging.log4j.script.appender;
    opens org.apache.logging.log4j.script.appender to
      org.apache.logging.log4j.core;
    exports org.apache.logging.log4j.script.appender.rolling.action;
    exports org.apache.logging.log4j.script.config.arbiter;
    opens org.apache.logging.log4j.script.config.arbiter to
      org.apache.logging.log4j.core;
    exports org.apache.logging.log4j.script.filter;
    exports org.apache.logging.log4j.script.layout;
    opens org.apache.logging.log4j.script.layout to
      org.apache.logging.log4j.core;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.plugins;
    requires java.scripting;

    provides PluginService with Log4jPlugins;
    provides ScriptManagerFactory with ScriptManagerFactoryImpl;
}
