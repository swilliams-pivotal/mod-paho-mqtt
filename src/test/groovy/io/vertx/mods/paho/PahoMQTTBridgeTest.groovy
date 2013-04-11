/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.mods.paho

import static org.junit.Assert.*
import static org.vertx.testtools.VertxAssert.*
import org.junit.Test
import org.vertx.java.core.Handler
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

/**
 * @author swilliams
 *
 */
class PahoMQTTBridgeTest extends TestVerticle {

  @Test
  public void testSubscribe() {
    def map = [
      'client': [
        'server-uri': 'tcp://m2m.eclipse.org:1883',
        'client-id': 'vertx-' + System.currentTimeMillis()
      ],
      'subscriptions':[
        ['topic':'foo', 'address':'test.foo']
      ]
    ]
    def config = new JsonObject(map)
    container.deployWorkerVerticle('groovy:'+PahoMQTTBridge.name, config, 1, false, { did->
      assertNotNull(did)
      testComplete()
    } as Handler)
  }

  @Test
  public void testUnsubscribe() {
    def map = [
      'client': [
        'server-uri': 'tcp://m2m.eclipse.org:1883',
        'client-id': 'vertx-' + System.currentTimeMillis()
      ],
      'subscriptions':[
        ['topic':'foo', 'address':'test.foo']
      ]
    ]
    def config = new JsonObject(map)
    container.deployWorkerVerticle('groovy:'+PahoMQTTBridge.name, config, 1, false, { did->
      assertNotNull(did)
      testComplete()
    } as Handler)
  }

  @Test
  public void testRelay() {
    def map = [
      'client': [
        'server-uri': 'tcp://m2m.eclipse.org:1883',
        'client-id': 'vertx-' + System.currentTimeMillis()
      ]
    ]
    def config = new JsonObject(map)
    container.deployWorkerVerticle('groovy:'+PahoMQTTBridge.name, config, 1, false, { did->
      assertNotNull(did)
      testComplete()
    } as Handler)
  }

}
