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
package com.whirvis.jraknet.interactive;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.whirvis.jraknet.RakNetTest;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;
import com.whirvis.jraknet.server.RakNetServer;

/**
 * Used to test the latency feature in <code>RakNetSession</code>.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class LatencyTest {

	private static final MinecraftIdentifier LATENCY_TEST_IDENTIFIER = new MinecraftIdentifier("A JRakNet latency test",
			RakNetTest.MINECRAFT_PROTOCOL_NUMBER, RakNetTest.MINECRAFT_VERSION, 0, 10,
			-1 /* We don't know the GUID yet */, "New World", "Developer");

	private final RakNetServer server;
	private final LatencyFrame frame;

	public LatencyTest() {
		this.server = new RakNetServer(RakNetTest.MINECRAFT_DEFAULT_PORT, LATENCY_TEST_IDENTIFIER.getMaxPlayerCount());
		this.frame = new LatencyFrame();
	}

	/**
	 * Starts the test.
	 * 
	 * @throws InterruptedException
	 *             if the thread is interrupted while it is sleeping.
	 */
	public void start() throws InterruptedException {

		// Set server options and start it
		LATENCY_TEST_IDENTIFIER.setServerGloballyUniqueId(server.getGloballyUniqueId());
		server.setIdentifier(LATENCY_TEST_IDENTIFIER);
		server.startThreaded();

		// Create window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		while (true) {
			Thread.sleep(500); // Lower CPU usage and give window time to update
			frame.updatePaneText(server.getSessions());
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException, InterruptedException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		LatencyTest test = new LatencyTest();
		test.start();
	}

}
