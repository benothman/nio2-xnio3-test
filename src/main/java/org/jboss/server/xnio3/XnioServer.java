/**
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and
 * individual
 * contributors as indicated by the @author tags. See the copyright.txt file in
 * the distribution
 * for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.server.xnio3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.server.common.AbstractServer;
import org.jboss.server.xnio3.common.XnioUtils;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;
import org.xnio.channels.StreamChannel;

/**
 * {@code Server}
 * 
 * Created on Oct 27, 2012 at 5:18:47 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public abstract class XnioServer extends AbstractServer {

	protected static final Logger logger = Logger.getLogger(XnioServer.class.getName());
	protected AtomicInteger counter = new AtomicInteger();

	/**
	 * Create a new instance of {@code Xnio3Server}
	 * 
	 * @param port
	 */
	public XnioServer(int port) {
		super(port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String mode = this.async ? "Asynchronous" : "Synchronous";
		logger.infov("Starting {0} XNIO3 Server on port {1} ...", mode, this.port);
		try {
			// Get the Xnio instance
			final Xnio xnio = Xnio.getInstance("nio", getClass().getClassLoader());
			int cores = Runtime.getRuntime().availableProcessors();

			// Create the OptionMap for the worker
			OptionMap optionMap = OptionMap.create(Options.WORKER_WRITE_THREADS, cores,
					Options.WORKER_READ_THREADS, cores);
			// Create the worker
			final XnioWorker worker = xnio.createWorker(null,optionMap);
			final SocketAddress address = new InetSocketAddress(this.port);
			final ChannelListener<? super AcceptingChannel<ConnectedStreamChannel>> acceptListener = ChannelListeners
					.openListenerAdapter(getAcceptListener());
			// configure the number of worker task max threads
			worker.setOption(Options.WORKER_TASK_MAX_THREADS, 1000);

			final AcceptingChannel<? extends ConnectedStreamChannel> server = worker
					.createStreamServer(address, acceptListener,
							OptionMap.create(Options.REUSE_ADDRESSES, Boolean.TRUE));
			server.resumeAccepts();
			logger.infov("{0} XNIO3 Sever started ...", mode);
		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

	/**
	 * @return the channel listener
	 */
	public abstract ChannelListener<Channel> getAcceptListener();

	/**
	 * 
	 * @param channel
	 * @param sessionId
	 * @throws IOException
	 */
	public void initSession(StreamChannel channel, String sessionId) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.clear();
		int nBytes = channel.read(buffer);
		buffer.flip();
		byte bytes[] = new byte[nBytes];
		buffer.get(bytes);
		String response = "jSessionId: " + sessionId + XnioUtils.CRLF;
		// write initialization response to client
		buffer.clear();
		buffer.put(response.getBytes());
		buffer.flip();
		channel.awaitWritable();
		channel.write(buffer);
	}
}
