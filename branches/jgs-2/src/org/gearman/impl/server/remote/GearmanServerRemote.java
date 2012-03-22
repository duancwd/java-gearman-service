/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.server.remote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.gearman.impl.GearmanImpl;
import org.gearman.impl.GearmanConstants;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.server.ServerShutdownListener;

/**
 * A object representing a remote gearman server
 * @author isaiah
 */
public class GearmanServerRemote implements GearmanServerInterface {

	/** The id for this remote server */
	private final String id;
	
	/** The address to the remote server */
	private final InetSocketAddress adrs;
	
	/** The gearman service that created this GearmanServerRemote object*/
	private final GearmanImpl gearman;
	
	/** The internal lock used to synchronize events */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	/** The connections created by this GearmanServerRemote */
	private final Set<GearmanConnection<?>> connections = new HashSet<>();
	
	/** A variable indicating if this GearmanServerRemote is shutdown */
	private boolean isShutdown = false;
	
	private final Set<ServerShutdownListener> listeners = new HashSet<>();
	
	/**
	 * Constructor
	 * @param gearman
	 * 		The gearman instance that is creating this service
	 * @param adrs
	 * 		The address of the remote gearman server
	 */
	public GearmanServerRemote(GearmanImpl gearman, InetSocketAddress adrs) {
		this(gearman, "remote: "+adrs.toString(), adrs);
	}
	
	public GearmanServerRemote(GearmanImpl gearman, String serverID, InetSocketAddress adrs) {
		this.gearman = gearman;
		this.adrs = adrs;
		
		this.id = serverID;
	}
	
	@Override
	public boolean isLocalServer() {
		return false;
	}

	@Override
	public String getHostName() {
		return this.adrs.getHostName();
	}

	@Override
	public void shutdown() {
		try {
			this.lock.writeLock().lock();
			this.isShutdown = true;
		} finally {
			this.lock.writeLock().unlock();
		}
		
		for(GearmanConnection<?> conn : this.connections) {
			try {
				conn.close();
			} catch (IOException e) {
				GearmanConstants.LOGGER.log(Level.SEVERE, "failed to close gearman connection", e);
			}
		}
		
		for(ServerShutdownListener l : listeners) {
			l.onShutdown(this);
		}
		
		this.getGearman().onServiceShutdown(this);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public GearmanImpl getGearman() {
		return this.gearman;
	}

	@Override
	public String getServerID() {
		return this.id;
	}
	
	@Override
	public String toString() {
		return this.id;
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof GearmanServerRemote))
			return false;
		return this.toString().equals(o.toString());
	}

	@Override
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback) {
		boolean isShutdown;
		try {
			lock.readLock().lock();
			if(!(isShutdown=this.isShutdown())) {
				final InnerGearmanConnectionHandler<A> innerHandler = new InnerGearmanConnectionHandler<>(handler, failCallback);
				gearman.getGearmanConnectionManager().createGearmanConnection(adrs, innerHandler, innerHandler);
			}
		} finally {
			lock.readLock().unlock();
		}
		
		if(isShutdown) failCallback.onComplete(this, ConnectCallbackResult.SERVICE_SHUTDOWN);
	}
	
	@Override
	public void finalize() throws Throwable {
		this.shutdown();
	}
	
	/**
	 * A GearmanConnectionHandler wrapper class that adds and removes connections to the connection set 
	 * @author isaiah
	 */
	private final class InnerGearmanConnectionHandler<A> implements GearmanConnectionHandler<A>, GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> {
		
		private final GearmanConnectionHandler<A> handler;
		private final GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback;
		
		private InnerGearmanConnectionHandler(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback) {
			this.handler = handler;
			this.failCallback = failCallback;
		}

		@Override
		public void onAccept(GearmanConnection<A> conn) {
			try {
				lock.readLock().lock();
				if(isShutdown()) {
					try {
						conn.close();
					} catch (IOException e) {
						failCallback.onComplete(GearmanServerRemote.this, ConnectCallbackResult.SERVICE_SHUTDOWN);
						GearmanConstants.LOGGER.log(Level.SEVERE, "failed to close connection", e);
					}
					return;
				}
			} finally {
				lock.readLock().unlock();
			}
			
			handler.onAccept(conn);
		}

		@Override
		public void onPacketReceived(GearmanPacket packet, GearmanConnection<A> conn) {
			handler.onPacketReceived(packet, conn);
		}
		
		@Override
		public void onDisconnect(GearmanConnection<A> conn) {
			connections.remove(conn);
			handler.onDisconnect(conn);
		}

		@Override
		public void onComplete(InetSocketAddress data, ConnectCallbackResult result) {
			this.failCallback.onComplete(GearmanServerRemote.this, result);
		}
	}

	@Override
	public Collection<Integer> getPorts() {
		return Collections.singleton(adrs.getPort());
	}

	@Override
	public void addShutdownListener(ServerShutdownListener listener) {
		synchronized(this.listeners) {
			this.listeners.add(listener);
		}
	}

	@Override
	public void removeShutdownListener(ServerShutdownListener listener) {
		synchronized(this.listeners) {
			this.listeners.remove(listener);
		}
	}
	
}
