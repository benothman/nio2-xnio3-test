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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import org.jboss.logging.Logger;
import org.jboss.server.common.ClientManager;

/**
 * {@code ClientManager}
 * 
 * Created on Oct 27, 2011 at 6:07:24 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class Nio2ClientManager extends ClientManager<AsynchronousSocketChannel> {

	private static final Logger logger = Logger.getLogger(Nio2ClientManager.class);

	/**
	 * Create a new instance of {@code ClientManager}
	 * 
	 * @param channel
	 */
	public Nio2ClientManager(AsynchronousSocketChannel channel) {
		super(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (!this.intialized) {
			this.init();
		}
		try {
			// Initialization of the communication
			byte bytes[] = new byte[this.readBuffer.capacity()];
			do {
				this.readBuffer.clear();
				int n = channel.read(this.readBuffer).get();
				if (n < 0) {
					break;
				}

				if (n > 0) {
					this.readBuffer.flip();
					this.readBuffer.get(bytes, 0, n);
					// write response to client
					writeResponse(channel);
				}
			} while (channel.isOpen());
		} catch (Exception exp) {
			logger.errorv("ERROR: from client side -> {0}", exp);
			exp.printStackTrace();
		} finally {
			this.close();
		}
		// logger.info("Client Manager shutdown");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.server.common.ClientManager#write(java.nio.channels.Channel,
	 * java.nio.ByteBuffer[], long)
	 */
	@Override
	public void write(final AsynchronousSocketChannel channel, final ByteBuffer[] buffers,
			final long total) throws Exception {

		for (ByteBuffer buffer : buffers) {
			write(channel, buffer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.server.common.ClientManager#write(java.nio.channels.Channel,
	 * java.nio.ByteBuffer)
	 */
	@Override
	protected void write(AsynchronousSocketChannel channel, ByteBuffer buffer) throws Exception {
		if (buffer.position() > 0) {
			buffer.flip();
		}

		while (buffer.hasRemaining()) {
			if (channel.write(buffer).get() < 0) {
				throw new IOException();
			}
		}
	}

}
