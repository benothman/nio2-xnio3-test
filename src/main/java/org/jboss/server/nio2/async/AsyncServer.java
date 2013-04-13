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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import org.jboss.server.common.Constants;
import org.jboss.server.common.FileLoader;
import org.jboss.server.nio2.NioServer;

/**
 * {@code NioAsyncServer}
 * 
 * Created on Oct 27, 2011 at 5:47:30 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class AsyncServer extends NioServer {

	/**
	 * Create a new instance of {@code AsyncServer}
	 */
	public AsyncServer(int port) {
		super(port);
		this.async = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.server.nio2.common.NioServer#processChannel(java.nio.channels
	 * .AsynchronousSocketChannel)
	 */
	public void processChannel(final AsynchronousSocketChannel channel) throws Exception {

		channel.setOption(StandardSocketOptions.SO_SNDBUF, Constants.DEFAULT_SO_SNDBUF);
		final String sessionId = generateSessionId();
		final ByteBuffer buffer = ByteBuffer.allocateDirect(512);
		final ByteBuffer writeBuffers[] = FileLoader.cloneData();
		final long fileLength = FileLoader.getFileLength();
		final CompletionHandler<Integer, Object[]> readHandler = new ReadCompletionHandler();
		final CompletionHandler<Long, Object[]> writeHandler = new WriteCompletionHandler();

		// Put params in the Map

		final Object array[] = new Object[10];
		array[Constants.CHANNEL_POS] = channel;
		array[Constants.READ_BUFFER_POS] = buffer;
		array[Constants.WRITE_BUFFERS_POS] = writeBuffers;
		array[Constants.FILE_LENGTH_POS] = fileLength;
		array[Constants.SESSION_ID_POS] = sessionId;
		array[Constants.READ_HANDLER_POS] = readHandler;
		array[Constants.WRITE_HANDLER_POS] = writeHandler;

		// Perform an asynchronous read operation
		channel.read(buffer, array, new CompletionHandler<Integer, Object[]>() {

			@Override
			public void completed(Integer nBytes, Object[] attachment) {
				if (nBytes < 0) {
					failed(new ClosedChannelException(), attachment);
					return;
				}
				if (nBytes > 0) {
					ByteBuffer buff = (ByteBuffer) array[Constants.READ_BUFFER_POS];
					buff.flip();
					byte bytes[] = new byte[nBytes];
					buff.get(bytes).clear();
					String response = "jSessionId: " + attachment[Constants.SESSION_ID_POS]
							+ Constants.CRLF;
					// write initialization response to client
					buff.put(response.getBytes()).flip();
					AsynchronousSocketChannel ch = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];
					ch.write(buff, attachment, new CompletionHandler<Integer, Object[]>() {

						@Override
						public void completed(Integer nBytes, Object[] attachment) {
							//System.out.println("Number of bytes written to client -> " + nBytes);
							if (nBytes < 0) {
								failed(new ClosedChannelException(), attachment);
							} else if (nBytes > 0) {
								AsynchronousSocketChannel channel = (AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS];
								ByteBuffer bb = (ByteBuffer) attachment[Constants.READ_BUFFER_POS];
								if (bb.hasRemaining()) {
									channel.write(bb, attachment, this);
								} else {
									@SuppressWarnings("unchecked")
									CompletionHandler<Integer, Object[]> readHandler = (CompletionHandler<Integer, Object[]>) attachment[Constants.READ_HANDLER_POS];
									//System.out.println("End of Session Initialization, Waiting for client requests");
									channel.read(bb, attachment, readHandler);
								}
							}
						}

						@Override
						public void failed(Throwable exc, Object[] attachment) {
							exc.printStackTrace();
							try {
								((AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS])
										.close();
							} catch (IOException e) {
								// NOPE
							}
						}
					});
				}
			}

			@Override
			public void failed(Throwable exc, Object[] attachment) {
				//exc.printStackTrace();
				try {
					((AsynchronousSocketChannel) attachment[Constants.CHANNEL_POS]).close();
				} catch (IOException e) {
					// NOPE
				}
			}
		});

	}
}
