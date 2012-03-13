package org.gearman.impl.server.local;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import org.gearman.Gearman;
import org.gearman.GearmanPersistence;
import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.GearmanConstants;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.util.GearmanUtils;

public class GearmanServerLocal implements GearmanServerInterface, GearmanConnectionHandler<Client> {

	private final String id;
	
	private final GearmanImpl gearman;
	private final Interpreter interpreter;
	
	private final Set<Client> clients = Collections.synchronizedSet(new HashSet<Client>());
	private final Set<Integer> openPorts;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private final String hostName;
	
	private boolean isShutdown = false;
	
	public GearmanServerLocal(GearmanImpl gearman, GearmanPersistence persistence, int...ports) throws IOException {
		this(gearman, persistence, createID(ports), ports);
	}
	
	public GearmanServerLocal(GearmanImpl gearman, GearmanPersistence persistence, String serverID, int...ports) throws IOException {
		this.gearman = gearman;
		
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			host = "localhost";
		}
		
		this.hostName = host;
		this.interpreter = new Interpreter(this, persistence);
		
		final Set<Integer> portSet = new HashSet<>(ports.length);
		
		for(int port : ports) {
			try {
				gearman.getGearmanConnectionManager().openPort(port, this);
				portSet.add(port);
			} catch (IOException ioe) {
				GearmanConstants.LOGGER.log(Level.SEVERE, "failed to open port: " + port, ioe);
			}
		}
		
		this.openPorts = Collections.unmodifiableSet(portSet);
		this.id = serverID;
	}
	
	private static final String createID(int[] openPorts) {
		final StringBuilder sb = new StringBuilder("local");
		
		for(Integer port : openPorts) {
			sb.append(':');
			sb.append(port);
		}
		
		return sb.toString();
	}
	
	Set<Client> getClientSet() {
		return this.clients;
	}
	
	@Override
	public boolean isLocalServer() {
		return true;
	}

	@Override
	public String getHostName() {
		return hostName;
	}

	@Override
	public void shutdown() {
		try {
			this.lock.writeLock().lock();
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public Gearman getGearman() {
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
		if(!(o instanceof GearmanServerLocal))
			return false;
		return this.toString().equals(o.toString());
	}

	@Override
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback) {
		try {
			this.lock.readLock().lock();
			
			if(this.isShutdown()) {
				failCallback.onComplete(this, ConnectCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			new LocalConnection<Client,A>(this,handler);
			
		} finally {
			this.lock.readLock().unlock();
		}
	}
	
	private static final class LocalConnection<X,Y> implements GearmanConnection<X> {
		private final LocalConnection<Y,X> peer;
		private final GearmanConnectionHandler<X> handler;
		private X att;
		
		private boolean isClosed = false;
		
		public LocalConnection(GearmanConnectionHandler<X> handler, GearmanConnectionHandler<Y> peerHandler) {
			/*
			 * This method can only be called by the ServerImpl class. This is because the onAccept method
			 * may cause problems if implemented by some other layer
			 */
			assert handler != null;
			assert peerHandler != null;
			assert handler instanceof GearmanServerLocal;
			
			
			this.handler = handler;
			
			this.peer = new LocalConnection<Y,X>(peerHandler, this);
			
			this.handler.onAccept(this);
			this.peer.handler.onAccept(peer);
		}
		
		private LocalConnection(GearmanConnectionHandler<X> handler, LocalConnection<Y,X> peer) {
			assert handler != null;
			assert peer != null;
			
			this.handler = handler;
			this.peer = peer;
		}
		
		@Override
		public final void close() throws IOException {
			synchronized(this) {
				if(this.isClosed) return;
				this.isClosed = true;
			}
			
			this.peer.close();
			this.handler.onDisconnect(this);
		}

		@Override
		public final String getHostAddress() {
			return "localhost";
		}

		@Override
		public final int getLocalPort() {
			return -1;
		}

		@Override
		public final int getPort() {
			return -1;
		}

		
		@Override
		public final X getAttachment() {
			return att;
		}

		@Override
		public final void setAttachment(final X att) {
			this.att = att;
		}
		
		@Override
		protected final void finalize() throws Throwable{
			this.close();
		}

		@Override
		public boolean isClosed() {
			return this.isClosed;
		}

		@Override
		public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, org.gearman.core.GearmanConnection.SendCallbackResult> callback) {
			if(this.isClosed) {
				if(callback!=null)
					callback.onComplete(packet, SendCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			this.peer.handler.onPacketReceived(packet, peer);
			if(callback!=null) callback.onComplete(packet, SendCallbackResult.SEND_SUCCESSFUL);
		}
	}

	@Override
	public Collection<Integer> getPorts() {
		return this.openPorts;
	}

	@Override
	public void onAccept(GearmanConnection<Client> conn) {
		try {
			this.lock.readLock().lock();
			if(this.isShutdown()) {
				conn.close();
				return;
			}
			
			GearmanConstants.LOGGER.log(Level.INFO, GearmanUtils.toString(conn) + " : Connected");
			
			final Client client = new ClientImpl(conn);
			conn.setAttachment(client);
				
			this.clients.add(client);
		} catch (IOException e) {
			GearmanConstants.LOGGER.log(Level.WARNING, "failed to close connection", e);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Client> conn) {
		GearmanConstants.LOGGER.log(Level.INFO, GearmanUtils.toString(conn) + " : IN : " + packet.getPacketType().toString());
		
		assert packet!=null;
		assert conn.getAttachment()!=null;
		
		try {
			this.interpreter.execute(packet, conn.getAttachment());
		} catch (Exception e) {
			GearmanConstants.LOGGER.log(Level.SEVERE, "failed to execute packet: "+packet.getPacketType().toString(),e);
		}
	}

	@Override
	public void onDisconnect(GearmanConnection<Client> conn) {
		Client client = conn.getAttachment();
		conn.setAttachment(null);
		if(client!=null) {
			client.close();
			this.clients.remove(conn.getAttachment());
		}
	}
}