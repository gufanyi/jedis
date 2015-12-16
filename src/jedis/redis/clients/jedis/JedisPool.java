package redis.clients.jedis;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.Pool;

import com.orangefunction.tomcat.redissessions.Serializer;

public class JedisPool extends Pool<Jedis> {

	private Serializer serializer = null;
	private static JedisPool connectionPool = null;

	public static JedisPool getInstance() {
		if (connectionPool == null) {
			synchronized (JedisPool.class) {
				if (connectionPool == null) {
					synchronized (JedisPool.class) {
						connectionPool = new JedisPool("localhost");
						try {
							connectionPool.serializer = ((Serializer) Class.forName("com.orangefunction.tomcat.redissessions.JavaSerializer").newInstance());
						} catch (InstantiationException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}

			}
		}
		return connectionPool;

	}

	public Serializer getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public Jedis acquireConnection() {
		Jedis jedis = (Jedis) JedisPool.getInstance().getResource();
		jedis.select(0);
		return jedis;
	}

	public void returnConnection(Jedis jedis, Boolean error) {
		if (error.booleanValue())
			JedisPool.getInstance().returnBrokenResource(jedis);
		else
			JedisPool.getInstance().returnResource(jedis);
	}

	public void returnConnection(Jedis jedis) {
		returnConnection(jedis, Boolean.valueOf(false));
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host) {
		this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(String host, int port) {
		this(new GenericObjectPoolConfig(), host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(final String host) {
		URI uri = URI.create(host);
		if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
			String h = uri.getHost();
			int port = uri.getPort();
			String password = uri.getUserInfo().split(":", 2)[1];
			int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);
			this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(h, port, Protocol.DEFAULT_TIMEOUT, password, database, null), new GenericObjectPoolConfig());
		} else {
			this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null),
					new GenericObjectPoolConfig());
		}
	}

	public JedisPool(final URI uri) {
		String h = uri.getHost();
		int port = uri.getPort();
		String password = uri.getUserInfo().split(":", 2)[1];
		int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);
		this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(h, port, Protocol.DEFAULT_TIMEOUT, password, database, null), new GenericObjectPoolConfig());
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port, int timeout, final String password) {
		this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port) {
		this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, final int port, final int timeout) {
		this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE, null);
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port, int timeout, final String password, final int database) {
		this(poolConfig, host, port, timeout, password, database, null);
	}

	public JedisPool(final GenericObjectPoolConfig poolConfig, final String host, int port, int timeout, final String password, final int database, final String clientName) {
		super(poolConfig, new JedisFactory(host, port, timeout, password, database, clientName));
	}

	public Jedis getResource() {
		Jedis jedis = super.getResource();
		jedis.setDataSource(this);
		return jedis;
	}

	public void returnBrokenResource(final Jedis resource) {
		if (resource != null) {
			returnBrokenResourceObject(resource);
		}
	}

	public void returnResource(final Jedis resource) {
		if (resource != null) {
			resource.resetState();
			returnResourceObject(resource);
		}
	}
}
