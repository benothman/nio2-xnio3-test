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
package org.jboss.server.nio2.sync;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import org.jboss.server.common.Constants;
import org.jboss.server.nio2.NioServer;

/**
 * {@code SyncServer}
 * 
 * Created on Nov 10, 2011 at 3:41:02 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class SyncServer extends NioServer {

	/**
	 * Create a new instance of {@code SyncServer}
	 */
	public SyncServer(int port) {
		super(port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.server.nio2.common.NioServer#processChannel(java.nio.channels
	 * .AsynchronousSocketChannel)
	 */
	@Override
	public void processChannel(AsynchronousSocketChannel channel) throws Exception {
		// Generate a new session id
		String sessionId = generateSessionId();
		final ByteBuffer readBuffer = ByteBuffer.allocate(512);
		// Initialize the session
		initSession(channel, readBuffer, sessionId);
		// Fix the channel send buffer size
		channel.setOption(StandardSocketOptions.SO_SNDBUF, Constants.DEFAULT_SO_SNDBUF);
		// Create a new client manager
		Nio2ClientManager manager = new Nio2ClientManager(channel);
		manager.setSessionId(sessionId);
		// Execute the client manager
		executor.execute(manager);
	}
}
