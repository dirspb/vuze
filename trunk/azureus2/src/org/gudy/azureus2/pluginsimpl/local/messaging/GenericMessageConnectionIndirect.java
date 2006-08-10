/*
 * Created on 10 Aug 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.messaging;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

import com.aelitis.azureus.core.nat.NATTraverser;


public class 
GenericMessageConnectionIndirect 
	implements GenericMessageConnectionAdapter
{
	public static final int MAX_MESSAGE_SIZE	= 32*1024; 
		
	private static final int MESSAGE_TYPE_CONNECT		= 1;
	private static final int MESSAGE_TYPE_ERROR			= 2;
	private static final int MESSAGE_TYPE_DATA			= 3;
	private static final int MESSAGE_TYPE_DISCONNECT	= 4;
	
	private static final int KEEP_ALIVE_PERIOD	= 10000;
	
	private static long	connection_id_next	= new Random().nextLong();
	
	private static Map	local_connections 	= new HashMap();
	private static Map	remote_connections 	= new HashMap();
	
	private static ThreadPool	keep_alive_pool = new ThreadPool( "GenericMessageConnectionIndirect:keepAlive", 8, true );
	
	static{
		
			// there are two reasons for timers
			//     1) to check for dead connections (i.e. keepalive)
			//     2) the connection is one-sided so if the responder sends an unsolicited message it 
			//        is queued and only picked up on a periodic ping by the initiator
		
		SimpleTimer.addPeriodicEvent(
			KEEP_ALIVE_PERIOD/2,
			new TimerEventPerformer()
			{				
				public void
				perform(
					TimerEvent	event )
				{
					synchronized( local_connections ){
					
						Iterator	it = local_connections.values().iterator();
						
						while( it.hasNext()){
							
							final GenericMessageConnectionIndirect con = (GenericMessageConnectionIndirect)it.next();
							
							if ( con.prepareForKeepAlive()){
								
								keep_alive_pool.run(
									new AERunnable()
									{
										public void
										runSupport()
										{
											con.keepAlive();
										}
									});
							}
						}
					}
				}
				
			});
	}
	
	protected static Map
	receive(
		MessageManagerImpl		message_manager,
		InetSocketAddress		originator,
		Map						message )
	{
		System.out.println( "GenericMessageConnectionIndirect::receive: " + originator + "/" + message );
		
			// if this purely a NAT traversal request then bail out 
		
		if ( !message.containsKey( "type" )){
			
			return( null );
		}
		
		int	type = ((Long)message.get("type")).intValue();
		
		if ( type == MESSAGE_TYPE_CONNECT ){
		
			String	msg_id 		= new String((byte[])message.get( "msg_id" ));
			String	msg_desc 	= new String((byte[])message.get( "msg_desc" ));

			GenericMessageEndpointImpl	endpoint = new GenericMessageEndpointImpl( originator );
			
			endpoint.addUDP( originator );
					
			GenericMessageHandler	handler = message_manager.getHandler( msg_id );
			
			if ( handler == null ){
				
				Debug.out( "No message handler registered for '" + msg_id + "'" );
				
				return( null );
			}
			
			try{
				Long	con_id;

				synchronized( remote_connections ){
					
					con_id = new Long( connection_id_next++ );
				}
				
				GenericMessageConnectionIndirect indirect_connection = 
					new GenericMessageConnectionIndirect( 
							message_manager, msg_id, msg_desc, endpoint, con_id.longValue());

				GenericMessageConnectionImpl new_connection = new GenericMessageConnectionImpl( message_manager, indirect_connection );

				if ( handler.accept( new_connection )){
					
					new_connection.accepted();
	
					synchronized( remote_connections ){
												
						remote_connections.put( con_id, indirect_connection );
					}
					
					List	replies = indirect_connection.receive((List)message.get( "data" ));
				
					Map	reply = new HashMap();
					
					reply.put( "type", new Long( MESSAGE_TYPE_CONNECT ));
					reply.put( "con_id", con_id );
					reply.put( "data", replies );
					
					return( reply );
					
				}else{
					
					return( null );
				}	
			
			}catch( MessageException e ){
				
				Debug.out( "Error accepting message", e);
				
				return( null );
			}

		}else if ( type == MESSAGE_TYPE_DATA ){
			
			Long	con_id = (Long)message.get( "con_id" );
			
			GenericMessageConnectionIndirect indirect_connection;
			
			synchronized( remote_connections ){
				
				indirect_connection = (GenericMessageConnectionIndirect)remote_connections.get( con_id );
			}
			
			if ( indirect_connection == null ){
				
				return( null );
			}
			
			Map	reply = new HashMap();

			if ( indirect_connection.isClosed()){
				
				reply.put( "type", new Long( MESSAGE_TYPE_DISCONNECT ));

			}else{
				
				List replies = indirect_connection.receive((List)message.get( "data" ));
				
				reply.put( "type", new Long( MESSAGE_TYPE_DATA ));
				reply.put( "data", replies );	
			}
			
			return( reply );
			
		}else{
			
				// error or disconnect		
			
			Long	con_id = (Long)message.get( "con_id" );
			
			GenericMessageConnectionIndirect indirect_connection;
			
			synchronized( remote_connections ){
				
				indirect_connection = (GenericMessageConnectionIndirect)remote_connections.get( con_id );
			}
			
			if ( indirect_connection != null ){
				
				try{
					indirect_connection.close( true );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			return( null );
		}
	}
	

	
	
	private MessageManagerImpl			message_manager;
	private String						msg_id;
	private String						msg_desc;
	private GenericMessageEndpoint		endpoint;
	
	private NATTraverser					nat_traverser;
	private GenericMessageConnectionImpl	owner;
	
	private	InetSocketAddress		rendezvous;
	private InetSocketAddress		target;
	
	private long					connection_id;
	private boolean					incoming;
	private boolean					closed;
	
	private LinkedList	send_queue		= new LinkedList();
	private AESemaphore	send_queue_sem	= new AESemaphore( "GenericMessageConnectionIndirect:sendq" );
	
	private volatile long		last_message_sent;
	private volatile boolean	keep_alive_in_progress;
	
	protected 
	GenericMessageConnectionIndirect(
		MessageManagerImpl			_message_manager,
		String						_msg_id,
		String						_msg_desc,
		GenericMessageEndpoint		_endpoint,
		InetSocketAddress			_rendezvous,
		InetSocketAddress			_target )
	{
			// outgoing
		
		message_manager	= _message_manager;
		msg_id			= _msg_id;
		msg_desc		= _msg_desc;
		endpoint		= _endpoint;
		rendezvous		= _rendezvous;
		target			= _target;
		
		nat_traverser = message_manager.getNATTraverser();
	}
	
	protected 
	GenericMessageConnectionIndirect(
		MessageManagerImpl			_message_manager,
		String						_msg_id,
		String						_msg_desc,
		GenericMessageEndpoint		_endpoint,
		long						_connection_id )
	{
			// incoming
		
		message_manager	= _message_manager;
		msg_id			= _msg_id;
		msg_desc		= _msg_desc;
		endpoint		= _endpoint;
		connection_id	= _connection_id;
		
		incoming		= true;
	}
	
	public void
	setOwner(
		GenericMessageConnectionImpl	_owner )
	{
		owner	= _owner;
	}
	
	public int
	getMaximumMessageSize()
	{
		return( MAX_MESSAGE_SIZE );
	}
	
	public GenericMessageEndpoint
	getEndpoint()
	{
		return( endpoint );
	}
	
	public void
	connect(
		ByteBuffer			initial_data,
		ConnectionListener	listener )
	{
		try{
			Map	message = new HashMap();
			
			byte[]	initial_data_bytes = new byte[ initial_data.remaining()];
			
			initial_data.get( initial_data_bytes );
			
			List	initial_messages = new ArrayList();
			
			initial_messages.add( initial_data_bytes );
			
			message.put( "type", new Long( MESSAGE_TYPE_CONNECT ));
			message.put( "msg_id", msg_id );
			message.put( "msg_desc", msg_desc );
			message.put( "data", initial_messages );
			
			Map reply = nat_traverser.sendMessage( message_manager, rendezvous, target, message );
	
			last_message_sent = SystemTime.getCurrentTime();

			if ( reply == null || !reply.containsKey( "type") ){
				
				listener.connectFailure( new Throwable( "Indirect connect failed (response=" + reply + ")" ));
				
			}else{
				
				int	reply_type = ((Long)reply.get( "type" )).intValue();
				
				if ( reply_type == MESSAGE_TYPE_ERROR ){
					
					listener.connectFailure( new Throwable( new String((byte[])reply.get( "error" ))));
	
				}else if ( reply_type == MESSAGE_TYPE_DISCONNECT ){
						
					listener.connectFailure( new Throwable( "Disconnected" ));

				}else if ( reply_type == MESSAGE_TYPE_CONNECT ){
										
					connection_id = ((Long)reply.get( "con_id" )).longValue();
					
					synchronized( local_connections ){
						
						local_connections.put( new Long( connection_id ), this );
					}
					
					listener.connectSuccess();

					List	replies = (List)reply.get( "data" );
					
					for (int i=0;i<replies.size();i++){
							
						owner.receive( new GenericMessage(msg_id, msg_desc, new DirectByteBuffer(ByteBuffer.wrap((byte[])replies.get(i))), false ));
					}
					
				}else{
					
					Debug.out( "Unexpected reply type - " + reply_type );
					
					listener.connectFailure( new Throwable( "Unexpected reply type - " + reply_type ));
				}
			}
		}catch( Throwable e ){
			
			listener.connectFailure( e );
		}
	}
	
	public void
	accepted()
	{
	}
	
	public void
	send(
		PooledByteBuffer			pbb )
	
		throws MessageException
	{
		if ( incoming ){
			
			synchronized( send_queue ){
				
				if ( send_queue.size() > 64 ){
					
					throw( new MessageException( "Send queue limit exceeded" ));
				}
				
				System.out.println( "  added entry to send queue, rem = " + send_queue.size());

				send_queue.add( pbb.toByteArray());
			}
			
			send_queue_sem.release();
			
		}else{
						
			List	messages = new ArrayList();
			
			messages.add( pbb.toByteArray());
			
			send( messages );
		}
	}
	
	protected void
	send(
		List	messages )
	{
		try{
			Map	message = new HashMap();
			
			message.put( "con_id", new Long( connection_id ));
			message.put( "type", new Long( MESSAGE_TYPE_DATA ));
			message.put( "data", messages );
			
			Map reply = nat_traverser.sendMessage( message_manager, rendezvous, target, message );
	
			last_message_sent = SystemTime.getCurrentTime();

			if ( reply == null || !reply.containsKey( "type")){
				
				owner.reportFailed( new Throwable( "Indirect message send failed (response=" + reply + ")" ));
				
			}else{
				
				int	reply_type = ((Long)reply.get( "type" )).intValue();
				
				if ( reply_type == MESSAGE_TYPE_ERROR ){
					
					owner.reportFailed( new Throwable( new String((byte[])reply.get( "error" ))));
	
				}else if ( reply_type == MESSAGE_TYPE_DATA ){
					
					List	replies = (List)reply.get( "data" );
											
					for (int i=0;i<replies.size();i++){
							
						owner.receive( new GenericMessage(msg_id, msg_desc, new DirectByteBuffer(ByteBuffer.wrap((byte[])replies.get(i))), false ));
					}
					
				}else if ( reply_type == MESSAGE_TYPE_DISCONNECT ){
					
					owner.reportFailed( new Throwable( "Disconnected" ));
				}
			}
		}catch( Throwable e ){
			
			owner.reportFailed( e );
		}
	}
	
	protected List
	receive(
		List		messages )
	{
		System.out.println( "receive: " + messages );
		
		
		for (int i=0;i<messages.size();i++){
			
			owner.receive( new GenericMessage(msg_id, msg_desc, new DirectByteBuffer(ByteBuffer.wrap((byte[])messages.get(i))), false ));
		}
		
		List	reply = new ArrayList();
		
			// hang around a bit to see if we can piggyback a reply
		
		if ( send_queue_sem.reserve( 2500 )){
			
				// give a little more time in case async > 1 message is being queued
			
			try{
				Thread.sleep(250);
				
			}catch( Throwable e ){
			}
			
			synchronized( send_queue ){
				
				while( send_queue.size() > 0 ){
									
					reply.add( send_queue.removeFirst());
				}
			}
			
				// grab sems for any entries other than the initial one grabbed above
			
			for (int i=1;i<reply.size();i++){
				
				send_queue_sem.reserve();
			}
		}
		
		return( reply );
	}
	
	public void
	close()
	
		throws MessageException
	{
		close( false );
	}
	
	protected void
	close(
		boolean	remote_closed )
	
		throws MessageException
	{
		if ( closed ){
			
			return;
		}
		
		try{
			closed	= true;
			
			if ( incoming ){
				
				synchronized( remote_connections ){
					
					remote_connections.remove( new Long( connection_id ));
				}
			}else{
				
				
				synchronized( local_connections ){
					
					local_connections.remove( new Long( connection_id ));
				}
				
				Map	message = new HashMap();
				
				message.put( "con_id", new Long( connection_id ));
				message.put( "type", new Long( MESSAGE_TYPE_DISCONNECT ));
				
				try{
					nat_traverser.sendMessage( message_manager, rendezvous, target, message );
					
					last_message_sent = SystemTime.getCurrentTime();

				}catch( Throwable e ){
					
					throw( new MessageException( "Close operation failed", e ));
				}
			}
		}finally{
			
			if ( remote_closed ){
				
				owner.reportFailed( new Throwable( "Remote closed connection" ));
			}
		}
	}
	
	protected boolean
	isClosed()
	{
		return( closed );
	}
	
	protected boolean
	prepareForKeepAlive()
	{
		if ( keep_alive_in_progress ){
			
			return( false );
		}
		
		long	now = SystemTime.getCurrentTime();
		
		if ( now < last_message_sent || now - last_message_sent > KEEP_ALIVE_PERIOD ){
			
			keep_alive_in_progress = true;
		
			return( true );
		}
		
		return( false );
	}
	
	protected void
	keepAlive()
	{
		try{
			
			send( new ArrayList());
			
		}finally{
			
			keep_alive_in_progress	= false;
		}
	}
}
