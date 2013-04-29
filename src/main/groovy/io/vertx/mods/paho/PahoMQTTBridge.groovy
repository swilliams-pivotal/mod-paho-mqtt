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

import groovy.transform.CompileStatic

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.Future

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence


/**
 * @author swilliams
 *
 */
@CompileStatic
class PahoMQTTBridge extends Verticle implements MqttCallback {

  static def DEFAULT_CONTROL_ADDRESS = 'vertx.mqtt.control'
  static def DEFAULT_RELAY_ADDRESS = 'vertx.mqtt.relay'

  String controlAddress
  String relayAddress

  MqttClient client
  String defaultTopic

  int retryConnectionCounter = 0
  Map topicAddressBridge = [:]
  Set permittedActions = ['subscribe', 'unsubscribe']

  @Override
  def start(Future<Void> result) {
    this.controlAddress = container.config['controlAddress'] ?: DEFAULT_CONTROL_ADDRESS
    this.relayAddress = container.config['relayAddress'] ?: DEFAULT_RELAY_ADDRESS
    this.defaultTopic = container.config['defaultTopic']

    vertx.eventBus.registerHandler(controlAddress, this.&control) { AsyncResult arc->
      if (arc.failed()) result.setFailure(arc.cause())
      vertx.eventBus.registerHandler(relayAddress, this.&relay) { AsyncResult arr->
        if (arr.failed()) result.setFailure(arr.cause())
      } // end closure 2
    } // end closure 1



    /* 
     * FIXME vert.x throws an exception if the try/catch is in the closure block above.
     * org.codehaus.groovy.runtime.typehandling.GroovyCastException: 
     *  Cannot cast object 'io.vertx.mods.paho.PahoMQTTBridge$_start_closure1@64aa8ffc' with class
     *  'io.vertx.mods.paho.PahoMQTTBridge$_start_closure1' to class 'io.vertx.mods.paho.PahoMQTTBridge'
     */
    try {
      configure(container.config['client'] as Map)

      List subscriptions = container.config['subscriptions'] as List
      subscriptions?.each { Map subscription->
        subscribe(subscription)
      }

      result.setResult(null)
    }
    catch (MqttException e) {
      result.setFailure(e)
    }
  }

  @Override
  def stop() {
    vertx.eventBus.unregisterHandler(relayAddress, this.&relay) {
      vertx.eventBus.unregisterHandler(controlAddress, this.&control) {

        Set keys = topicAddressBridge.keySet()
        if (keys.size() > 0) {
          client?.connect()

          for (String topicName : keys) {
            client?.unsubscribe(topicName)
            topicAddressBridge.remove topicName
          }

          client?.disconnect()
        }
      }
    }
  }

  def configure(Map config) throws MqttException {
    String uri = config['server-uri']
    String clientId = config['client-id']

    String persistenceDir = config['persistence-dir'] ?: System.getProperty('java.io.tmpdir')
    def persistence = new MqttDefaultFilePersistence(persistenceDir)
    client = new MqttClient(uri, clientId, persistence)

    client.setCallback(this)

    client.connect()
    if (client.connected) {
      this.retryConnectionCounter = 0
    }
    client.disconnect()
  }

  def control(Message msg) {
    String action = msg.body['action']
    Map config = msg.body['config'] as Map

    if (!permittedActions.contains(action)) return

    this.invokeMethod(action, config)
  }

  def relay(Message msg) {

    String topicName = msg.body['topic'] ?: defaultTopic
    int qos = msg.body['qos'] as int ?: 2
    byte[] payload = msg.body['payload']

    client?.connect()

    def topic = client.getTopic(topicName)
    def message = new MqttMessage(payload)
    message.setQos(qos)

    def token = topic.publish(message)
    // token.waitForCompletion()

    client.disconnect()
  }

  def subscribe(Map config) {

    String address = config['address']
    String topicName = config['topic'] ?: defaultTopic
    int qos = (config['qos'] ?: 2) as int

    client?.connect()
    client?.subscribe(topicName, qos)
    topicAddressBridge.put topicName, address
    client?.disconnect()
  }

  def unsubscribe(Map config) {

    String address = config['address']
    String topicName = config['topic'] ?: defaultTopic

    client?.connect()
    client?.unsubscribe(topicName)
    topicAddressBridge.remove topicName
    client?.disconnect()
  }

  @Override
  public void connectionLost(Throwable t) {
    // TODO retry connect() if retryConnectionCounter < 5?
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    // TODO Auto-generated method stub
  }

  @Override
  public void messageArrived(String topic, MqttMessage msg) throws Exception {
    String address = topicAddressBridge.get(topic)
    vertx.eventBus.send(address, msg.payload)
  }

}
