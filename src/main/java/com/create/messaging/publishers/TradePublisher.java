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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TradePublisher implements Lifecycle, MessageHandler, Publisher<Trade> {
    private static final Logger LOG = LoggerFactory.getLogger(TradePublisher.class);
    private AtomicBoolean started = new AtomicBoolean(false);
    private final List<FluxSink<Trade>> sinks = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;
    private final Flux<Trade> tradeFlux;

    public TradePublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.tradeFlux = Flux.<Trade>create(sinks::add)
                .log();
    }

    @Override
    public void handleMessage(@NotNull Message<?> message) throws MessagingException {
        if (started.get()) {
            try {
                Trade trade = getTrade(message);
                notifyAllSubscriptions(trade);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse message content : " + Objects.toString(message), e);
            }
        } else {
            throw new IllegalStateException("Attempt to publish a message on not started publisher");
        }
    }

    @Override
    public void subscribe(Subscriber<? super Trade> subscriber) {
        tradeFlux.subscribe(subscriber);
    }

    private Trade getTrade(Message<?> message) throws java.io.IOException {
        byte[] payload = getPayload(message);
        return objectMapper.readValue(payload, Trade.class);
    }

    private byte[] getPayload(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            return (byte[]) payload;
        }
        throw new IllegalArgumentException("Expected payload of type byte array. Found : " + Objects.toString(payload));
    }

    @Override
    public void start() {
        started.set(true);
    }

    @Override
    public void stop() {
        if (started.getAndSet(false)) {
            completeAllSubscriptions();
        }
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    private void notifyAllSubscriptions(Trade trade) {
        sinks.forEach(sink -> notifyActiveSinkOrRemoveCancelled(sink, trade));
    }

    private void notifyActiveSinkOrRemoveCancelled(FluxSink<Trade> sink,
                                                   Trade trade) {
        if (sink.isCancelled()) {
            sinks.remove(sink);
        } else {
            sink.next(trade);
        }
    }

    private void completeAllSubscriptions() {
        sinks.stream()
                .filter(sink -> !sink.isCancelled())
                .forEach(FluxSink::complete);
        sinks.clear();
    }
}
