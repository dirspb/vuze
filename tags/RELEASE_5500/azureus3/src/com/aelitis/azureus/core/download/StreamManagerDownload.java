/*
 * Created on Jun 21, 2010
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.download;

import java.net.URL;

import org.gudy.azureus2.core3.download.DownloadManager;

public interface 
StreamManagerDownload 
{
	public DownloadManager
	getDownload();
	
	public int
	getFileIndex();
	
	public URL
	getURL();
	
	public boolean
	getPreviewMode();
	
	public void
	setPreviewMode(
		boolean	preview_mode );
	
	public void
	cancel();
	
	public boolean
	isCancelled();
}
