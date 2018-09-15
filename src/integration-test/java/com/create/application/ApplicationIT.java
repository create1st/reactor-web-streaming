/*
 * Copyright  2016 Sebastian Gil.
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

package com.create.application;

import com.create.application.configuration.AppConfiguration;
import com.create.application.configuration.AppTestConfiguration;
import com.create.application.configuration.WebConfiguration;
import com.create.messaging.channels.Trades;
import com.create.pojo.Customer;
import com.create.pojo.Trade;
import com.create.pojo.Transaction;
import com.create.repositories.TradeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                AppTestConfiguration.class,
                AppConfiguration.class
        })
@AutoConfigureWebTestClient
public class ApplicationIT {
    private static final String CUSTOMER_ID = "CustomerID";
    private static final String CUSTOMER = "Customer";
    private static final String TRADE_ID = "TradeID";
    private static final long AMOUNT = 123L;

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TradeRepository tradeRepository;

    @Test
    public void contextLoads()  {
    }

    @Test
    public void transactionIsReceivedOnWebEndpoint() {
        getSingleTradePublisher()
                .delayElement(Duration.ofMillis(500))
                .subscribe(this::send);
        Flux<Transaction> transactionFlux = webTestClient
                .get()
                .uri(WebConfiguration.TRANSACTIONS_URL)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Transaction.class)
                .getResponseBody();
        StepVerifier.create(transactionFlux)
                .expectNext(getExpectedTransaction())
                .thenCancel()
                .verify();
    }

    @Test
    public void tradeIsStoredInMongoDb() {
        Flux<Trade> repositoryFlux = tradeRepository.deleteAll()
                .then(getSingleTradePublisher()
                        .map(this::send))
                .delayElement(Duration.ofMillis(500))
                .thenMany(tradeRepository.findAll());
        StepVerifier.create(repositoryFlux)
                .expectNext(getTrade())
                .thenCancel()
                .verify();
    }

    private Mono<Object> send(Trade msg) {
        rabbitTemplate.convertAndSend(Trades.INCOMING_TRADES, msg);
        return Mono.empty();
    }

    private Mono<Trade> getSingleTradePublisher() {
        return Mono.just(getTrade())
                .log();
    }

    private Transaction getExpectedTransaction() {
        return Transaction.builder()
                .amount(AMOUNT)
                .name(CUSTOMER)
                .build();
    }

    private Trade getTrade() {
        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .name(CUSTOMER)
                .build();
        return Trade.builder()
                .id(TRADE_ID)
                .amount(AMOUNT)
                .customer(customer)
                .build();
    }

}
