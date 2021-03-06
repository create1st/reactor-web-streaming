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

import com.create.messaging.channels.Trades;
import com.create.messaging.publishers.TradePublisher;
import com.create.messaging.publishers.TransactionPublisher;
import com.create.messaging.subscribers.PersistenceSubscriber;
import com.create.pojo.Trade;
import com.create.pojo.Transaction;
import com.create.repositories.TradeRepository;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding({Trades.class})
class ReactorConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    TradePublisher tradePublisher() {
        return new TradePublisher();
    }

    @Bean
    Subscriber<Trade> persistenceSubscriber(TradeRepository tradeRepository,
                                            TradePublisher tradePublisher) {
        PersistenceSubscriber persistenceSubscriber = new PersistenceSubscriber(tradeRepository);
        tradePublisher.subscribe(persistenceSubscriber);
        return persistenceSubscriber;
    }

    @Bean
    Publisher<Transaction> transactionPublisher(Publisher<Trade> tradePublisher) {
        return new TransactionPublisher(tradePublisher);
    }
}
