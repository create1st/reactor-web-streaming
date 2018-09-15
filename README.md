# Spring-Boot + Reactor Project + WebFlux + Reactive Mongo DB + Reactive AMQP + AOP

This project waits for **_Trade_** messages send via `AMQP` as a `JSON` strings and stores them in `Mongo DB`. 
In the same time messages are transformed to version with limited set of information and published
as `SSE` (Server Sent Event) on `REST` endpoint _**/transactions**_

For integration test purpose `Embedded Mongo DB` is used and `Embedded QPID AMQP` broker. 