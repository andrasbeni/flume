/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flume.source.jms;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;

import com.google.common.base.Optional;

public class JMSMessageConsumerFactory {

  JMSMessageConsumer create(InitialContext initialContext, ConnectionFactory connectionFactory,
      String destinationName, JMSDestinationType destinationType,
      JMSDestinationLocator destinationLocator, String messageSelector, int batchSize,
      long pollTimeout, JMSMessageConverter messageConverter,
      Optional<String> userName, Optional<String> password, Optional<String> clientId,
      boolean createDurableSubscription, String durableSubscriptionName) {
    return new JMSMessageConsumer(initialContext, connectionFactory, destinationName,
        destinationLocator, destinationType, messageSelector, batchSize, pollTimeout,
        messageConverter, userName, password, clientId, 
        createDurableSubscription, durableSubscriptionName);
  }
}
