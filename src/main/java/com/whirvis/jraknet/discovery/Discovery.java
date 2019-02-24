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
package com.whirvis.jraknet.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.identifier.Identifier;
import com.whirvis.jraknet.protocol.status.UnconnectedPong;
import com.whirvis.jraknet.scheduler.Scheduler;

/**
 * Used to discover servers on the local network and on external networks.
 *
 * @author Trent "Whirvis" Summerlin
 * @since JRakNet v2.11.0
 * @see DiscoveryMode
 * @see DiscoveredServer
 * @see DiscoveryListener
 * @see #setDiscoveryMode(DiscoveryMode)
 * @see #addListener(DiscoveryListener)
 */
public class Discovery {

	/**
	 * The address to broadcast to in order to discover servers on the local
	 * network.
	 */
	private static final String BROADCAST_ADDRESS = "255.255.255.255";

	/**
	 * The server is a server discovered on the local network.
	 */
	private static final boolean LOCAL_SERVER = false;

	/**
	 * The server is a server discovered on an external network.
	 */
	private static final boolean EXTERNAL_SERVER = true;

	/**
	 * Used to convert a {@link java.util.stream.Stream Stream} to a
	 * <code>InetSocketAddress[]</code>.
	 */
	private static final IntFunction<InetSocketAddress[]> INETSOCKETADDRESS_FUNCTION = new IntFunction<InetSocketAddress[]>() {

		@Override
		public InetSocketAddress[] apply(int value) {
			return new InetSocketAddress[value];
		}

	};

	/**
	 * Used to convert a {@link java.util.stream.Stream Stream} to a
	 * <code>DiscoveredServer[]</code>.
	 */
	private static final IntFunction<DiscoveredServer[]> DISCOVERED_SERVER_FUNCTION = new IntFunction<DiscoveredServer[]>() {

		@Override
		public DiscoveredServer[] apply(int value) {
			return new DiscoveredServer[value];
		}

	};

	private static final Logger LOG = LogManager.getLogger("jraknet-discovery");
	private static final long TIMESTAMP = System.currentTimeMillis();
	private static final long PING_ID = UUID.randomUUID().getLeastSignificantBits();
	protected static DiscoveryMode discoveryMode = DiscoveryMode.ALL_CONNECTIONS;
	protected static final ConcurrentLinkedQueue<DiscoveryListener> LISTENERS = new ConcurrentLinkedQueue<DiscoveryListener>();
	protected static final ConcurrentHashMap<InetSocketAddress, Boolean> DISCOVERY_ADDRESSES = new ConcurrentHashMap<InetSocketAddress, Boolean>();
	protected static final ConcurrentHashMap<InetSocketAddress, DiscoveredServer> DISCOVERED = new ConcurrentHashMap<InetSocketAddress, DiscoveredServer>();
	protected static DiscoveryThread thread = null;

	private Discovery() {
		// Static class
	}

	/**
	 * Returns the timestamp of the discovery system. The timestamp of the
	 * discovery system is how long in milliseconds has passed since it has been
	 * created.
	 * 
	 * @return the timestamp of the discovery system.
	 */
	protected static long getTimestamp() {
		return System.currentTimeMillis() - TIMESTAMP;
	}

	/**
	 * Returns the ping ID of the discovery system.
	 * 
	 * @return the ping ID of the discovery system.
	 */
	protected static long getPingId() {
		return PING_ID;
	}

	/**
	 * Returns the discovery mode. The discovery mode determines how server
	 * server discovery is handled.
	 * 
	 * @return the discovery mode.
	 */
	public static DiscoveryMode getDiscoveryMode() {
		if (discoveryMode == null) {
			discoveryMode = DiscoveryMode.DISABLED;
		}
		return discoveryMode;
	}

	/**
	 * Sets the discovery mode. If disabling the discovery system, all of the
	 * currently discovered servers will be treated as if they had been
	 * forgotten. If discovery is enabled once again later on, all servers
	 * listed for discovery via {@link #addPort(int)} and
	 * {@link #addServer(InetSocketAddress)} will be rediscovered. To stop this
	 * from occurring, they can be removed via {@link #removePort(int)} and
	 * {@link #removeServer(DiscoveredServer)}
	 * 
	 * @param mode
	 *            the new discovery mode. A <code>null</code> value will have
	 *            the discovery mode be set to {@link DiscoveryMode#DISABLED}.
	 */
	public static synchronized void setDiscoveryMode(DiscoveryMode mode) {
		discoveryMode = (mode == null ? DiscoveryMode.DISABLED : mode);
		if (discoveryMode == DiscoveryMode.DISABLED) {
			DISCOVERY_ADDRESSES.keySet().stream().filter(address -> DISCOVERED.containsKey(address))
					.forEach(address -> callEvent(listener -> listener.onServerForgotten(DISCOVERED.get(address))));
			DISCOVERED.clear(); // Forget all servers
		} else if (thread == null && !LISTENERS.isEmpty()) {
			thread = new DiscoveryThread();
			thread.start();
		}
		LOG.debug("Set discovery mode to " + mode + (mode == DiscoveryMode.DISABLED ? ", forgot all servers" : ""));
	}

	/**
	 * Adds a {@link DiscoveryListener} to the discovery system. Listeners are
	 * used to listen for events that occur relating to the discovery system
	 * such as discovering servers, forgetting servers, etc.
	 * 
	 * @param listener
	 *            the listener to add.
	 * @throws NullPointerException
	 *             if the <code>listener</code> is <code>null</code>.
	 */
	public static synchronized void addListener(DiscoveryListener listener) throws NullPointerException {
		if (listener == null) {
			throw new NullPointerException("Listener cannot be null");
		} else if (LISTENERS.contains(listener)) {
			return; // Prevent duplicates
		}
		LISTENERS.add(listener);
		LOG.debug("Added listener of class " + listener.getClass().getName());
		if (thread == null) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Removes a {@link DiscoveryListener} from the discovery system.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	public static void removeListener(DiscoveryListener listener) {
		if (LISTENERS.remove(listener)) {
			LOG.debug("Removed listener of class " + listener.getClass().getName());
		}
	}

	/**
	 * Calls an event.
	 * 
	 * @param event
	 *            the event to call.
	 * @throws NullPointerException
	 *             if the <code>event</code> is <code>null</code>.
	 * @see com.whirvis.jraknet.discovery.DiscoveryListener DiscoveryListener
	 */
	protected static void callEvent(Consumer<? super DiscoveryListener> event) throws NullPointerException {
		if (event == null) {
			throw new NullPointerException("Event cannot be null");
		}
		LISTENERS.forEach(listener -> Scheduler.scheduleSync(listener, event));
	}

	/**
	 * Returns the ports that are being broadcasted to on the local network.
	 * 
	 * @return the ports that are being broadcasted to on the local network.
	 */
	public static int[] getPorts() {
		return DISCOVERY_ADDRESSES.keySet().stream()
				.filter(address -> DISCOVERY_ADDRESSES.get(address).booleanValue() == LOCAL_SERVER)
				.mapToInt(InetSocketAddress::getPort).toArray();
	}

	/**
	 * Starts broadcasting to the ports on the local network for server
	 * discovery. It is also possible to discovery only certain servers on the
	 * local network via {@link #addServer(InetSocketAddress)} if desired.
	 * 
	 * @param ports
	 *            the ports to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if one of the ports is not within the range of
	 *             <code>0-65535</code>.
	 */
	public static synchronized void addPorts(int... ports) throws IllegalArgumentException {
		for (int port : ports) {
			if (port < 0x0000 || port > 0xFFFF) {
				throw new IllegalArgumentException("Invalid port range");
			}
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, port);
			if (DISCOVERY_ADDRESSES.put(discoveryAddress, LOCAL_SERVER) == null) {
				LOG.debug("Added discovery port " + port);
			}
		}
		if (thread == null && !DISCOVERY_ADDRESSES.isEmpty()) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Starts broadcasting to the port on the local network for server
	 * discovery. It is also possible to discovery only certain servers on the
	 * local network via {@link #addServer(InetSocketAddress)} if desired.
	 * 
	 * @param port
	 *            the port to start broadcasting to.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code>.
	 */
	public static void addPort(int port) throws IllegalArgumentException {
		addPorts(port);
	}

	/**
	 * Stops broadcasting to the ports on the local network.
	 * 
	 * @param ports
	 *            the ports to stop broadcasting to.
	 */
	public static void removePorts(int... ports) {
		for (int port : ports) {
			if (port < 0x0000 || port > 0xFFFF) {
				continue; // Invalid port range
			}

			/*
			 * It would makes sense to check if the address is a local address
			 * and not an external address. However, the broadcast address
			 * 255.255.255.255 could never be used for external servers, so it
			 * is not checked for here. As an extra safeguard, the
			 * addExternalServer() method will not allow for an IP address that
			 * is equivalent to that of the broadcast address 255.255.255.255.
			 */
			InetSocketAddress discoveryAddress = new InetSocketAddress(BROADCAST_ADDRESS, port);
			if (DISCOVERY_ADDRESSES.remove(discoveryAddress) != null) {
				LOG.debug("Removed discovery port " + port);
			}
		}
	}

	/**
	 * Stops broadcasting to the port on the local network.
	 * 
	 * @param port
	 *            the port to stop broadcasting to.
	 */
	public static void removePort(int port) {
		removePorts(port);
	}

	// TODO: SET PORTS FUNCTION

	/**
	 * Stops broadcasting to all ports. To stop broadcasting to a specific port,
	 * use the {@link #removePort(int)} method.
	 */
	public static void clearPorts() {
		if (!DISCOVERY_ADDRESSES.containsValue(LOCAL_SERVER)) {
			return; // No ports to clear
		}
		Iterator<InetSocketAddress> addresses = DISCOVERY_ADDRESSES.keySet().iterator();
		while (addresses.hasNext()) {
			InetSocketAddress address = addresses.next();
			boolean type = DISCOVERY_ADDRESSES.get(address).booleanValue();
			if (type == LOCAL_SERVER) {
				addresses.remove();
				DiscoveredServer forgotten = DISCOVERED.remove(address);
				if (forgotten != null) {
					callEvent(listener -> listener.onServerForgotten(forgotten));
				}
			}
		}
		LOG.debug("Cleared discovery ports");
	}

	/**
	 * Returns the servers that are being broadcasted to.
	 * 
	 * @return the servers that are being broadcasted to.
	 */
	public static InetSocketAddress[] getServers() {
		return DISCOVERY_ADDRESSES.keySet().stream()
				.filter(address -> DISCOVERY_ADDRESSES.get(address).booleanValue() == EXTERNAL_SERVER)
				.toArray(INETSOCKETADDRESS_FUNCTION);
	}

	/**
	 * Starts broadcasting to the server address for server discovery. This
	 * allows for the discovery of servers on external networks. If discovering
	 * on the local network, it is possible to discover all servers running on a
	 * specified port via the {@link #addPort(int)} method.
	 * 
	 * @param address
	 *            the server address.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the IP address of <code>address</code> is
	 *             <code>null</code> or the address is the broadcast address of
	 *             {@value #BROADCAST_ADDRESS}.
	 */
	public static synchronized void addServer(InetSocketAddress address)
			throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("Address cannot be null");
		} else if (address.getAddress() == null) {
			throw new NullPointerException("IP address cannnot be null");
		} else if (BROADCAST_ADDRESS.equals(address.getAddress().getHostAddress())) {
			throw new IllegalArgumentException("IP address cannot be broadcast address " + BROADCAST_ADDRESS);
		}
		if (DISCOVERY_ADDRESSES.put(address, EXTERNAL_SERVER) == null) {
			LOG.debug("Added external server with address " + address + " for discovery");
		}
		if (thread == null) {
			thread = new DiscoveryThread();
			thread.start();
		}
	}

	/**
	 * Starts broadcasting to the server address for server discovery. This
	 * allows for the discovery of servers on external networks. If discovering
	 * on the local network, it is possible to discover all servers running on a
	 * specified port via the {@link #addPort(int)} method.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>address</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code> or the <code>address</code> is the
	 *             broadcast address of {@value #BROADCAST_ADDRESS}.
	 */
	public static void addServer(InetAddress address, int port) throws NullPointerException, IllegalArgumentException {
		if (address == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Invalid port range");
		}
		addServer(new InetSocketAddress(address, port));
	}

	/**
	 * Starts broadcasting to the server address for server discovery. This
	 * allows for the discovery of servers on external networks. If discovering
	 * on the local network, it is possible to discover all servers running on a
	 * specified port via the {@link #addPort(int)} method.
	 * 
	 * @param host
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws NullPointerException
	 *             if the <code>host</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>port</code> is not within the range of
	 *             <code>0-65535</code> or the <code>host</code> is the
	 *             broadcast address of {@value #BROADCAST_ADDRESS}.
	 * @throws UnknownHostException
	 *             if no IP address for the <code>host</code> could be found, or
	 *             if a scope_id was specified for a global IPv6 address.
	 */
	public static void addServer(String host, int port)
			throws NullPointerException, IllegalArgumentException, UnknownHostException {
		if (host == null) {
			throw new NullPointerException("IP address cannot be null");
		} else if (port < 0x0000 || port > 0xFFFF) {
			throw new IllegalArgumentException("Invalid port range");
		}
		addServer(InetAddress.getByName(host), port);
	}

	/**
	 * Stops broadcasting to the server address.
	 * 
	 * @param address
	 *            the server address.
	 * @throws IllegalArgumentException
	 *             if the address is not that of an external server.
	 */
	public static void removeServer(InetSocketAddress address) throws IllegalArgumentException {
		if (address == null) {
			return; // No address
		} else if (address.getAddress() == null) {
			return; // No IP address
		} else if (!DISCOVERY_ADDRESSES.containsKey(address)) {
			return; // No address to remove
		} else if (DISCOVERY_ADDRESSES.get(address).booleanValue() != EXTERNAL_SERVER) {
			throw new IllegalArgumentException("Address must be that of an external server");
		}
		if (DISCOVERY_ADDRESSES.remove(address) != null) {
			DiscoveredServer forgotten = DISCOVERED.remove(address);
			if (forgotten != null) {
				callEvent(listener -> listener.onServerForgotten(forgotten));
			}
			LOG.debug("Removed external server with address " + address + " from discovery");
		}
	}

	/**
	 * Stops broadcasting to the server address.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 */
	public static void removeServer(InetAddress address, int port) {
		if (address == null) {
			return; // No address
		} else if (port < 0x0000 || port > 0xFFFF) {
			return; // Invalid port range
		}
		removeServer(new InetSocketAddress(address, port));
	}

	/**
	 * Stops broadcasting to the server address.
	 * 
	 * @param address
	 *            the server IP address.
	 * @param port
	 *            the server port.
	 * @throws UnknownHostException
	 *             if no IP address for the host could be found, or if a
	 *             scope_id was specified for a global IPv6 address.
	 */
	public static void removeServer(String address, int port) throws UnknownHostException {
		if (address == null) {
			return; // No address
		}
		removeServer(InetAddress.getByName(address), port);
	}

	/**
	 * Stops broadcasting to the discovered server.
	 * 
	 * @param server
	 *            the discovered server.
	 * @throws IllegalArgumentException
	 *             if the <code>server</code> is not an external server.
	 */
	public static void removeServer(DiscoveredServer server) throws IllegalArgumentException {
		if (!server.isExternal()) {
			throw new IllegalArgumentException("Discovered server must be an external server");
		}
		removeServer(server.getAddress());
	}

	// TODO: SET SERVERS FUNCTION

	/**
	 * Stops broadcasting to all server addresses. To stop broadcasting to a
	 * specific server address, use the {@link #removeServer(InetSocketAddress)}
	 * method.
	 */
	public static void clearServers() {
		if (!DISCOVERY_ADDRESSES.containsValue(EXTERNAL_SERVER)) {
			return; // No external servers to clear
		}
		Iterator<InetSocketAddress> addresses = DISCOVERY_ADDRESSES.keySet().iterator();
		while (addresses.hasNext()) {
			InetSocketAddress address = addresses.next();
			boolean type = DISCOVERY_ADDRESSES.get(address).booleanValue();
			if (type == EXTERNAL_SERVER) {
				addresses.remove();
				DiscoveredServer forgotten = DISCOVERED.remove(address);
				if (forgotten != null) {
					callEvent(listener -> listener.onServerForgotten(forgotten));
				}
			}
		}
		LOG.debug("Cleared external servers from discovery");
	}

	/**
	 * Returns the discovered servers, both local and external.
	 * 
	 * @return the discovered servers, both local and external.
	 * @see #getLocal()
	 * @see #getExternal()
	 */
	public static DiscoveredServer[] getDiscovered() {
		return DISCOVERED.values().toArray(new DiscoveredServer[DISCOVERED.size()]);
	}

	/**
	 * Returns the locally discovered servers.
	 * 
	 * @return the locally discovered servers.
	 */
	public static DiscoveredServer[] getLocal() {
		// TODO: Do not use streams?
		return DISCOVERED.values().stream().filter(server -> !server.isExternal()).toArray(DISCOVERED_SERVER_FUNCTION);
	}

	/**
	 * Returns the externally discovered servers.
	 * 
	 * @return the externally discovered servers.
	 */
	public static DiscoveredServer[] getExternal() {
		// TODO: Do not use streams?
		return DISCOVERED.values().stream().filter(server -> server.isExternal()).toArray(DISCOVERED_SERVER_FUNCTION);
	}

	/**
	 * Updates discovery information for the server with the specified address.
	 * 
	 * @param sender
	 *            the server address.
	 * @param pong
	 *            the decoded <code>UnconnectedPong</code> packet.
	 * @throws NullPointerException
	 *             if <code>sender</code> is <code>null</code> or the IP address
	 *             of <code>sender</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the <code>pong</code> packet failed to decode.
	 */
	protected static synchronized void updateDiscoveryData(InetSocketAddress sender, UnconnectedPong pong)
			throws NullPointerException, IllegalArgumentException {
		if (sender == null) {
			throw new NullPointerException("Sender cannot be null");
		} else if (sender.getAddress() == null) {
			throw new NullPointerException("Sender IP address cannot be null");
		} else if (pong.failed()) {
			throw new IllegalArgumentException("Unconnected pong failed to decode");
		} else if (!RakNet.isLocalAddress(sender) && !DISCOVERY_ADDRESSES.containsKey(sender)) {
			return; // Not a local server or a registered external server
		}
		boolean external = !RakNet.isLocalAddress(sender);
		if (DISCOVERY_ADDRESSES.containsKey(sender)) {
			external = DISCOVERY_ADDRESSES.get(sender).booleanValue();
		}

		// Update server information
		if (!DISCOVERED.containsKey(sender)) {
			DiscoveredServer discovered = new DiscoveredServer(sender, external, pong.identifier);
			DISCOVERED.put(sender, discovered);
			LOG.info("Discovered " + (external ? "external" : "local") + " with address " + sender);
			callEvent(listener -> listener.onServerDiscovered(discovered));
		} else {
			DiscoveredServer discovered = DISCOVERED.get(sender);
			discovered.setTimestamp(System.currentTimeMillis());
			if (!pong.identifier.equals(discovered.getIdentifier())) {
				Identifier oldIdentifier = discovered.getIdentifier();
				discovered.setIdentifier(pong.identifier);
				LOG.debug("Updated local server with address " + sender + " identifier to \"" + pong.identifier + "\"");
				callEvent(listener -> listener.onServerIdentifierUpdate(discovered, oldIdentifier));
			}
		}
	}

}
