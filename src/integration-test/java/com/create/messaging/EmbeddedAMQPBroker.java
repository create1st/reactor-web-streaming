/*
 * Copyright  2018 Sebastian Gil.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.create.messaging;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.model.SystemConfig;
import org.apache.qpid.server.store.MemorySystemConfigImpl;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Map.entry;

public class EmbeddedAMQPBroker implements SmartLifecycle {
    private static final String QPID_CONFIG_JSON = "qpid-config.json";
    private static final String SPRING_RABBITMQ_PREFIX = "spring.rabbitmq";
    private static final String SPRING_RABBITMQ_PORT = SPRING_RABBITMQ_PREFIX + ".port";
    private static final String SPRING_RABBITMQ_VIRTUAL_HOST = SPRING_RABBITMQ_PREFIX + ".virtual-host";
    private static final String QPID_PREFIX = "qpid";
    private static final String QPID_AMQP_PORT = QPID_PREFIX + ".amqp_port";
    private static final String QPID_VHOST = QPID_PREFIX + ".vhost";
    private static final String DEFAULT_VIRTUAL_HOST = "localhost";
    private static final String DEFAUL_AMQP_PORT = "5672";

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final SystemLauncher broker = new SystemLauncher();
    private final Environment environment;

    public EmbeddedAMQPBroker(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void start() {
        if (!started.getAndSet(true)) {
            startEmbeddedAMQPBroker();
        }
    }

    @Override
    public void stop() {
        if (started.getAndSet(false)) {
            broker.shutdown();
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    private void startEmbeddedAMQPBroker() {
        Map<String, Object> systemConfigAttributes = getSystemConfigAttributes();
        try {
            broker.startup(systemConfigAttributes);
        } catch (Exception e) {
            started.set(false);
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> getSystemConfigAttributes() {
        String initialConfigurationLocation = getResourcePath(QPID_CONFIG_JSON);
        return Map.ofEntries(
                entry(QPID_AMQP_PORT, environment.getProperty(SPRING_RABBITMQ_PORT, DEFAUL_AMQP_PORT)),
                entry(QPID_VHOST, environment.getProperty(SPRING_RABBITMQ_VIRTUAL_HOST, DEFAULT_VIRTUAL_HOST)),
                entry(SystemConfig.TYPE, MemorySystemConfigImpl.SYSTEM_CONFIG_TYPE),
                entry(SystemConfig.INITIAL_CONFIGURATION_LOCATION, initialConfigurationLocation)
        );
    }

    private String getResourcePath(String fileName) {
        URL resource = EmbeddedAMQPBroker.class.getClassLoader().getResource(fileName);
        if (resource != null) {
            return resource.toExternalForm();
        }
        throw new IllegalArgumentException("AMQP broker configuration File not found : " + fileName);
    }
}
