/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flume.source;

import org.apache.commons.io.IOUtils;

import org.apache.commons.io.LineIterator;
import org.apache.flume.Channel;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
import org.apache.flume.lifecycle.LifecycleController;
import org.apache.flume.lifecycle.LifecycleState;
import org.jboss.netty.channel.ChannelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestNetcatSource {
  private static final Logger logger =
      LoggerFactory.getLogger(TestAvroSource.class);

  /**
   * Five first sentences of the Fables "The Crow and the Fox"
   * written by Jean de La Fontaine, French poet.
   *
   * @see <a href="http://en.wikipedia.org/wiki/Jean_de_La_Fontaine">Jean de La Fontaine on
   * wikipedia</a>
   */
  private final String french = "Maître Corbeau, sur un arbre perché, " +
      "Tenait en son bec un fromage. " +
      "Maître Renard, par l'odeur alléché, " +
      "Lui tint à peu près ce langage : " +
      "Et bonjour, Monsieur du Corbeau,";

  private final String english = "At the top of a tree perched Master Crow; " +
      "In his beak he was holding a cheese. " +
      "Drawn by the smell, Master Fox spoke, below. " +
      "The words, more or less, were these: " +
      "\"Hey, now, Sir Crow! Good day, good day!";

  private int selectedPort;
  private NetcatSource source;
  private Channel channel;
  private InetAddress localhost;
  private Charset defaultCharset = Charset.forName("UTF-8");

  /**
   * We set up the the Netcat source with a mock Channel on localhost
   */
  @Before
  public void setUp() throws UnknownHostException {
    localhost = InetAddress.getByName("127.0.0.1");
    source = new NetcatSource();
    final BlockingQueue<Event> channelsQueue = new ArrayBlockingQueue<>(100);
    channel = mock(Channel.class);
    when(channel.getTransaction()).thenReturn(mock(Transaction.class));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Event event = (Event) invocation.getArguments()[0];
        channelsQueue.put(event);
        return null;
      }
    }).when(channel).put(Mockito.<Event>any());
    doAnswer(new Answer<Event>() {
      @Override
      public Event answer(InvocationOnMock invocation) throws Throwable {
        return channelsQueue.poll(1, TimeUnit.SECONDS);
      }
    }).when(channel).take();

    Configurables.configure(channel, new Context());

    ChannelSelector rcs = new ReplicatingChannelSelector();
    rcs.setChannels(Collections.singletonList(channel));

    source.setChannelProcessor(new ChannelProcessor(rcs));
  }

  /**
   * Test with UTF-16BE encoding Text with both french and english sentences
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testUTF16BEencoding() throws InterruptedException, IOException {
    String encoding = "UTF-16BE";
    startSource(encoding, "false", "1", "512");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    try {
      // Test on english text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, english, encoding);
        Assert.assertArrayEquals("Channel contained our event", english.getBytes(defaultCharset),
            getFlumeEvent());
      }
      // Test on french text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, french, encoding);
        Assert.assertArrayEquals("Channel contained our event", french.getBytes(defaultCharset),
            getFlumeEvent());
      }
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test with UTF-16LE encoding Text with both french and english sentences
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testUTF16LEencoding() throws InterruptedException, IOException {
    String encoding = "UTF-16LE";
    startSource(encoding, "false", "1", "512");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    try {
      // Test on english text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, english, encoding);
        Assert.assertArrayEquals("Channel contained our event", english.getBytes(defaultCharset),
            getFlumeEvent());
      }
      // Test on french text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, french, encoding);
        Assert.assertArrayEquals("Channel contained our event", french.getBytes(defaultCharset),
            getFlumeEvent());
      }
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test with UTF-8 encoding Text with both french and english sentences
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testUTF8encoding() throws InterruptedException, IOException {
    String encoding = "UTF-8";
    startSource(encoding, "false", "1", "512");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    try {
      // Test on english text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, english, encoding);
        Assert.assertArrayEquals("Channel contained our event", english.getBytes(defaultCharset),
            getFlumeEvent());
      }
      // Test on french text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, french, encoding);
        Assert.assertArrayEquals("Channel contained our event", french.getBytes(defaultCharset),
            getFlumeEvent());
      }
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test with ISO-8859-1 encoding Text with both french and english sentences
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testIS88591encoding() throws InterruptedException, IOException {
    String encoding = "ISO-8859-1";
    startSource(encoding, "false", "1", "512");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    try {
      // Test on english text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, english, encoding);
        Assert.assertArrayEquals("Channel contained our event", english.getBytes(defaultCharset),
            getFlumeEvent());
      }
      // Test on french text snippet
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, french, encoding);
        Assert.assertArrayEquals("Channel contained our event", french.getBytes(defaultCharset),
            getFlumeEvent());
      }
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test if the correct ack message is sent for every event
   */
  @Test
  public void testCustomAckMessage() throws InterruptedException, IOException {
    testAcknowledgement(english, new char[] {6}, "\u0006");
    testAcknowledgement(french, new char[] {6}, "\u0006");
  }

  /**
   * Test if the default ack message is sent for every event in the correct encoding
   */
  @Test
  public void testDefaultAckMessage() throws InterruptedException, IOException {
    testAcknowledgement(english,
        NetcatSourceConfigurationConstants.DEFAULT_ACKNOWLEDGEMENT_MESSAGE.toCharArray(),
        null);
    testAcknowledgement(french,
        NetcatSourceConfigurationConstants.DEFAULT_ACKNOWLEDGEMENT_MESSAGE.toCharArray(),
        null);
  }

  private void testAcknowledgement(String eventBody, char[] expectedAck, String configuredAck)
      throws InterruptedException, IOException { 
    String encoding = "UTF-8";
    char[] actualAck = new char[expectedAck.length];
    startSource(encoding, "true", "1", "512", configuredAck);
    Socket netcatSocket = new Socket(localhost, selectedPort);
    InputStreamReader ackReader = new InputStreamReader(netcatSocket.getInputStream(), encoding);
    try {
      for (int i = 0; i < 20; i++) {
        sendEvent(netcatSocket, eventBody, encoding);
        Assert.assertArrayEquals("Channel contained our event", eventBody.getBytes(defaultCharset),
            getFlumeEvent());
        Assert.assertEquals("Could read acknowledgement fully",
            expectedAck.length, ackReader.read(actualAck));
        Assert.assertArrayEquals("Socket contained the correct ack", expectedAck, actualAck);
      }
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test that line above MaxLineLength are discarded
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testMaxLineLength() throws InterruptedException, IOException {
    String encoding = "UTF-8";
    startSource(encoding, "false", "1", "10");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    try {
      sendEvent(netcatSocket, "123456789", encoding);
      Assert.assertArrayEquals("Channel contained our event",
                               "123456789".getBytes(defaultCharset), getFlumeEvent());
      sendEvent(netcatSocket, english, encoding);
      Assert.assertEquals("Channel does not contain an event", null, getRawFlumeEvent());
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  /**
   * Test that line above MaxLineLength are discarded
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void testMaxLineLengthwithAck() throws InterruptedException, IOException {
    String encoding = "UTF-8";
    String ackEvent = "OK";
    String ackErrorEvent = "FAILED: Event exceeds the maximum length (10 chars, including newline)";
    startSource(encoding, "true", "1", "10");
    Socket netcatSocket = new Socket(localhost, selectedPort);
    LineIterator inputLineIterator = IOUtils.lineIterator(netcatSocket.getInputStream(), encoding);
    try {
      sendEvent(netcatSocket, "123456789", encoding);
      Assert.assertArrayEquals("Channel contained our event",
                               "123456789".getBytes(defaultCharset), getFlumeEvent());
      Assert.assertEquals("Socket contained the Ack", ackEvent, inputLineIterator.nextLine());
      sendEvent(netcatSocket, english, encoding);
      Assert.assertEquals("Channel does not contain an event", null, getRawFlumeEvent());
      Assert.assertEquals("Socket contained the Error Ack", ackErrorEvent, inputLineIterator
          .nextLine());
    } finally {
      netcatSocket.close();
      stopSource();
    }
  }

  private void startSource(String encoding, String ack, String batchSize, String maxLineLength)
      throws InterruptedException {
    startSource(encoding, ack, batchSize, maxLineLength, null);
  }

  private void startSource(String encoding, String ack, String batchSize, String maxLineLength,
      String ackMessage) throws InterruptedException {
    boolean bound = false;

    for (int i = 0; i < 100 && !bound; i++) {
      try {
        Context context = new Context();
        context.put("port", String.valueOf(selectedPort = 10500 + i));
        context.put("bind", "0.0.0.0");
        context.put("ack-every-event", ack);
        if (ackMessage != null) {
          context.put("ack-message", ackMessage);
        }
        context.put("encoding", encoding);
        context.put("batch-size", batchSize);
        context.put("max-line-length", maxLineLength);

        Configurables.configure(source, context);

        source.start();
        bound = true;
      } catch (ChannelException e) {
        /*
         * NB: This assume we're using the Netty server under the hood and the
         * failure is to bind. Yucky.
         */
      }
    }

    Assert.assertTrue("Reached start or error",
        LifecycleController.waitForOneOf(source, LifecycleState.START_OR_ERROR));
    Assert.assertEquals("Server is started", LifecycleState.START,
        source.getLifecycleState());
  }

  private void sendEvent(Socket socket, String content, String encoding) throws IOException {
    OutputStream output = socket.getOutputStream();
    IOUtils.write(content + IOUtils.LINE_SEPARATOR_UNIX, output, encoding);
    output.flush();
  }

  private byte[] getFlumeEvent() {
    return getRawFlumeEvent().getBody();
  }

  private Event getRawFlumeEvent() {
    Event event = channel.take();
    logger.debug("Round trip event:{}", event);

    return event;
  }

  private void stopSource() throws InterruptedException {
    source.stop();
    Assert.assertTrue("Reached stop or error",
        LifecycleController.waitForOneOf(source, LifecycleState.STOP_OR_ERROR));
    Assert.assertEquals("Server is stopped", LifecycleState.STOP,
        source.getLifecycleState());
    logger.info("Source stopped");
  }
}
