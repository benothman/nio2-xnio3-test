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

import org.jboss.server.common.Constants;

/**
 * {@code ReadCompletionHandler}
 * 
 * Created on Nov 16, 2011 at 8:59:51 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
class ReadCompletionHandler implements CompletionHandler<Integer, Object[]> {

	/**
	 * Create a new instance of {@code ReadCompletionHandler}
	 * 
	 * @param sessionId
	 * @param byteBuffer
	 */
	public ReadCompletionHandler() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void completed(Integer nBytes, Object[] attachment) {
		if (nBytes < 0) {
			failed(new ClosedChannelException(), attachment);
			return;
		}

		if (nBytes > 0) {
			ByteBuffer buff = (ByteBuffer) attachment[Constants.READ_BUFFER_POS];
			buff.flip();
			byte bytes[] = new byte[nBytes];
			buff.get(bytes).clear();
			//System.out.println("ASYNC-NIO.2 --> [" + attachment[Constants.SESSION_ID_POS] + "] "
			//		+ new String(bytes).trim());
			// write response to client
			writeResponse(attachment);
		} else {
			AsynchronousSocketChannel channel = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];
			ByteBuffer bb = (ByteBuffer) attachment[Constants.READ_BUFFER_POS];
			bb.clear();
			// Read again from client
			channel.read(bb, attachment, this);
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

	/**
	 * @param attachment
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void writeResponse(Object[] attachment) {

		// Retrieve the channel
		AsynchronousSocketChannel channel = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];
		// Retrieve the completion handler
		CompletionHandler<Long, Object[]> writeHandler = (CompletionHandler<Long, Object[]>) attachment[Constants.WRITE_HANDLER_POS];
		// Retrieve the buffers to write
		ByteBuffer buffers[] = (ByteBuffer[]) attachment[Constants.WRITE_BUFFERS_POS];
		// Flip all buffers
		flipAll(buffers);
		// Write response to client
		channel.write(buffers, 0, buffers.length, Constants.DEFAULT_TIMEOUT,
				Constants.DEFAULT_TIME_UNIT, attachment, writeHandler);

	}

	/**
	 * Flip all the write byte buffers
	 * 
	 * @param buffers
	 */
	protected static void flipAll(ByteBuffer[] buffers) {
		for (ByteBuffer bb : buffers) {
			if (bb.position() > 0) {
				bb.flip();
			}
		}
	}

}
