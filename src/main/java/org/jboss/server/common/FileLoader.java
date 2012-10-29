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
package org.jboss.server.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jboss.server.xnio3.common.XnioUtils;

/**
 * {@code FileLoader}
 * 
 * Created on Oct 26, 2012 at 2:39:38 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public final class FileLoader {

	private static long fileLength;
	private static ByteBuffer data[];

	/**
	 * Create a new instance of {@code FileLoader}
	 */
	private FileLoader() {
		super();
	}

	/**
	 * 
	 * @return
	 */
	public static ByteBuffer[] cloneData() {
		if (data == null) {
			try {
				init();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ByteBuffer[] buffers = new ByteBuffer[data.length];
		synchronized (data) {
			for (int i = 0; i < data.length; i++) {
				buffers[i] = ByteBuffer.allocateDirect(data[i].capacity());
				data[i].flip();
				buffers[i].put(data[i]);
			}
		}

		return buffers;
	}

	/**
	 * 
	 * @throws IOException
	 */
	private static synchronized void init() throws IOException {

		File file = new File("data" + File.separatorChar + "file.txt");
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			FileChannel fileChannel = raf.getChannel();

			fileLength = fileChannel.size() + XnioUtils.CRLF.getBytes().length;
			double tmp = (double) fileLength / XnioUtils.WRITE_BUFFER_SIZE;
			int length = (int) Math.ceil(tmp);
			data = new ByteBuffer[length];

			for (int i = 0; i < data.length - 1; i++) {
				data[i] = ByteBuffer.allocateDirect(XnioUtils.WRITE_BUFFER_SIZE);
			}

			int size = (int) (fileLength % XnioUtils.WRITE_BUFFER_SIZE);
			data[data.length - 1] = ByteBuffer.allocateDirect(size);
			// Read the whole file in one pass
			fileChannel.read(data);
		}
		// Put the <i>CRLF</i> chars at the end of the last byte buffer to mark
		// the end of data
		data[data.length - 1].put(XnioUtils.CRLF.getBytes());

	}

	/**
	 * @return the fileLength
	 */
	public static long getFileLength() {
		return fileLength;
	}

}
