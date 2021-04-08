/***********************************************************************
Copyright 2018 Stefanie Ververs, University of Lübeck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
/***********************************************************************/
package de.uzl.itcr.mimic4fhir.queue;

import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Publisher class for sending bundle message to RabbitMQ
 * @author Stefanie Ververs
 *
 */
public class Sender {
	private final static String QUEUE_NAME = "BundleQ";
	private Channel channel;
	private Connection connection;
	
	/**
	 * Constructor - creates new channel connection
	 */
	public Sender() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	/**
	 * Send message to queue
	 * @param message json message with number and bundle data
	 */
	public void send(String message) {
		try {
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Close channel connection
	 */
	public void close() {
		try {
			this.channel.close();
			this.connection.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}
}
