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
import java.nio.channels.Channel;

import org.jboss.server.xnio3.XnioServer;
import org.xnio.ChannelListener;
import org.xnio.Options;
import org.xnio.channels.StreamChannel;

/**
 * {@code AsyncServer}
 * 
 * Created on Nov 10, 2011 at 3:41:02 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class AsyncServer extends XnioServer {

	/**
	 * Create a new instance of {@code AsyncServer}
	 * 
	 * @param port
	 */
	public AsyncServer(int port) {
		super(port);
		this.async = true;
	}

	/**
	 * {@code CloseChannelListener}
	 * 
	 * Created on Nov 11, 2011 at 1:58:40 PM
	 * 
	 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
	 */
	private class CloseChannelListener implements ChannelListener<StreamChannel> {

		private String sessionId;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xnio.ChannelListener#handleEvent(java.nio.channels.Channel)
		 */
		@Override
		public void handleEvent(StreamChannel channel) {
			logger.infov("Closing remote connection for session: {0} (counter = {1}) ",
					this.sessionId, counter.decrementAndGet());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.server.xnio3.common.Xnio3Server#getAcceptListener()
	 */
	@Override
	public ChannelListener<Channel> getAcceptListener() {
		return new ChannelListener<Channel>() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.xnio.ChannelListener#handleEvent(java.nio.channels.Channel)
			 */
			@Override
			public void handleEvent(Channel channel) {
				counter.incrementAndGet();
				final StreamChannel streamChannel = (StreamChannel) channel;
				String sessionId = generateSessionId();
				try {
					// Fix the size of the send buffer to 8KB
					streamChannel.setOption(Options.SEND_BUFFER, 8 * 1024);
					initSession(streamChannel, sessionId);
					// Setup the read listener
					ReadChannelListener readListener = new ReadChannelListener();
					readListener.setSessionId(sessionId);
					streamChannel.getReadSetter().set(readListener);
					// Setup the close listener
					//CloseChannelListener closeListener = new CloseChannelListener();
					//closeListener.sessionId = sessionId;
					//streamChannel.getCloseSetter().set(closeListener);
					// Resume reads
					streamChannel.resumeReads();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}
}
