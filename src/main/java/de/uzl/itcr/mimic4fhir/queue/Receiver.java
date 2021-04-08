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

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import de.uzl.itcr.mimic4fhir.OutputMode;
import de.uzl.itcr.mimic4fhir.work.FHIRComm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RabbitMQ consumer class for receiving and processing bundles
 * 
 * @author Stefanie Ververs
 *
 */
public class Receiver {
	private final static Logger logger = LoggerFactory.getLogger(Receiver.class);
	private final static String QUEUE_NAME = "BundleQ";
	private Channel channel;
	private FHIRComm fhirConnector;
	private OutputMode outputMode;
	private Connection connection;

	/**
	 * Constructor - creates new channel connection
	 */
	public Receiver() {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			logger.debug(String.valueOf(channel == null));
		} catch (Exception e) {
			logger.error("Exception has been thrown!");
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Set the Fhir-Communication class
	 * 
	 * @param fhirCom
	 */
	public void setFhirConnector(FHIRComm fhirCom) {
		this.fhirConnector = fhirCom;
	}

	/**
	 * Set the outputMode (how to process bundles)
	 * 
	 * @param outputMode
	 */
	public void setOutputMode(OutputMode outputMode) {
		this.outputMode = outputMode;
	}

	/**
	 * Start listening (and receiving) messages
	 */
	public void receive() {

		try {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {

					// bundle xml from json message data
					InputStream is = new ByteArrayInputStream(body);
					JsonReader jsonReader = Json.createReader(is);
					JsonObject json = jsonReader.readObject();
					jsonReader.close();

					String number = json.getString("number");
					String bundleXml = json.getString("bundle");

					if (bundleXml.equals("END")) {
						// End this queue..
						channel.basicCancel(consumerTag);
						try {
							channel.close();
							connection.close();
						} catch (TimeoutException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						// process bundle
						try {
							performAction(number, bundleXml);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}
			};
			channel.basicConsume(QUEUE_NAME, true, consumer);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void performAction(String number, String bundle) {
		// Perform action for bundle
		switch (outputMode) {
		case PRINT_CONSOLE:
			fhirConnector.printBundleAsXml(fhirConnector.getBundleFromString(bundle));
			break;
		case PRINT_FILE:
			fhirConnector.printBundleAsXmlToFile(number, fhirConnector.getBundleFromString(bundle));
			break;
		case PRINT_BOTH:
			fhirConnector.printBundleAsXml(fhirConnector.getBundleFromString(bundle));
			fhirConnector.printBundleAsXmlToFile(number, fhirConnector.getBundleFromString(bundle));
			break;
		case PUSH_SERVER:
			fhirConnector.bundleToServer(fhirConnector.getBundleFromString(bundle));
			break;
		}
	}
}
