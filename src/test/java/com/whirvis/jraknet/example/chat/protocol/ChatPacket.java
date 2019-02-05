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
package com.whirvis.jraknet.example.chat.protocol;

import java.util.UUID;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.protocol.MessageIdentifier;

/**
 * Used to read and write data related to the chat protocol used for the
 * example.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class ChatPacket extends RakNetPacket {

	/**
	 * Constructs a <code>ChatPacket</code> with the ID.
	 * 
	 * @param id
	 *            The ID of the packet.
	 */
	public ChatPacket(int id) {
		super(id);
		if (id < MessageIdentifier.ID_USER_PACKET_ENUM) {
			throw new IllegalArgumentException("Packet ID too low!");
		}
	}

	/**
	 * Constructs a <code>ChatPacket</code> that reads from and writes to the
	 * <code>Packet</code>.
	 * 
	 * @param packet
	 *            the <code>Packet</code> to read from and write to.
	 */
	public ChatPacket(Packet packet) {
		super(packet);
	}

	/**
	 * Reads a <code>UUID</code>.
	 * 
	 * @return a <code>UUID</code>.
	 */
	public UUID readUUID() {
		long mostSigBits = this.readLong();
		long leastSigBits = this.readLong();
		return new UUID(mostSigBits, leastSigBits);
	}

	/**
	 * Writes a <code>UUID</code>.
	 * 
	 * @param uuid
	 *            the <code>UUID</code>.
	 * @return The packet.
	 */
	public ChatPacket writeUUID(UUID uuid) {
		this.writeLong(uuid.getMostSignificantBits());
		this.writeLong(uuid.getLeastSignificantBits());
		return this;
	}

}
