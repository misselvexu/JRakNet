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
package com.whirvis.jraknet.peer;

/**
 * Represents the current status of a connection in a {@link RakNetPeer}.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 */
public enum RakNetState {

	/**
	 * The peer is connected.
	 * <p>
	 * This is the starting value of the state for all peers; as it is assumed a
	 * peer has connected by the time it is created.
	 */
	CONNECTED(0),

	/**
	 * The peer is handshaking.
	 */
	HANDSHAKING(1),

	/**
	 * The peer is logged in.
	 */
	LOGGED_IN(2),

	/**
	 * The peer is disconnected.
	 */
	DISCONNECTED(-1);

	private final int order;

	/**
	 * Constructs a <code>RakNetState</code> with the specified order.
	 * 
	 * @param order
	 *            the order of the state.
	 */
	private RakNetState(int order) {
		this.order = order;
	}

	/**
	 * Returns the order of the state.
	 * 
	 * @return the order of the state.
	 */
	public int getOrder() {
		return this.order;
	}

}
