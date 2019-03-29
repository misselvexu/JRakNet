/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Trent "Whirvis" Summerlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.whirvis.jraknet;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.peer.RakNetServerPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.BlockedAddress;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;

/**
 * Tests the sequenced packet feature of the
 * {@link com.whirvis.jraknet.peer.RakNetPeer RakNetPeer} through a stress test.
 * <p>
 * This stress test sends 1000 packets using the
 * {@link Reliability#UNRELIABLE_SEQUENCED UNRELIABLE_SEQUENCED} reliability,
 * and also reports if they were lost or received too late.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.0.0
 */
public final class SequencedPacketTest {

	private static final Logger LOG = LogManager.getLogger(SequencedPacketTest.class);
	private static final short SEQUENCE_START_ID = 0xFE;
	private static final int PACKET_SEND_COUNT = 1000;
	private static long startSend = -1;
	private static int packetReceiveCount = 0;
	private static boolean[] packetsReceived = new boolean[PACKET_SEND_COUNT];

	private SequencedPacketTest() {
		// Static class
	}

	/**
	 * The entry point for the test.
	 * 
	 * @param args
	 *            the program arguments. These values are ignored.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread. The
	 *             <i>interrupted status</i> of the current thread is cleared
	 *             when this exception is thrown.
	 * @throws UnknownHostException
	 *             if the <code>localhost</code> address could not be found.
	 */
	public static void main(String[] args) throws RakNetException, InterruptedException, UnknownHostException {
		LOG.info("Creating server, sleeping for 3000MS, and then creating the client...");
		createServer();
		RakNet.sleep(3000L);
		createClient();

		// Wait for either a result or for a timeout
		long currentTime = System.currentTimeMillis();
		while (true) {
			Thread.sleep(0, 1); // Lower CPU usage
			if (currentTime - startSend >= 30000 && startSend > -1) {
				LOG.info("Failed to complete test due to timeout (Took over 30 seconds!), printing results...");
				printResults();
				System.exit(1);
			}
			currentTime = System.currentTimeMillis();
		}
	}

	/**
	 * Creates the server for the test.
	 * 
	 * @return the server that will receive the sequenced packets.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 */
	private static RakNetServer createServer() throws RakNetException {
		RakNetServer server = new RakNetServer(RakNetTest.WHIRVIS_DEVELOPMENT_PORT, 1);
		server.addListener(new RakNetServerListener() {

			@Override
			public void onLogin(RakNetServer server, RakNetClientPeer peer) {
				if (RakNet.isLocalAddress(peer.getAddress())) {
					LOG.info("Server - Client logged in from " + peer.getAddress());
				} else {
					server.disconnect(peer, "Session is not from local address");
					server.blockAddress(peer.getInetAddress(), BlockedAddress.PERMANENT_BLOCK);
				}
			}

			@Override
			public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer,
					String reason) {
				LOG.info("Server - Client from " + address + " disconnected (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
				int packetIndex = packet.readInt();
				packetReceiveCount++;
				packetsReceived[packetIndex] = true;
				if (packetIndex >= PACKET_SEND_COUNT - 1) {
					printResults();
					System.exit(0);
				}
			}

			@Override
			public void onHandlerException(RakNetServer server, InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});
		server.start();
		return server;
	}

	/**
	 * Creates the client for the test.
	 * 
	 * @return the client that will be sending the sequenced packets.
	 * @throws RakNetException
	 *             if a RakNet error occurs.
	 * @throws UnknownHostException
	 *             if the <code>localhost</code> address could not be found.
	 */
	private static RakNetClient createClient() throws RakNetException, UnknownHostException {
		RakNetClient client = new RakNetClient();
		client.addListener(new RakNetClientListener() {

			@Override
			public void onLogin(RakNetClient client, RakNetServerPeer peer) {
				LOG.info("Client - Logged in to server with MTU " + peer.getMaximumTransferUnit() + ", sending "
						+ PACKET_SEND_COUNT + " packets...");
				int packetSize = 0;
				startSend = System.currentTimeMillis();
				for (int i = 0; i < PACKET_SEND_COUNT; i++) {
					RakNetPacket sequencedPacket = new RakNetPacket(SEQUENCE_START_ID);
					sequencedPacket.writeInt(i);
					packetSize += sequencedPacket.size();
					peer.sendMessage(Reliability.UNRELIABLE_SEQUENCED, sequencedPacket);
				}
				LOG.info("Client - Sent " + PACKET_SEND_COUNT + " packets (" + packetSize + " bytes, "
						+ (packetSize / 4) + " ints)");
			}

			@Override
			public void onDisconnect(RakNetClient client, RakNetServerPeer peer, String reason) {
				LOG.error("Client - Lost connection to server (" + reason + ")");
				System.exit(1);
			}

			@Override
			public void onHandlerException(RakNetClient client, InetSocketAddress address, Throwable cause) {
				cause.printStackTrace();
				System.exit(1);
			}

		});
		client.connect("localhost", RakNetTest.WHIRVIS_DEVELOPMENT_PORT);
		return client;
	}

	/**
	 * Prints the results of the test.
	 */
	private static void printResults() {
		LOG.info("Server - Sequenced packet test finished, lost "
				+ (packetReceiveCount >= PACKET_SEND_COUNT ? "no"
						: Float.toString(
								((float) PACKET_SEND_COUNT - packetReceiveCount / (float) PACKET_SEND_COUNT) * 100)
								.substring(0, 3).replace(".", "") + "% of")
				+ " packets (Took " + (System.currentTimeMillis() - startSend) + "MS)");
		if (packetReceiveCount < PACKET_SEND_COUNT) {
			ArrayList<Integer> lostPackets = new ArrayList<Integer>();
			for (int i = 0; i < packetsReceived.length; i++) {
				if (packetsReceived[i] == false) {
					lostPackets.add(i);
				}
			}
			StringBuilder lostPacketsBuilder = new StringBuilder();
			for (int i = 0; i < lostPackets.size(); i++) {
				Integer wi = lostPackets.get(i);
				lostPacketsBuilder.append(wi.intValue() + (i + 1 < lostPackets.size() ? ", " : ""));
			}
			LOG.info("Packet" + (lostPackets.size() == 1 ? "" : "s") + " lost: " + lostPacketsBuilder.toString());
		}
	}

}
