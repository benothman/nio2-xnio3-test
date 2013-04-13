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
package org.jboss.server.nio2.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import org.jboss.logging.Logger;
import org.jboss.server.common.Constants;

/**
 * {@code WriteCompletionHandler}
 * 
 * Created on Nov 17, 2011 at 9:33:12 AM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
class WriteCompletionHandler implements CompletionHandler<Long, Object[]> {

	private static final Logger logger = Logger.getLogger(CompletionHandler.class);
	private int offset = 0;
	private long written = 0;

	/**
	 * Create a new instance of {@code WriteCompletionHandler}
	 * 
	 * @param channel
	 * @param sessionId
	 */
	public WriteCompletionHandler() {
		super();
	}

	/**
	 * 
	 */
	protected void reset() {
		this.offset = 0;
		this.written = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object,
	 * java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void completed(Long nBytes, Object[] attachment) {
		if (nBytes < 0) {
			failed(new ClosedChannelException(), attachment);
		} else {
			this.written += nBytes;
			final long total = (Long) attachment[Constants.FILE_LENGTH_POS];
			final AsynchronousSocketChannel channel = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];

			if (this.written < total) {
				// Write the rest of bytes
				ByteBuffer buffers[] = (ByteBuffer[]) attachment[Constants.WRITE_BUFFERS_POS];
				offset = (int) (written / buffers[0].capacity());
				channel.write(buffers, offset, buffers.length - offset, Constants.DEFAULT_TIMEOUT,
						Constants.DEFAULT_TIME_UNIT, attachment, this);
			} else {
				this.reset();
				ByteBuffer buff = (ByteBuffer) attachment[Constants.READ_BUFFER_POS];
				CompletionHandler<Integer, Object[]> readHandler = (CompletionHandler<Integer, Object[]>) attachment[Constants.READ_HANDLER_POS];

				// Read again from client
				channel.read(buff, Constants.DEFAULT_TIMEOUT, Constants.DEFAULT_TIME_UNIT,
						attachment, readHandler);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable,
	 * java.lang.Object)
	 */
	@Override
	public void failed(Throwable exc, Object[] attachment) {
		try {
			String sessionId = (String) attachment[Constants.SESSION_ID_POS];
			System.out.println("[" + sessionId + "] Closing remote connection");
			AsynchronousSocketChannel channel = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];
			channel.close();
		} catch (IOException e) {
			// NOPE
		}
	}
}
