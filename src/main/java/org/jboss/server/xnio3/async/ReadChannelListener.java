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
package org.jboss.server.xnio3.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jboss.server.common.FileLoader;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamChannel;

/**
 * {@code ReadChannelListener}
 * 
 * Created on Nov 22, 2011 at 4:44:01 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class ReadChannelListener implements ChannelListener<StreamChannel> {
	
	private String		sessionId;
	private ByteBuffer	readBuffer;
	private ByteBuffer	writeBuffers[];
	private long		fileLength;
	
	/**
	 * Create a new instance of {@code ReadChannelListener}
	 */
	public ReadChannelListener() {
		this.init();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xnio.ChannelListener#handleEvent(java.nio.channels.Channel )
	 */
	public void handleEvent(StreamChannel channel) {
		try {
			int nBytes = channel.read(readBuffer);
			if (nBytes < 0) {
				// means that the connection was closed remotely
				channel.close();
				return;
			}
			
			if (nBytes > 0) {
				readBuffer.flip();
				byte bytes[] = new byte[nBytes];
				readBuffer.get(bytes).clear();
				writeResponse(channel);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param channel
	 * @throws Exception
	 */
	void writeResponse(StreamChannel channel) throws Exception {
		// Write the file content to the channel
		write(channel, writeBuffers, fileLength);
	}
	
	/**
	 * 
	 * @param buffers
	 */
	public void flipAll(ByteBuffer[] buffers) {
		for (ByteBuffer bb : buffers) {
			if (bb.position() > 0) {
				bb.flip();
			}
		}
	}
	
	/**
	 * 
	 * @param channel
	 * @param buffers
	 * @throws Exception
	 */
	protected void write(final StreamChannel channel, final ByteBuffer[] buffers, final long total)
			throws Exception {
		
		// Flip all buffers
		flipAll(buffers);
		
		long nw = 0, x = 0;
		int offset = 0, len = buffers.length;
		do {
			offset = (int) (nw / buffers[0].capacity());
			// Wait until the channel becomes writable again
			channel.awaitWritable();
			// Write data to client
			x = channel.write(buffers, offset, len - offset);
			if (x < 0) {
				throw new ClosedChannelException();
			}
			nw += x;
		} while (nw < total);
	}
	
	/**
	 * 
	 * @param channel
	 * @param buffer
	 * @throws IOException
	 */
	void write(StreamChannel channel, ByteBuffer byteBuffer) throws IOException {
		byteBuffer.flip();
		// Wait until the channel becomes writable again
		channel.awaitWritable();
		channel.write(byteBuffer);
	}
	
	/**
	 * Read the file from HD and initialize the write byte buffers array.
	 */
	private void init() {
		this.readBuffer = ByteBuffer.allocateDirect(512);
		this.writeBuffers = FileLoader.cloneData();
		this.fileLength = FileLoader.getFileLength();
	}
	
	/**
	 * @return the readBuffer
	 */
	public ByteBuffer getReadBuffer() {
		return this.readBuffer;
	}
	
	/**
	 * Getter for sessionId
	 * 
	 * @return the sessionId
	 */
	public String getSessionId() {
		return this.sessionId;
	}
	
	/**
	 * Setter for the sessionId
	 * 
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
}
