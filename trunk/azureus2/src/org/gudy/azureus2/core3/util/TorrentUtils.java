/*
 * File    : TorrentUtils.java
 * Created : 13-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import  org.gudy.azureus2.core.MessageText;
import  org.gudy.azureus2.core3.torrent.*;

public class 
TorrentUtils 
{
	public static String
	exceptionToText(
		TOTorrentException	e )
	{
		String	errorDetail;
		
		int	reason = e.getReason();
  			
		if ( reason == TOTorrentException.RT_FILE_NOT_FOUND ){
 	     	        		 		
			errorDetail = MessageText.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
	        		
		}else if ( reason == TOTorrentException.RT_ZERO_LENGTH ){
	     
			errorDetail = MessageText.getString("DownloadManager.error.fileempty"); //$NON-NLS-1$
	        
		}else if ( reason == TOTorrentException.RT_TOO_BIG ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.filetoobig"); //$NON-NLS-1$
			        
		}else if ( reason == TOTorrentException.RT_DECODE_FAILS ){
	 
			errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo"); //$NON-NLS-1$
	 		        
		}else if ( reason == TOTorrentException.RT_UNSUPPORTED_ENCODING ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_READ_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
					
		}else if ( reason == TOTorrentException.RT_HASH_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
		}else{
	 	     
			errorDetail = e.getMessage();
		}
		
		return( errorDetail );
	}
}
