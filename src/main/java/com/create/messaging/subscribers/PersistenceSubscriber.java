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

package com.create.messaging.subscribers;

import com.create.pojo.Trade;
import com.create.repositories.TradeRepository;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceSubscriber implements Subscriber<Trade> {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceSubscriber.class);

    private final TradeRepository tradeRepository;

    public PersistenceSubscriber(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Trade trade) {
        tradeRepository.save(trade)
                .subscribe(saved -> LOG.debug("Saved : {}", saved));
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("Subscriber error", t);
    }

    @Override
    public void onComplete() {
        LOG.info("subscriber completed");
    }
}
