/*
 * Created on 25-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.resourcedownloader.impl;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.resourcedownloader.*;

public class 
ResourceDownloaderTimeoutImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	protected ResourceDownloader		delegate;
	protected int						timeout_millis;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;

	protected Object					result;
	protected Semaphore					done_sem	= new Semaphore();
		
	public
	ResourceDownloaderTimeoutImpl(
		ResourceDownloader	_delegate,
		int					_timeout_millis )
	{
		delegate			= _delegate;
		timeout_millis		= _timeout_millis;
	}
	
	public String
	getName()
	{
		return( delegate.getName() + ", timeout=" + timeout_millis );
	}
	
	public ResourceDownloader
	getClone()
	{
		return( new ResourceDownloaderTimeoutImpl( delegate.getClone(), timeout_millis ));
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		asyncDownload();
		
		done_sem.reserve();
		
		if ( result instanceof InputStream ){
			
			return((InputStream)result);
		}
		
		throw((ResourceDownloaderException)result);
	}
	
	public synchronized void
	asyncDownload()
	{		
		if ( !cancelled ){
			
			current_downloader = delegate.getClone();
			
			current_downloader.addListener( this );
			
			current_downloader.asyncDownload();
		
			Thread t = new Thread( "ResourceDownloaderTimeout")
				{
					public void
					run()
					{
						try{
							Thread.sleep( timeout_millis );
							
							cancel(new ResourceDownloaderException( "Download timeout"));
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				};
			
			t.setDaemon(true);
	
			t.start();
		}
	}
	
	public synchronized void
	cancel()
	{
		cancel( new ResourceDownloaderException( "Download cancelled"));
	}
	
	protected synchronized void
	cancel(
		ResourceDownloaderException reason )
	{
		result	= reason; 
		
		cancelled	= true;
	
		informFailed((ResourceDownloaderException)result );
		
		if ( current_downloader != null ){
			
			current_downloader.cancel();
		}
	}	
	
	public void
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		result	= data;
		
		done_sem.release();
		
		informComplete( data );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		done_sem.release();
		
		informFailed( e );
	}
}
