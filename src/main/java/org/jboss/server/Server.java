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

package org.jboss.server;

import org.jboss.logging.Logger;
import org.jboss.server.common.Constants;

/**
 * {@code Server}
 * 
 * Created on Oct 29, 2012 at 12:19:48 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public abstract class Server {
	
	/**
	 *
	 */
	public static final Logger	LOG	= Logger.getLogger(Server.class);
	
	/**
	 * Create a new instance of {@code Server}
	 */
	public Server() {
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println();
		if (args.length < 2) {
			System.err.println("Usage: java " + Server.class.getName() + " type mode [port]\n");
			System.err.println("  --> type: xnio or nio (Allowed values: \"xnio3\" and \"nio2\")");
			System.err.print("  --> mode: the channel processing mode, i.e, sync/async (");
			System.err.println("Allowed values: \"sync\" or \"async\")");
			System.err
					.println("  --> port: the server port number to which the server channel will bind.");
			System.err.println("            Default value: 8080");
			System.out.println();
			System.exit(-1);
		}
		
		int port = Constants.DEFAULT_SERVER_PORT;
		if (args.length >= 3) {
			try {
				port = Integer.valueOf(args[2]);
				if (port <= 1024) {
					LOG.error("Invalid port number. The port number must be between 1025 and 65535");
					port = Constants.DEFAULT_SERVER_PORT;
					LOG.infov("Adjusting port number to the default server port {0}", port);
				}
			} catch (Throwable e) {
				LOG.errorv("Invalid port number format: {0}", args[2]);
				LOG.infov("Using the default port number {0}", port);
			}
		}
		
		switch (args[0]) {
			case "nio2":
				org.jboss.server.nio2.MainServer.run(args[1], port);
				break;
			case "xnio3":
				org.jboss.server.xnio3.MainServer.run(args[1], port);
				break;
			default:
				LOG.errorv("Unknown server type \"{0}\"", args[0]);
				LOG.error("Allowed values: \"xnio3\" and \"nio2\"");
				break;
		}
	}
	
}
