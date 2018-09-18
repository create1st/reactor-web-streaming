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

package com.create.application.configuration;

import com.create.messaging.EmbeddedAMQPBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("qpid")
class QpidTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("inputBindingLifecycle")
    EmbeddedAMQPBroker embeddedAMQPBroker(Environment environment) {
        return new EmbeddedAMQPBroker(environment);
    }
}
