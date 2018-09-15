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

import com.create.messaging.channels.Trades;
import com.create.pojo.Trade;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TradePublisher implements Lifecycle, Publisher<Trade> {
    private static final Logger LOG = LoggerFactory.getLogger(TradePublisher.class);
    private AtomicBoolean started = new AtomicBoolean(false);
    private final List<FluxSink<Trade>> sinks = new CopyOnWriteArrayList<>();
    private final Flux<Trade> tradeFlux;

    public TradePublisher() {
        this.tradeFlux = Flux.<Trade>create(sinks::add)
                .log();
    }

    @StreamListener(Trades.INCOMING_TRADES)
    public void onMessage(@Payload Trade trade) {
        if (started.get()) {
            try {
                notifyAllSubscriptions(trade);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse message content : " + Objects.toString(trade), e);
            }
        } else {
            throw new IllegalStateException("Attempt to publish a message on not started publisher");
        }
    }

    @Override
    public void subscribe(Subscriber<? super Trade> subscriber) {
        tradeFlux.subscribe(subscriber);
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
        List<FluxSink<Trade>> deadSinks = sinks.stream()
                .map(sink -> notifyActiveSinkAndReportDead(sink, trade))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        sinks.removeAll(deadSinks);
    }

    private FluxSink<Trade> notifyActiveSinkAndReportDead(FluxSink<Trade> sink,
                                                          Trade trade) {
        FluxSink<Trade> deadSink = null;
        if (sink.isCancelled()) {
            deadSink = sink;
        } else {
            try {
                sink.next(trade);
            } catch (Exception e) {
                LOG.error("Failed to notify : {}", sink, e);
                sink.error(e);
                deadSink = sink;
            }
        }
        return deadSink;
    }

    private void completeAllSubscriptions() {
        sinks.stream()
                .filter(sink -> !sink.isCancelled())
                .forEach(FluxSink::complete);
        sinks.clear();
    }
}
