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

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@code AbstractServer}
 * 
 * Created on Oct 27, 2012 at 5:10:43 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public abstract class AbstractServer implements Runnable {

	/**
	 *
	 */
	protected int port;
	protected boolean async = false;
	protected ExecutorService executor = Executors.newFixedThreadPool(512);

	/**
	 * Create a new instance of {@code Server}
	 */
	public AbstractServer(int port) {
		this.port = port;
	}

	/**
	 * Generate a random and unique session Id
	 * 
	 * @return a random and unique session Id
	 */
	public static String generateSessionId() {
		return UUID.randomUUID().toString();
	}
}
