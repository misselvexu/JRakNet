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
package com.whirvis.jraknet.example.chat.server.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.example.chat.server.ChatServer;

/**
 * Allows the server to add, remove, and rename channels.
 *
 * @author Trent "Whirvis" Summerlin
 */
public class ChannelCommand extends Command {

	private static final Logger LOG = LogManager.getLogger(ChannelCommand.class);
	
	private final ChatServer server;

	public ChannelCommand(ChatServer server) {
		super("channel", "<add:remove> [id] <name>", "Used to modify channel data");
		this.server = server;
	}

	@Override
	public boolean handleCommand(String[] args) {
		if (args.length >= 2) {
			int channelId = RakNet.parseIntPassive(args[1]);

			// Do we add or remove a channel?
			if (args[0].equalsIgnoreCase("add")) {
				// No channel was set, we must try to determine one
				boolean hadId = true;
				if (channelId < 0) {
					hadId = false;
					channelId = 0;
					while (!server.hasChannel(channelId)) {
						channelId++;
						if (channelId >= RakNet.MAX_CHANNELS) {
							LOG.error("Unable to add channel, either remove some or assign an ID manually!");
							return true;
						}
					}
				} else {
					// Channel was preset but there is no name!
					if (args.length < 3) {
						LOG.error("Failed to add channel with ID " + channelId + ", no name was provided!");
						return true;
					}
				}

				// Add the channel and notify the server
				String channelName = remainingArguments((hadId ? 2 : 1), args);
				server.addChannel(channelId, channelName);
				LOG.info("Added channel \"" + channelName + "\" with ID " + channelId);
				return true;
			} else if (args[0].equalsIgnoreCase("rename")) {
				if (args.length >= 3) {
					// Does the channel exist yet?
					if (!server.hasChannel(channelId)) {
						LOG.error("Channel with ID " + channelId + " has not yet been created!");
						return true;
					}

					// Rename the channel and notify the server
					String channelName = server.getChannelName(channelId);
					server.renameChannel(channelId, remainingArguments(2, args));
					LOG.info("Renamed channel with ID " + channelId + " from \"" + channelName + "\" to \""
							+ server.getChannelName(channelId) + "\"");
					return true;
				}
			} else if (args[0].equalsIgnoreCase("remove")) {
				// Does the channel exist yet?
				if (!server.hasChannel(channelId)) {
					LOG.error("Channel was ID " + channelId + " has not yet been created!");
					return true;
				}

				// Remove the channel and notify the server
				String channelName = server.getChannelName(channelId);
				server.removeChannel(channelId);
				LOG.info("Removed channel \"" + channelName + "\"");
				return true;
			}
		}
		return false;
	}

}
