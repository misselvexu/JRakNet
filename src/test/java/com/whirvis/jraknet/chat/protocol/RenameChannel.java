/*
 *    __     ______     ______     __  __     __   __     ______     ______  
 *   /\ \   /\  == \   /\  __ \   /\ \/ /    /\ "-.\ \   /\  ___\   /\__  _\
 *  _\_\ \  \ \  __<   \ \  __ \  \ \  _"-.  \ \ \-.  \  \ \  __\   \/_/\ \/  
 * /\_____\  \ \_\ \_\  \ \_\ \_\  \ \_\ \_\  \ \_\\"\_\  \ \_____\    \ \_\ 
 * \/_____/   \/_/ /_/   \/_/\/_/   \/_/\/_/   \/_/ \/_/   \/_____/     \/_/                                                                          
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
package com.whirvis.jraknet.chat.protocol;

import com.whirvis.jraknet.Packet;

/**
 * A <code>RENAME_CHANNEL</code> packet.
 * <p>
 * This packet is sent by the {@link com.whirvis.jraknet.chat.server.ChatServer
 * ChatServer} when it has renamed a channel.
 * 
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.0.0
 */
public final class RenameChannel extends ChatPacket {

	/**
	 * The channel being renamed.
	 */
	public int channel;

	/**
	 * The new channel name.
	 */
	public String newChannelName;

	/**
	 * Creates a <code>RENAME_CHANNEL</code> packet to be encoded.
	 * 
	 * @see #encode()
	 */
	public RenameChannel() {
		super(ID_RENAME_CHANNEL);
	}

	/**
	 * Creates an <code>RENAME_CHANNEL</code> packet to be decoded.
	 * 
	 * @param packet the original packet whose data will be read from in the
	 *               {@link #decode()} method.
	 */
	public RenameChannel(Packet packet) {
		super(packet);
	}

	@Override
	public void encode() {
		this.writeUnsignedByte(channel);
		this.writeString(newChannelName);
	}

	@Override
	public void decode() {
		this.channel = this.readUnsignedByte();
		this.newChannelName = this.readString();
	}

}
