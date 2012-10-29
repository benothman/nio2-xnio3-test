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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;

import org.jboss.server.nio2.common.Nio2Utils;

/**
 * {@code ReadCompletionHandler}
 * 
 * Created on Nov 16, 2011 at 8:59:51 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
class ReadCompletionHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {

	private String sessionId;
	// The read buffer
	private ByteBuffer readBuffer;
	// An array of byte buffers for write operations
	private ByteBuffer writeBuffers[];
	private long fileLength;

	/**
	 * Create a new instance of {@code ReadCompletionHandler}
	 * 
	 * @param sessionId
	 * @param byteBuffer
	 */
	public ReadCompletionHandler(String sessionId, ByteBuffer byteBuffer) {
		this.sessionId = sessionId;
		this.readBuffer = byteBuffer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void completed(Integer nBytes, AsynchronousSocketChannel channel) {
		if (nBytes < 0) {
			failed(new ClosedChannelException(), channel);
			return;
		}

		if (nBytes > 0) {
			readBuffer.flip();
			byte bytes[] = new byte[nBytes];
			readBuffer.get(bytes);
			try {
				// write response to client
				writeResponse(channel);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Read again with the this CompletionHandler
		readBuffer.clear();
		channel.read(readBuffer, Nio2Utils.TIMEOUT, Nio2Utils.TIME_UNIT, channel, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable,
	 * java.lang.Object)
	 */
	@Override
	public void failed(Throwable exc, AsynchronousSocketChannel channel) {
		System.out.println("[" + this.sessionId + "] Read Operation failed");
		exc.printStackTrace();
		try {
			System.out.println("[" + this.sessionId + "] Closing remote connection");
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write the response to client
	 * 
	 * @param channel
	 *            the {@code AsynchronousSocketChannel} channel to which write
	 * @throws Exception
	 */
	protected void writeResponse(AsynchronousSocketChannel channel) throws Exception {
		if (this.writeBuffers == null) {
			initWriteBuffers();
		}
		// Write the file content to the channel
		write(channel, this.writeBuffers);
	}

	/**
	 * Flip all the write byte buffers
	 * 
	 * @param buffers
	 */
	protected static void flipAll(ByteBuffer[] buffers) {
		for (ByteBuffer bb : buffers) {
			bb.flip();
		}
	}

	/**
	 * Read the file from HD and initialize the write byte buffers array.
	 * 
	 * @throws IOException
	 */
	private void initWriteBuffers() throws IOException {

		File file = new File("data" + File.separatorChar + "file32k.txt");
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			FileChannel fileChannel = raf.getChannel();

			fileLength = fileChannel.size() + Nio2Utils.CRLF.getBytes().length;
			double tmp = (double) fileLength / Nio2Utils.WRITE_BUFFER_SIZE;
			int length = (int) Math.ceil(tmp);
			writeBuffers = new ByteBuffer[length];

			for (int i = 0; i < writeBuffers.length - 1; i++) {
				writeBuffers[i] = ByteBuffer.allocate(Nio2Utils.WRITE_BUFFER_SIZE);
			}

			int temp = (int) (fileLength % Nio2Utils.WRITE_BUFFER_SIZE);
			writeBuffers[writeBuffers.length - 1] = ByteBuffer.allocateDirect(temp);
			// Read the whole file in one pass
			fileChannel.read(writeBuffers);
		}
		// Put the <i>CRLF</i> chars at the end of the last byte buffer to mark
		// the end of data
		writeBuffers[writeBuffers.length - 1].put(Nio2Utils.CRLF.getBytes());
	}

	/**
	 * 
	 * @param channel
	 * @param buffers
	 * @param length
	 * @throws Exception
	 */
	protected void write(final AsynchronousSocketChannel channel, final ByteBuffer[] buffers,
			final long total) throws Exception {

		// Flip all the write byte buffers
		flipAll(buffers);
		// Write response to client
		channel.write(buffers, 0, buffers.length, Nio2Utils.TIMEOUT, Nio2Utils.TIME_UNIT, total,
				new CompletionHandler<Long, Long>() {
					private int offset = 0;
					private long written = 0;

					@Override
					public void completed(Long nBytes, Long total) {
						System.out.println("Written = " + nBytes+" of " + total);
						written += nBytes;
						if (written < total) {
							offset = (int) (written / buffers[0].capacity());
							channel.write(buffers, offset, buffers.length - offset,
									Nio2Utils.TIMEOUT, Nio2Utils.TIME_UNIT, total, this);
						}
					}

					@Override
					public void failed(Throwable exc, Long attachment) {
						exc.printStackTrace();
					}
				});
	}

	/**
	 * 
	 * @param channel
	 * @param buffers
	 * @throws Exception
	 */
	protected void write(final AsynchronousSocketChannel channel, final ByteBuffer[] buffers)
			throws Exception {
		for (ByteBuffer buffer : buffers) {
			write(channel, buffer);
		}
	}

	/**
	 * Write the byte buffer to the specified channel
	 * 
	 * @param channel
	 *            the {@code AsynchronousSocketChannel} channel
	 * @param byteBuffer
	 *            the data that will be written to the channel
	 * @throws IOException
	 */
	protected int write(AsynchronousSocketChannel channel, ByteBuffer byteBuffer) throws Exception {
		byteBuffer.flip();
		return channel.write(byteBuffer).get();
	}
}
