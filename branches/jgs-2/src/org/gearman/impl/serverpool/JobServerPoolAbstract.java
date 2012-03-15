package org.gearman.impl.serverpool;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.GearmanServer;
import org.gearman.GearmanServerPool;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.core.GearmanPacket;

/**
 * A nasty class used to manage multa
 * 
 * @author isaiah
 */
abstract class JobServerPoolAbstract <X extends ConnectionController<?,?>> implements GearmanServerPool {
	static final String DEFAULT_CLIENT_ID = "-";
	
	private final GearmanImpl gearman;
	
	final ConcurrentHashMap<Object, X> connMap = new ConcurrentHashMap<Object,X>();
	private final GearmanLostConnectionPolicy defaultPolicy;
	private GearmanLostConnectionPolicy policy;;
	private long waitPeriod;
	private boolean isShutdown = false;
	private String id = JobServerPoolAbstract.DEFAULT_CLIENT_ID;
	
	JobServerPoolAbstract(GearmanImpl gearman, GearmanLostConnectionPolicy defaultPolicy, long waitPeriod, TimeUnit unit) {
		this.defaultPolicy = defaultPolicy;
		this.policy = defaultPolicy;
		this.waitPeriod = unit.toNanos(waitPeriod);
		this.gearman = gearman;
	}
	
	@Override
	public boolean addServer(GearmanServer srvr) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		final X x = this.createController(srvr);
		if(this.connMap.putIfAbsent(srvr, x)==null) {
			x.onNew();
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public GearmanImpl getGearman() {
		return this.gearman;
	}

	@Override
	public String getClientID() {
		return this.id;
	}

	@Override
	public long getReconnectPeriod(TimeUnit unit) {
		return unit.convert(this.waitPeriod,TimeUnit.NANOSECONDS);
	}

	@Override
	public int getServerCount() {
		return this.connMap.size();
	}

	@Override
	public boolean hasServer(GearmanServer srvr) {
		return this.connMap.containsKey(srvr);
	}

	@Override
	public void removeAllServers() {
		Iterator<X> it = this.connMap.values().iterator();
		X value;
		
		while(it.hasNext()) {
			value = it.next();
			it.remove();
			
			if(value!=null) {
				value.dropServer();
			}
		}
	}

	@Override
	public boolean removeServer(GearmanServer srvr) {
		final X x = this.connMap.get(srvr);
		
		if(x!=null) {
			x.dropServer();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setClientID(String id) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		if(this.id.equals(id)) return;
		
		for(X x : this.connMap.values()) {
			x.sendPacket(GearmanPacket.createSET_CLIENT_ID(id), null);
		}
	}

	@Override
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		if(this.policy==null)
			this.policy = this.defaultPolicy;
		else
			this.policy = policy;
	}

	@Override
	public void setReconnectPeriod(long time, TimeUnit unit) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		this.waitPeriod = unit.toNanos(time);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public synchronized void shutdown() {
		if(this.isShutdown) return;
		this.isShutdown = true;
		
		this.removeAllServers();
	}
	
	protected Map<Object,X> getConnections() {
		return Collections.unmodifiableMap(this.connMap);
	}
	
	protected GearmanLostConnectionPolicy getDefaultPolicy() {
		return this.defaultPolicy;
	}
	
	protected GearmanLostConnectionPolicy getPolicy() {
		return this.policy;
	}
	
	/**
	 * Creates a new ConnectionControler to add to the JobServerPool<br>
	 * Note: The returned value is not guaranteed to be added to the set
	 * of connections.  
	 * @param key
	 * 		The ConnectionControler's key
	 * @return
	 * 		
	 */
	protected abstract X createController(GearmanServer key);
}