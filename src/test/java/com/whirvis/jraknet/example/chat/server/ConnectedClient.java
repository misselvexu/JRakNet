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
package com.whirvis.jraknet.example.chat.server;

import java.util.UUID;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.example.chat.ChatMessageIdentifier;
import com.whirvis.jraknet.example.chat.ServerChannel;
import com.whirvis.jraknet.example.chat.protocol.AddChannelPacket;
import com.whirvis.jraknet.example.chat.protocol.ChatMessagePacket;
import com.whirvis.jraknet.example.chat.protocol.KickPacket;
import com.whirvis.jraknet.example.chat.protocol.LoginAcceptedPacket;
import com.whirvis.jraknet.example.chat.protocol.RemoveChannelPacket;
import com.whirvis.jraknet.example.chat.protocol.RenameChannelPacket;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.session.InvalidChannelException;
import com.whirvis.jraknet.session.RakNetClientSession;

/**
 * Represents a client connect to a <code>ChatServer</code> and is used to easy
 * set status, send chat messages, etc.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class ConnectedClient {

	public static final int USER_STATUS_CLIENT_CONNECTED = 0x00;
	public static final int USER_STATUS_CLIENT_DISCONNECTED = 0x01;

	private final RakNetClientSession session;
	private final UUID uuid;
	private String username;

	/**
	 * Constructs a <code>ConnectedClient</code> with the
	 * <code>RakNetClientSession</code>, <code>UUID</code> and username.
	 * 
	 * @param session
	 *            the <code>RakNetSession</code>.
	 * @param uuid
	 *            the <code>UUID</code>.
	 * @param username
	 *            the username.
	 */
	public ConnectedClient(RakNetClientSession session, UUID uuid, String username) {
		this.session = session;
		this.uuid = uuid;
		this.username = username;
	}

	/**
	 * Returns the assigned UUID of the client.
	 * 
	 * @return the assigned UUID of the client.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Returns the username of the client.
	 * 
	 * @return the username of the client.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Returns the session of the client.
	 * 
	 * @return the session of the client.
	 */
	public RakNetClientSession getSession() {
		return this.session;
	}

	/**
	 * Accepts the client's requested login and sends the data required for the
	 * client to display the server data properly.
	 * 
	 * @param name
	 *            the name of the server.
	 * @param motd
	 *            the server message of the day.
	 * @param channels
	 *            the channels the client can use.
	 */
	public void acceptLogin(String name, String motd, ServerChannel[] channels) {
		LoginAcceptedPacket accepted = new LoginAcceptedPacket();
		accepted.userId = this.uuid;
		accepted.serverName = name;
		accepted.serverMotd = motd;
		accepted.channels = channels;
		accepted.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, accepted);
	}

	/**
	 * Tells the client its new username has been accepted.
	 * 
	 * @param username
	 *            the client's new username.
	 */
	public void acceptUsernameUpdate(String username) {
		this.username = username;
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_ACCEPTED);
	}

	/**
	 * Tells the client its new username has been denied.
	 */
	public void denyUsernameUpdate() {
		session.sendMessage(Reliability.RELIABLE_ORDERED, ChatMessageIdentifier.ID_UPDATE_USERNAME_FAILURE);
	}

	/**
	 * Sends a chat message to the client on the channel.
	 * 
	 * @param message
	 *            the message to send.
	 * @param channel
	 *            the channel to send the message on.
	 */
	public void sendChatMessage(String message, int channel) {
		if (channel >= RakNet.MAX_CHANNELS) {
			throw new InvalidChannelException();
		}

		ChatMessagePacket chat = new ChatMessagePacket();
		chat.message = message;
		chat.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, channel, chat);
	}

	/**
	 * Notifies the client of a new channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the name of the channel.
	 */
	public void addChannel(int channel, String name) {
		AddChannelPacket addChannel = new AddChannelPacket();
		addChannel.channel = channel;
		addChannel.channelName = name;
		addChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, addChannel);
	}

	/**
	 * Notifies the client of a channel rename.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 * @param name
	 *            the new name of the channel.
	 */
	public void renameChannel(int channel, String name) {
		RenameChannelPacket renameChannel = new RenameChannelPacket();
		renameChannel.channel = channel;
		renameChannel.newChannelName = name;
		renameChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, renameChannel);
	}

	/**
	 * Notifies the client of a removed channel.
	 * 
	 * @param channel
	 *            the ID of the channel.
	 */
	public void removeChannel(int channel) {
		RemoveChannelPacket removeChannel = new RemoveChannelPacket();
		removeChannel.channel = channel;
		removeChannel.encode();
		session.sendMessage(Reliability.RELIABLE_ORDERED, removeChannel);
	}

	/**
	 * Kicks the client.
	 * 
	 * @param reason
	 *            the reason the client was kicked.
	 */
	public void kick(String reason) {
		KickPacket kick = new KickPacket();
		kick.reason = reason;
		kick.encode();
		session.sendMessage(Reliability.UNRELIABLE, kick);
	}

}
