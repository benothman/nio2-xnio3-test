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
package org.jboss.server.nio2;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

import org.jboss.logging.Logger;
import org.jboss.server.common.AbstractServer;
import org.jboss.server.common.Constants;

/**
 * {@code NioServer}
 * 
 * Created on Oct 27, 2012 at 5:37:27 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public abstract class NioServer extends AbstractServer {

	/**
	 *
	 */
	protected static final Logger logger = Logger.getLogger(NioServer.class.getName());

	/**
	 * Create a new instance of {@code NioServer}
	 */
	public NioServer(int port) {
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
		logger.infov("Starting {0} NIO.2 Sever on port {1} ...", mode, port);

		try {
			// Create an asynchronous channel group
			AsynchronousChannelGroup threadGroup = AsynchronousChannelGroup
					.withThreadPool(executor);
			try (AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(
					threadGroup).bind(new InetSocketAddress(port))) {
				boolean running = true;
				logger.infov("{0} NIO.2 Sever started ...", mode);

				while (running) {
					Future<AsynchronousSocketChannel> future = listener.accept();
					final AsynchronousSocketChannel channel = future.get();
					// Process the channel
					processChannel(channel);
				}
			}
		} catch (Throwable th) {
			th.printStackTrace();
		}

	}

	/**
	 * Process the newly accepted channel
	 * 
	 * @param channel
	 */
	public abstract void processChannel(final AsynchronousSocketChannel channel) throws Exception;

	/**
	 * 
	 * @param channel
	 * @param buffer
	 * @param sessionId
	 * @throws Exception
	 */
	protected static void initSession(AsynchronousSocketChannel channel, ByteBuffer buffer,
			String sessionId) throws Exception {
		buffer.clear();
		Future<Integer> future = channel.read(buffer);
		int nBytes = future.get();
		buffer.flip();
		byte bytes[] = new byte[nBytes];
		buffer.get(bytes).clear();
		System.out.println("[" + sessionId + "] " + new String(bytes).trim());
		String response = "jSessionId: " + sessionId + Constants.CRLF;
		// write initialization response to client
		buffer.put(response.getBytes()).flip();
		System.out.println("buffer.position(): " + buffer.position());
		System.out.println("buffer.limit(): " + buffer.limit());
		int x = channel.write(buffer).get();
		System.out.println("Number of bytes written -> " + x);
	}

}
