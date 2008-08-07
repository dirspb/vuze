/*
 * Created on Jul 29, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.subscriptions;

import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionHistory;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.core.subs.impl.SubscriptionDownloader;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.listener.MetaSearchListener;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;

public class 
SubscriptionManagerUI 
{
	private static final Object	SUB_IVIEW_KEY = new Object();
	
	private Graphic	icon_rss;
	private List	icon_list	= new ArrayList();
	
	private TableColumn	subs_i_column;
	private TableColumn	subs_c_column;
	
	private SubscriptionManager	subs_man;
	
	private boolean		side_bar_setup;
	
	public
	SubscriptionManagerUI(
		AzureusCore			core )
	{
		PluginInterface	default_pi = core.getPluginManager().getDefaultPluginInterface();
		
		final TableManager	table_manager = default_pi.getUIManager().getTableManager();

		
		if ( Constants.isCVSVersion()){			
			
			// check assoc
		
		{
			final TableContextMenuItem menu_item_itorrents = 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.lookupassoc");
			final TableContextMenuItem menu_item_ctorrents 	= 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.lookupassoc");
			
			menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
			menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_PUSH);
	
			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						if ( rows.length > 0 ){
							
							Download download = (Download)rows[0].getDataSource();
							
							new SubscriptionListWindow(PluginCoreUtils.unwrap(download), false);
						}
						/*
						for (int i=0;i<rows.length;i++){
							
							Download download = (Download)rows[i].getDataSource();
							
							Torrent t = download.getTorrent();
							
							if ( t != null ){
								
								try{
									lookupAssociations( 
										t.getHash(),
										new SubscriptionLookupListener()
										{
											public void
											found(
												byte[]					hash,
												Subscription			subscription )
											{
												log( "    lookup: found " + ByteFormatter.encodeString( hash ) + " -> " + subscription.getName());
											}
											
											public void
											complete(
												byte[]					hash,
												Subscription[]			subscriptions )
											{
												log( "    lookup: complete " + ByteFormatter.encodeString( hash ) + " -> " +subscriptions.length );
	
											}
											
											public void
											failed(
												byte[]					hash,
												SubscriptionException	error )
											{
												log( "    lookup: failed", error );
											}
										});
									
								}catch( Throwable e ){
									
									log( "Lookup failed", e );
								}
							}	
						}*/
					}
				};
			
			menu_item_itorrents.addMultiListener( listener );
			menu_item_ctorrents.addMultiListener( listener );	
		}
		
			// make assoc
		
		{
			final TableContextMenuItem menu_item_itorrents = 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azsubs.contextmenu.addassoc");
			final TableContextMenuItem menu_item_ctorrents 	= 
				table_manager.addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azsubs.contextmenu.addassoc");
			
			menu_item_itorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			menu_item_ctorrents.setStyle(TableContextMenuItem.STYLE_MENU);
			
			MenuItemFillListener	menu_fill_listener = 
				new MenuItemFillListener()
				{
					public void
					menuWillBeShown(
						MenuItem	menu,
						Object		target )
					{	
						if ( subs_man == null ){
							
							return;
						}
						
						TableRow[]	rows;
						
						if ( target instanceof TableRow[] ){
							
							rows = (TableRow[])target;
							
						}else{
							
							rows = new TableRow[]{ (TableRow)target };
						}
						
						final List	hashes = new ArrayList();
						
						for (int i=0;i<rows.length;i++){
							
							Download	download = (Download)rows[i].getDataSource();
						
							if ( download != null ){
								
								Torrent torrent = download.getTorrent();
								
								if ( torrent != null ){
									
									hashes.add( torrent.getHash());
								}
							}
						}
													
						menu.removeAllChildItems();
						
						boolean enabled = hashes.size() > 0;
						
						if ( enabled ){
						
							Subscription[] subs = subs_man.getSubscriptions();
							
							boolean	incomplete = ((TableContextMenuItem)menu).getTableID() == TableManager.TABLE_MYTORRENTS_INCOMPLETE;
							
							TableContextMenuItem parent = incomplete?menu_item_itorrents:menu_item_ctorrents;
															
							for (int i=0;i<subs.length;i++){
								
								final Subscription	sub = subs[i];
								
								TableContextMenuItem item =
									table_manager.addContextMenuItem(
										parent,
										"!" + sub.getName() + "!");
								
								item.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem 	menu,
											Object 		target ) 
										{
											for (int i=0;i<hashes.size();i++){
												
												sub.addAssociation( (byte[])hashes.get(i));
											}
										}
									});
							}
						}
						
						menu.setEnabled( enabled );
					}
				};
				
			menu_item_itorrents.addFillListener( menu_fill_listener );
			menu_item_ctorrents.addFillListener( menu_fill_listener );		
		}
	}
	
		TableCellRefreshListener	refresh_listener = 
			new TableCellRefreshListener()
			{
				public void 
				refresh(
					TableCell cell )
				{
					if ( subs_man == null ){
						
						return;
					}
					
					Download	dl = (Download)cell.getDataSource();
					
					if ( dl == null ){
						
						return;
					}
					
					Torrent	torrent = dl.getTorrent();
					
					if ( torrent != null ){
						
						Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
											
						cell.setGraphic( subs.length > 0?icon_rss:null );
					}
				}
			};
			
		TableCellMouseListener	mouse_listener = 
			new TableCellMouseListener()
			{
				public void 
				cellMouseTrigger(
					TableCellMouseEvent event )
				{
					if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN ){
						
						event.skipCoreFunctionality	= true;
						
						TableCell cell = event.cell;
						
						Download	dl = (Download)cell.getDataSource();
						
						Torrent	torrent = dl.getTorrent();
						
						if ( torrent != null ){
							
							Subscription[] subs = subs_man.getKnownSubscriptions( torrent.getHash());
							
							if ( subs.length > 0 ){
								
								new SubscriptionListWindow(PluginCoreUtils.unwrap(dl),true);
							}
						}
					}
				}
			};
			
			// MyTorrents incomplete
			
		subs_i_column = 
			table_manager.createColumn(
					TableManager.TABLE_MYTORRENTS_INCOMPLETE,
					"azsubs.ui.column.subs" );
		
		subs_i_column.setAlignment(TableColumn.ALIGN_CENTER);
		subs_i_column.setPosition(TableColumn.POSITION_LAST);
		subs_i_column.setMinWidth(100);
		subs_i_column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		subs_i_column.setType(TableColumn.TYPE_GRAPHIC);
		
		subs_i_column.addCellRefreshListener( refresh_listener );
		subs_i_column.addCellMouseListener( mouse_listener );
		
		table_manager.addColumn( subs_i_column );	
		
			// MyTorrents complete

		subs_c_column = 
			table_manager.createColumn(
					TableManager.TABLE_MYTORRENTS_COMPLETE,
					"azsubs.ui.column.subs" );
		
		subs_c_column.setAlignment(TableColumn.ALIGN_CENTER);
		subs_c_column.setPosition(TableColumn.POSITION_LAST);
		subs_c_column.setMinWidth(100);
		subs_c_column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		subs_c_column.setType(TableColumn.TYPE_GRAPHIC);
		
		subs_c_column.addCellRefreshListener( refresh_listener );
		subs_c_column.addCellMouseListener( mouse_listener );
		
		table_manager.addColumn( subs_c_column );	

		default_pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							icon_rss			= loadGraphic( swt, "rss.png" );

							subs_man = SubscriptionManagerFactory.getSingleton();
							
							subs_man.addListener(
								new SubscriptionManagerListener()
								{
									public void 
									subscriptionAdded(
										Subscription subscription ) 
									{
									}
									
									public void
									subscriptionChanged(
										Subscription		subscription )
									{
									}
									
									public void 
									subscriptionRemoved(
										Subscription subscription ) 
									{
									}
									
									public void 
									associationsChanged(
										byte[] hash )
									{
										subs_i_column.invalidateCells();
										subs_c_column.invalidateCells();
									}
								});	
							

							SkinViewManager.addListener(
								new SkinViewManagerListener() 
								{
									public void 
									skinViewAdded(
										SkinView skinview) 
									{
										if ( skinview instanceof SideBar ){
											
											setupSideBar((SideBar) skinview);
										}
									}
								});
							
							SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
							
							if ( sideBar != null ){
								
								setupSideBar( sideBar );
							}
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
					}
				});
		
	}
	
	protected void
	setupSideBar(
		final SideBar		side_bar )
	{
		synchronized( this ){
			
			if ( side_bar_setup ){
				
				return;
			}
			
			side_bar_setup = true;
		}
		
		subs_man.addListener(
			new SubscriptionManagerListener()
			{
				public void 
				subscriptionAdded(
					Subscription 		subscription ) 
				{
					addSubscription( side_bar, subscription );
				}
	
				public void
				subscriptionChanged(
					Subscription		subscription )
				{
					changeSubscription( side_bar, subscription );
				}
				
				public void 
				subscriptionRemoved(
					Subscription 		subscription ) 
				{
					removeSubscription( side_bar, subscription );
				}
				
				public void
				associationsChanged(
					byte[]		association_hash )
				{
					 
				}
			});
		
		Subscription[]	subs = subs_man.getSubscriptions();
		
		for (int i=0;i<subs.length;i++){
			
			addSubscription( side_bar, subs[i] );
		}
	}
	
	protected void
	changeSubscription(
		SideBar				side_bar,
		final Subscription	subs )
	{
		if ( subs.isSubscribed()){
			
			addSubscription(side_bar, subs);
			
		}else{
			
			removeSubscription(side_bar, subs);
		}
	}
	
	protected void
	addSubscription(
		final SideBar			side_bar,
		final Subscription		subs )
	{
		if ( !subs.isSubscribed()){
			
			return;
		}
		
		refreshColumns();
		
		synchronized( this ){
						
			if ( subs.getUserData( SUB_IVIEW_KEY ) == null ){
	
				final sideBarItem new_si = new sideBarItem();
				
				subs.setUserData( SUB_IVIEW_KEY, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( SubscriptionManagerUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								TreeItem  tree_item = 
									side_bar.createTreeItemFromIView(
										SideBar.SIDEBAR_SECTION_SUBSCRIPTIONS, 
										new subscriptionView( subs ),
										ByteFormatter.encodeString(subs.getPublicKey()), 
										null, 
										false, 
										true );
								
								new_si.setTreeItem( tree_item );
							}
						}
					});
			}
		}
	}
	
	protected void
	removeSubscription(
		SideBar				side_bar,
		final Subscription	subs )
	{
		synchronized( this ){
			
			final sideBarItem existing = (sideBarItem)subs.getUserData( SUB_IVIEW_KEY );
			
			if ( existing != null ){
				
				subs.setUserData( SUB_IVIEW_KEY, null );
				
				existing.destroy();
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								synchronized( SubscriptionManagerUI.this ){

									TreeItem ti = existing.getTreeItem();
									
									if ( ti != null ){
										
										ti.dispose();
									}
								}
							}
						});
			}
		}
		
		refreshColumns();
	}
	
	protected void
	refreshColumns()
	{
		subs_i_column.invalidateCells();
		subs_c_column.invalidateCells();
	}
	
	protected Graphic
	loadGraphic(
		UISWTInstance	swt,
		String			name )
	{
		Image	image = swt.loadImage( "org/gudy/azureus2/ui/icons/" + name );

		Graphic graphic = swt.createGraphic(image );
		
		icon_list.add( graphic );
		
		return( graphic );
	}
	
	protected static class
	subscriptionView
		extends AbstractIView
	{
		private Subscription	subs;
		
		private Composite		parent_composite;
		private Composite		composite;
				
		private Label			info_lab;
		private Label			info_lab2;
		private StyledText		json_area;
		
		protected
		subscriptionView(
			Subscription		_subs )
		{
			subs = _subs;
		}
		
		public void 
		initialize(
			Composite _parent_composite )
		{  
			parent_composite	= _parent_composite;
			
			composite = new Composite( parent_composite, SWT.NULL );
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData grid_data = new GridData(GridData.FILL_BOTH );
			composite.setLayoutData(grid_data);


				// control area
			
			final Composite controls = new Composite(composite, SWT.NONE);
			layout = new GridLayout();
			layout.numColumns = 5;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			controls.setLayout(layout);
			grid_data = new GridData(GridData.FILL_HORIZONTAL );
			controls.setLayoutData(grid_data);
			
			info_lab = new Label( controls, SWT.NULL );
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalIndent = 4;
			info_lab.setLayoutData(grid_data);

				
		
			final Button delete_button = new Button( controls, SWT.NULL );
			delete_button.setText( "Delete" );
						
			delete_button.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent e )
					{
						subs.remove();
					}
				});

			final Button save_button = new Button( controls, SWT.NULL );
			save_button.setText( "Save" );
					
			
			json_area = new StyledText(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			grid_data = new GridData(GridData.FILL_BOTH);
			grid_data.horizontalSpan = 1;
			grid_data.horizontalIndent = 4;
			json_area.setLayoutData(grid_data);
			json_area.setIndent( 4 );

			save_button.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent event )
					{
						try{
							subs.setJSON( json_area.getText());
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				});
			
			subs.addListener(
				new SubscriptionListener()
				{
					public void 
					subscriptionChanged(
						Subscription subs ) 
					{
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									updateInfo();
								}
							});
					}
				});
			
			final Button browse_button = new Button( controls, SWT.NULL );
			browse_button.setText( "Browser" );
						
			browse_button.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent e )
					{
						String url = com.aelitis.azureus.util.Constants.URL_PREFIX + "xsearch/index.html?subscription=" + subs.getID() + "&" + com.aelitis.azureus.util.Constants.URL_SUFFIX;
						
						BrowserWindow browser = new BrowserWindow( 
							UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell(),
							url,
							600, 800, true, false );
						
						browser.getContext().addMessageListener(
							new MetaSearchListener( null ));
					}
				});
			
			final Button download_button = new Button( controls, SWT.NULL );
			download_button.setText( "Download" );
						
			download_button.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent event )
					{
						try{
							subs.getManager().getScheduler().download( subs );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				});
			
			info_lab2 = new Label( controls, SWT.NULL );
			
			grid_data = new GridData(GridData.FILL_HORIZONTAL);
			grid_data.horizontalIndent = 4;
			info_lab2.setLayoutData(grid_data);

			updateInfo();
		}
		  
		protected void
		updateInfo()
		{
			info_lab.setText( 
					"ID=" + subs.getID() +
					", version=" + subs.getVersion() +
					", subscribed=" + subs.isSubscribed() +
					", public=" + subs.isPublic() +
					", mine=" + subs.isMine() +
					", popularity=" + subs.getCachedPopularity() +
					", associations=" + subs.getAssociationCount());
			
			SubscriptionHistory history = subs.getHistory();
			
			info_lab2.setText( 
					"History: " + 
					"enabled=" + history.isEnabled() +
					", scan=" + new SimpleDateFormat().format(new Date( history.getLastScanTime())) +
					", last_new=" + new SimpleDateFormat().format(new Date( history.getLastNewResultTime())) +
					", read=" + history.getNumRead() +
					" ,unread=" + history.getNumUnread());
					
			try{
			
				json_area.setText( subs.getJSON());
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		public Composite 
		getComposite()
		{ 
			return( composite );
		}
		
		public String 
		getFullTitle() 
		{
			return( subs.getName());
		}
	}
	
	protected static class
	sideBarItem
	{
		private TreeItem	tree_item;
		private boolean		destroyed;
		
		protected
		sideBarItem()
		{
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item )
		{
			tree_item	= _tree_item;
		}
		
		protected TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		protected void
		destroy()
		{
			destroyed = true;
		}
	}
}
