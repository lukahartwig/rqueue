/*
 * Copyright 2020 Sonu Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.sonus21.rqueue.spring;

import com.github.sonus21.rqueue.config.RqueueConfig;
import com.github.sonus21.rqueue.listener.RqueueMessageHandler;
import com.github.sonus21.rqueue.listener.RqueueMessageListenerContainer;
import com.github.sonus21.rqueue.metrics.QueueCounter;
import com.github.sonus21.rqueue.metrics.RqueueCounter;
import com.github.sonus21.rqueue.metrics.RqueueMetrics;
import com.github.sonus21.rqueue.producer.RqueueMessageSender;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RqueueMessageConfig extends RqueueConfig {

  @Bean
  public RqueueMessageHandler rqueueMessageHandler() {
    if (simpleRqueueListenerContainerFactory.getRqueueMessageHandler() != null) {
      return simpleRqueueListenerContainerFactory.getRqueueMessageHandler();
    }
    if (simpleRqueueListenerContainerFactory.getMessageConverters() != null) {
      return new RqueueMessageHandler(simpleRqueueListenerContainerFactory.getMessageConverters());
    }
    return new RqueueMessageHandler();
  }

  @Bean
  public RqueueMessageListenerContainer rqueueMessageListenerContainer(
      RqueueMessageHandler rqueueMessageHandler) {
    if (simpleRqueueListenerContainerFactory.getRqueueMessageHandler() == null) {
      simpleRqueueListenerContainerFactory.setRqueueMessageHandler(rqueueMessageHandler);
    }
    if (simpleRqueueListenerContainerFactory.getRedisConnectionFactory() == null) {
      simpleRqueueListenerContainerFactory.setRedisConnectionFactory(getRedisConnectionFactory());
    }
    return simpleRqueueListenerContainerFactory.createMessageListenerContainer();
  }

  @Bean
  public RqueueMessageSender rqueueMessageSender() {
    if (simpleRqueueListenerContainerFactory.getMessageConverters() != null) {
      return new RqueueMessageSender(
          getMessageTemplate(getRedisConnectionFactory()),
          simpleRqueueListenerContainerFactory.getMessageConverters());
    }
    return new RqueueMessageSender(getMessageTemplate(getRedisConnectionFactory()));
  }

  @Bean
  @Conditional(MissingRedisMessageListener.class)
  public RedisMessageListenerContainer redisMessageListenerContainer() {
    RedisMessageListenerContainer messageListenerContainer = new RedisMessageListenerContainer();
    messageListenerContainer.setConnectionFactory(getRedisConnectionFactory());
    return messageListenerContainer;
  }

  @Bean
  @Conditional(MetricsEnabled.class)
  public RqueueMetrics rqueueMetrics(
      RqueueMessageListenerContainer rqueueMessageListenerContainer,
      MeterRegistry meterRegistry,
      RqueueMetricsProperties rqueueMetricsProperties) {
    QueueCounter queueCounter = new QueueCounter();
    return new RqueueMetrics(
        rqueueMessageListenerContainer.getRqueueMessageTemplate(),
        rqueueMetricsProperties,
        meterRegistry,
        queueCounter);
  }

  @Bean
  @Conditional(MetricsEnabled.class)
  public RqueueCounter rqueueCounter(RqueueMetrics rqueueMetrics) {
    return new RqueueCounter(rqueueMetrics.getQueueCounter());
  }
}
