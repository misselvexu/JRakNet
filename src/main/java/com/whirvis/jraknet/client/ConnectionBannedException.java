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
package com.whirvis.jraknet.client;

import java.net.InetSocketAddress;

/**
 * Signals that a RakNet client is attempting to connect to a server that has
 * banned its connection.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.0
 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
 * @see com.whirvis.jraknet.client.RakNetClientException RakNetClientException
 */
public class ConnectionBannedException extends RakNetClientException {

	private static final long serialVersionUID = 8440218445920818619L;

	private final InetSocketAddress address;

	/**
	 * Constructs a <code>ConnectedionBannedException</code>.
	 * 
	 * @param client
	 *            the client that threw the exception.
	 * @param address
	 *            the address of the server that banned the client's connection.
	 * @see com.whirvis.jraknet.client.RakNetClient RakNetClient
	 */
	public ConnectionBannedException(RakNetClient client, InetSocketAddress address) {
		super(client, "Connection banned");
		this.address = address;
	}

	/**
	 * Returns the address of the server that banned the client's connection.
	 * 
	 * @return the address of the server that banned the client's connection.
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

}
