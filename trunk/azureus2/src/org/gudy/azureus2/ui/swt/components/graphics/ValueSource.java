/*
 * Created on Apr 26, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package org.gudy.azureus2.ui.swt.components.graphics;

import org.eclipse.swt.graphics.Color;

public interface 
ValueSource 
{
	public static final int STYLE_NONE	= 0x00000000;
	public static final int STYLE_UP	= 0x00000001;
	public static final int STYLE_DOWN	= 0x00000002;
	public static final int STYLE_NAMED	= 0x00000004;
	
	public String
	getName();
	
	public Color
	getLineColor();
	
	public boolean
	isTrimmable();
	
	public int
	getValue();
	
	public int
	getStyle();
}
