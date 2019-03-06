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
package com.whirvis.jraknet.protocol.connection;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;

/**
 * A <code>CONNECTION_BANNED</code> packet.
 * <p>
 * This packet is sent by the server to the client if it has been banned either
 * during connection or while it is connected to the server.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v1.0.0
 */
public class ConnectionBanned extends RakNetPacket {
	
	/**
	 * Whether or not the magic is valid.
	 */
	public boolean magic;

	/**
	 * The server's globally unique identifier.
	 */
	public long serverGuid;

	/**
	 * Creates a <code>CONNECTION_BANNED</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public ConnectionBanned() {
		super(ID_CONNECTION_BANNED);
	}

	/**
	 * Creates a <code>CONNECTION_BANNED</code> packet to be decoded.
	 * 
	 * @param packet
	 *            the original packet whose data will be read from in the
	 *            {@link #decode()} method.
	 */
	public ConnectionBanned(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeMagic();
		this.writeLong(serverGuid);
	}

	@Override
	public void decode() {
		this.magic = this.readMagic();
		this.serverGuid = this.readLong();
	}

}
