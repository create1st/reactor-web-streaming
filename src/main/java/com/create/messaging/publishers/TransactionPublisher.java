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

package com.create.messaging.publishers;

import com.create.pojo.Trade;
import com.create.pojo.Transaction;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

public class TransactionPublisher implements Publisher<Transaction> {
    private Flux<Transaction> transactionFlux;

    public TransactionPublisher(@NonNull Publisher<Trade> tradePublisher) {
        transactionFlux = Flux.from(tradePublisher)
                .map(this::getTransaction);
    }

    private Transaction getTransaction(Trade trade) {
        return Transaction.builder()
                .amount(trade.getAmount())
                .name(trade.getCustomer().getName())
                .build();
    }

    @Override
    public void subscribe(Subscriber<? super Transaction> subscriber) {
        transactionFlux.subscribe(subscriber);
    }
}