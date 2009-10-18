/*
 * Created on Jan 27, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;

public class 
DeviceManagerImpl 
	implements DeviceManager, DeviceOfflineDownloaderManager, AEDiagnosticsEvidenceGenerator
{
	private static final String	LOGGER_NAME 				= "Devices";
	private static final String	CONFIG_FILE 				= "devices.config";
	private static final String	AUTO_SEARCH_CONFIG_KEY		= "devices.config.auto_search";
	
	private static final String	RSS_ENABLE_CONFIG_KEY		= "devices.config.rss_enable";
	
	private static final String OD_ENABLED_CONFIG_KEY			= "devices.config.od.enabled";
	private static final String OD_IS_AUTO_CONFIG_KEY			= "devices.config.od.auto";
	private static final String OD_INCLUDE_PRIVATE_CONFIG_KEY	= "devices.config.od.inc_priv";
	
	
	private static final String CONFIG_DEFAULT_WORK_DIR	= "devices.config.def_work_dir";
	
	
	protected static final int	DEVICE_UPDATE_PERIOD	= 5*1000;
	
	private static DeviceManagerImpl		singleton;
	
	public static void
	preInitialise()
	{
	}
	
	public static DeviceManager
	getSingleton()
	{
		synchronized( DeviceManagerImpl.class ){
			
			if ( singleton == null ){
				
				singleton = new DeviceManagerImpl();
			}
		}
		
		return( singleton );
	}
	
	
	private AzureusCore		azureus_core;
	
	private TorrentAttribute			od_manual_ta;
	
	private List<DeviceImpl>			device_list = new ArrayList<DeviceImpl>();
	private Map<String,DeviceImpl>		device_map	= new HashMap<String, DeviceImpl>();
	
	private DeviceTivoManager		tivo_manager;
	private DeviceManagerUPnPImpl	upnp_manager;
	private DeviceDriveManager		drive_manager;
		
		// have to go async on this as there are situations where we end up firing listeners
		// while holding monitors and this can result in deadlock if sync
	
	private static final int LT_DEVICE_ADDED		= 1;
	private static final int LT_DEVICE_CHANGED		= 2;
	private static final int LT_DEVICE_ATTENTION	= 3;
	private static final int LT_DEVICE_REMOVED		= 4;
	private static final int LT_INITIALIZED			= 5;
	
	private ListenerManager<DeviceManagerListener>	listeners = 
		ListenerManager.createAsyncManager(
				"DM:ld",
				new ListenerManagerDispatcher<DeviceManagerListener>()
				{
					public void 
					dispatch(
						DeviceManagerListener 		listener, 
						int 						type, 
						Object 						value ) 
					{
						DeviceImpl	device = (DeviceImpl)value;
						
						switch( type ){
						
							case LT_DEVICE_ADDED:{
								
								listener.deviceAdded( device );
								
								break;
							}
							case LT_DEVICE_CHANGED:{
								
								if ( deviceAdded( device )){
								
									device.fireChanged();
									
									listener.deviceChanged( device );
								}
								
								break;
							}
							case LT_DEVICE_ATTENTION:{
								
								if ( deviceAdded( device )){
								
									listener.deviceAttentionRequest( device );
								}
								
								break;
							}
							case LT_DEVICE_REMOVED:{
								
								listener.deviceRemoved( device );
								
								break;
							}
							case LT_INITIALIZED:{
								
								listener.deviceManagerLoaded();
								
								break;
							}
						}
					}
					
					protected boolean
					deviceAdded(
						Device		device )
					{
						synchronized( DeviceManagerImpl.this ){
							
							return( device_list.contains( device ));
						}
					}
				});
	
	
	private boolean	auto_search;
	
	private DeviceManagerRSSFeed	rss_publisher;
	
	private boolean	od_enabled;
	private boolean	od_is_auto;
	private boolean od_include_private;
	
	
	private boolean	closing;
	
	private boolean	config_unclean;
	private boolean	config_dirty;
	
	private int		explicit_search;
	
	private TranscodeManagerImpl	transcode_manager;
	
	private int						getMimeType_fails;
	
	private AEDiagnosticsLogger		logger;
	
	private AsyncDispatcher	async_dispatcher = new AsyncDispatcher( 10*1000 );

	private volatile boolean initialized = false;
	
	protected
	DeviceManagerImpl()
	{
		AEDiagnostics.addEvidenceGenerator( this );

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				initWithCore(core);
			}
		});
	}
	
	private void 
	initWithCore(
		final AzureusCore core ) 
	{
		azureus_core = core;
		
		od_manual_ta = PluginInitializer.getDefaultInterface().getTorrentManager().getPluginAttribute( "device.manager.od.ta.manual" );
		
		rss_publisher = new DeviceManagerRSSFeed( this );

			// need to pick up auto-search early on
		
		COConfigurationManager.addAndFireParameterListeners(
				new String[]{
					AUTO_SEARCH_CONFIG_KEY,
				},
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name ) 
					{
						auto_search = COConfigurationManager.getBooleanParameter( AUTO_SEARCH_CONFIG_KEY, true );
					}
				});
		
		COConfigurationManager.addAndFireParameterListeners(
				new String[]{
					OD_ENABLED_CONFIG_KEY,
					OD_IS_AUTO_CONFIG_KEY,
					OD_INCLUDE_PRIVATE_CONFIG_KEY
				},
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name ) 
					{
						boolean	new_od_enabled 				= COConfigurationManager.getBooleanParameter( OD_ENABLED_CONFIG_KEY, true );
						boolean	new_od_is_auto 				= COConfigurationManager.getBooleanParameter( OD_IS_AUTO_CONFIG_KEY, true );
						boolean	new_od_include_private_priv	= COConfigurationManager.getBooleanParameter( OD_INCLUDE_PRIVATE_CONFIG_KEY, false );
						
						if ( new_od_enabled != od_enabled || new_od_is_auto != od_is_auto || new_od_include_private_priv != od_include_private ){
							
							od_enabled			= new_od_enabled;
							od_is_auto			= new_od_is_auto;
							od_include_private	= new_od_include_private_priv;
							
							manageOD();
						}
					}
				});
		
			// init tivo before upnp as upnp init completion starts up tivo
		
		tivo_manager = new DeviceTivoManager( this );

		upnp_manager = new DeviceManagerUPnPImpl( this );

		loadConfig();
				
		new DeviceiTunesManager( this );
				
		drive_manager = new DeviceDriveManager( this );
		
		transcode_manager = new TranscodeManagerImpl( this );
		
		core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				stopping(
					AzureusCore		core )
				{					
					synchronized( DeviceManagerImpl.this ){
				
						if ( config_dirty || config_unclean ){
							
							saveConfig();
						}
						
						closing	= true;
						
						transcode_manager.close();
						
						DeviceImpl[] devices = getDevices();
						
						for ( DeviceImpl device: devices ){
							
							device.close();
						}
					}
				}
			});
		
		upnp_manager.initialise();
		
		SimpleTimer.addPeriodicEvent(
				"DeviceManager:update",
				DEVICE_UPDATE_PERIOD,
				new TimerEventPerformer()
				{
					private int tick_count = 0;
					
					public void 
					perform(
						TimerEvent event ) 
					{
						List<DeviceImpl> copy;
						
						tick_count++;
						
						transcode_manager.updateStatus( tick_count );
						
						synchronized( DeviceManagerImpl.this ){

							if( device_list.size() == 0 ){
								
								return;
							}
							
							copy = new ArrayList<DeviceImpl>( device_list );
						}
						
						for ( DeviceImpl device: copy ){
							
							device.updateStatus( tick_count );
						}
					}
				});
		
		initialized = true;
		
		listeners.dispatch( LT_INITIALIZED, null );
	}
	
	protected void
	manageOD()
	{
		DeviceImpl[] devices = getDevices();
		
		for ( DeviceImpl device: devices ){
			
			if ( device.getType() == Device.DT_OFFLINE_DOWNLOADER ){
				
				((DeviceOfflineDownloaderImpl)device).checkConfig();
			}
		}
	}
	
	protected void
	UPnPManagerStarted()
	{
		tivo_manager.startUp();
		
		DeviceImpl[] devices = getDevices();
		
		for ( DeviceImpl device: devices ){
			
			if ( device instanceof DeviceUPnPImpl ){
				
				((DeviceUPnPImpl)device).UPnPInitialised();
			}
		}
	}
	
	protected AzureusCore
	getAzureusCore()
	{
		return( azureus_core );
	}
	
	protected DeviceManagerUPnPImpl 
	getUPnPManager()
	{
		return( upnp_manager );
	}
	
	public boolean
	isTiVoEnabled()
	{
		return( tivo_manager.isEnabled());
	}
	
	public void
	setTiVoEnabled(
		boolean	enabled )
	{
		tivo_manager.setEnabled( enabled );
	}
	
	public DeviceTemplate[] 
	getDeviceTemplates(
		int		device_type )
	{
		if ( transcode_manager == null || device_type != Device.DT_MEDIA_RENDERER ){
			
			return( new DeviceTemplate[0] );
		}
		
		TranscodeProvider[] providers = transcode_manager.getProviders();
		
		List<DeviceTemplate> result = new ArrayList<DeviceTemplate>();
		
		for ( TranscodeProvider provider: providers ){
			
			TranscodeProfile[] profiles = provider.getProfiles();
			
			Map<String,DeviceMediaRendererTemplateImpl>	class_map = new HashMap<String, DeviceMediaRendererTemplateImpl>();
			
			for ( TranscodeProfile profile: profiles ){
				
				String	classification = profile.getDeviceClassification();
				
				if ( classification.startsWith( "apple." )){
					
					classification = "apple.";
				}
				
				boolean	auto = 
					classification.equals( "sony.PS3" ) ||
					classification.equals( "microsoft.XBox" ) ||
					classification.equals( "apple." ) ||
					classification.equals( "nintendo.Wii" ) ||
					classification.equals( "browser.generic" );
				
				DeviceMediaRendererTemplateImpl temp = class_map.get( classification );
				
				if ( temp == null ){
					
					temp = new DeviceMediaRendererTemplateImpl( this, classification, auto );
					
					class_map.put( classification, temp );
					
					result.add( temp );
				}
				
				temp.addProfile( profile );
			}
		}
		
		return( result.toArray( new DeviceTemplate[ result.size() ]));
	}
	
	public DeviceManufacturer[] 
  	getDeviceManufacturers(
  		int		device_type )
	{
		DeviceTemplate[] templates = getDeviceTemplates( device_type );
		
		Map<String,DeviceManufacturerImpl>	map = new HashMap<String, DeviceManufacturerImpl>();
		
		for ( DeviceTemplate template: templates ){
			
			if ( template.getType() != device_type ){
				
				continue;
			}
			
			String	man_str = template.getManufacturer();
			
			DeviceManufacturerImpl man = map.get( man_str );
			
			if ( man == null ){
				
				man = new DeviceManufacturerImpl( man_str );
				
				map.put( man_str, man );
			}
			
			man.addTemplate( template );
		}
		
		return( map.values().toArray( new DeviceManufacturer[ map.size() ] ));
	}
	
	protected Device
	createDevice(
		int						device_type,
		String					classification,
		String					name )
	
		throws DeviceManagerException
	{
		if ( device_type == Device.DT_MEDIA_RENDERER ){
			
			DeviceImpl res = new DeviceMediaRendererManual( this, classification, true, name );
			
			addDevice( res );
			
			return( res );
			
		}else{
			
			throw( new DeviceManagerException( "Can't manually create this device type" ));
		}
	}
	
	public void
	search(
		final int					millis,
		final DeviceSearchListener	listener )
	{
		new AEThread2( "DM:search", true )
		{
			public void
			run()
			{
				synchronized( DeviceManagerImpl.this ){
				
					explicit_search++;
				}
				
				tivo_manager.search();
				
				drive_manager.search();
				
				AESemaphore	sem = new AESemaphore( "DM:search" );
				
				DeviceManagerListener	dm_listener =
					new DeviceManagerListener()
					{
						public void
						deviceAdded(
							Device		device )
						{
							listener.deviceFound( device );
						}
						
						public void
						deviceChanged(
							Device		device )
						{
						}
						
						public void
						deviceAttentionRequest(
							Device		device )
						{	
						}
						
						public void
						deviceRemoved(
							Device		device )
						{
						}

						public void 
						deviceManagerLoaded() {
						}
					};
					
				try{
					addListener( dm_listener );
				
					upnp_manager.search();
					
					sem.reserve( millis );
					
				}finally{
					
					synchronized( DeviceManagerImpl.this ){
						
						explicit_search--;
					}
					
					removeListener( dm_listener );
					
					listener.complete();
				}
			}
		}.start();
	}
	
	protected DeviceImpl
	getDevice(
		String		id )
	{
		synchronized( this ){

			return( device_map.get( id ));
		}
	}
	
	protected DeviceImpl
	addDevice(
		DeviceImpl		device )
	{
		return( addDevice( device, true ));
	}
	
	protected DeviceImpl
	addDevice(
		DeviceImpl		device,
		boolean			is_alive )
	{
			// for xbox (currently) we automagically replace a manual entry with an auto one as we may have
			// added the manual one when receiving a previous browse before getting the UPnP renderer details
			
		DeviceImpl	existing = null;
		
		synchronized( this ){
						
			existing = device_map.get( device.getID());
			
			if ( existing != null ){
				
				existing.updateFrom( device, is_alive );
												
			}else{
			
				if ( device.getType() == Device.DT_MEDIA_RENDERER ){
					
					DeviceMediaRenderer renderer = (DeviceMediaRenderer)device;
					
					if ( renderer.getRendererSpecies() == DeviceMediaRenderer.RS_XBOX && !renderer.isManual()){
						
						for ( DeviceImpl d: device_list ){
							
							if ( d.getType() == Device.DT_MEDIA_RENDERER ){
								
								DeviceMediaRenderer r = (DeviceMediaRenderer)d;
								
								if ( r.getRendererSpecies() == DeviceMediaRenderer.RS_XBOX && r.isManual()){
									
									existing = d;

									log( "Merging " + device.getString() + " -> " + existing.getString());
										
									String	secondary_id = device.getID();
									
									existing.setSecondaryID( secondary_id );
									
									existing.updateFrom( device, is_alive );
								}
							}
						}
					}
				}
			}
			
			if ( existing == null ){
			
				device_list.add( device );
				
				device_map.put( device.getID(), device );
			}
		}
		
		if ( existing != null ){
			
				// don't trigger config save here, if anything has changed it will have been handled
				// by the updateFrom call above
			
			// if anything has changed then the updateFrom methods should have indicated this
			// so there's no need to blindly fire a change event here
			// deviceChanged( existing, false );
			
			return( existing );
		}
					
		device.initialise();
		
		if ( is_alive ){
		
			device.alive();
		}
		
		deviceAdded( device );
		
		configDirty();
		
		return( device );
	}
	
	protected void
	removeDevice(
		DeviceImpl		device )
	{
		synchronized( this ){
			
			DeviceImpl existing = device_map.remove( device.getID());
			
			if ( existing == null ){
				
				return;
			}
			
			device_list.remove( device );
			
			String secondary_id = device.getSecondaryID();
			
			if ( secondary_id != null ){
				
				device_map.remove( secondary_id );
			}
		}
		
		device.destroy();
		
		deviceRemoved( device );
		
		configDirty();
	}

	public boolean
	isBusy()
	{
		if ( getTranscodeManager().getQueue().isTranscoding()){
			
			return( true );
		}
		
		synchronized( this ){
			
			for ( DeviceImpl device: device_list ){
				
				if ( device.isBusy()){
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	public DeviceImpl[]
  	getDevices()
	{
		synchronized( this ){
			
			return( device_list.toArray( new DeviceImpl[ device_list.size()] ));
		}
	}
  		
	public boolean
	getAutoSearch()
	{
		return( auto_search );
	}
	
	public void
	setAutoSearch(
		boolean	auto )
	{
		COConfigurationManager.setParameter( AUTO_SEARCH_CONFIG_KEY, auto );
	}
	
	public boolean
	isRSSPublishEnabled()
	{
		return( COConfigurationManager.getBooleanParameter( RSS_ENABLE_CONFIG_KEY, false ) );
	}
	
	public void
	setRSSPublishEnabled(
		boolean		enabled )
	{
		COConfigurationManager.setParameter( RSS_ENABLE_CONFIG_KEY, enabled );
	}
	
	public String
	getRSSLink()
	{
		return( rss_publisher.getFeedURL());
	}
	
		// offline downloader stuff
	
	public DeviceOfflineDownloaderManager
	getOfflineDownlaoderManager()
	{
		return( this );
	}
	
	public boolean
	isOfflineDownloadingEnabled()
	{
		return( od_enabled );
	}
	
	public void
	setOfflineDownloadingEnabled(
		boolean		enabled )
	{
		COConfigurationManager.setParameter( OD_ENABLED_CONFIG_KEY, enabled );
	}

	public boolean
	getOfflineDownloadingIsAuto()
	{
		return( od_is_auto );
	}
	
	public void
	setOfflineDownloadingIsAuto(
		boolean		auto )
	{
		COConfigurationManager.setParameter( OD_IS_AUTO_CONFIG_KEY, auto );
	}

	public boolean
	getOfflineDownloadingIncludePrivate()
	{
		return( od_include_private );	
	}
	
	public void
	setOfflineDownloadingIncludePrivate(
		boolean		include )
	{
		COConfigurationManager.setParameter( OD_INCLUDE_PRIVATE_CONFIG_KEY, include );
	}
	
	public boolean
	isManualDownload(
		Download		download )
	{
		return( download.getBooleanAttribute( od_manual_ta ));
	}
	
	public void
	addManualDownloads(
		Download[]		downloads )
	{
		for ( Download d: downloads ){
			
			d.setBooleanAttribute( od_manual_ta, true );
		}
		
		manageOD();
	}

	public void
	removeManualDownloads(
		Download[]		downloads )
	{
		for ( Download d: downloads ){
			
			d.setBooleanAttribute( od_manual_ta, false );
		}
		
		manageOD();
	}
	
		// sdsd
	
	protected boolean
	isExplicitSearch()
	{
		synchronized( this ){
			
			return( explicit_search > 0 );
		}
	}
	
	protected boolean
	isClosing()
	{
		return( closing );
	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
				
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_devices = (List)map.get( "devices" );
			
			if ( l_devices != null ){
				
				for (int i=0;i<l_devices.size();i++){
					
					Map	m = (Map)l_devices.get(i);
					
					try{
						DeviceImpl device = DeviceImpl.importFromBEncodedMapStatic(this,  m );
						
						device_list.add( device );
						
						device_map.put( device.getID(), device );
						
						String secondary_id = device.getSecondaryID();
						
						if ( secondary_id != null ){
							
							device_map.put( secondary_id, device );
						}
							
						device.initialise();
					
						log( "    loaded " + device.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import subscription from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	configDirty(
		DeviceImpl		device,
		boolean			save_changes )
	{
		deviceChanged( device, save_changes );
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"Subscriptions:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( DeviceManagerImpl.this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			if ( closing ){
				
					// to late to try writing
				
				return;
			}
			
			config_dirty 	= false;
			config_unclean	= false;
			
			if ( device_list.size() == 0 ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
				
				Map map = new HashMap();
				
				List	l_devices = new ArrayList();
				
				map.put( "devices", l_devices );
				
				Iterator<DeviceImpl>	it = device_list.iterator();
				
				while( it.hasNext()){
					
					DeviceImpl device = it.next();
						
					try{
						Map d = new HashMap();
						
						device.exportToBEncodedMap( d );
						
						l_devices.add( d );
						
					}catch( Throwable e ){
						
						log( "Failed to save device " + device.getString(), e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected void
	deviceAdded(
		DeviceImpl		device )
	{
		configDirty();
		
		// I'd rather put this in a listener, but for now this will ensure
		// it gets QOS'd even before any listeners are added
		
		try{
			PlatformDevicesMessenger.qosFoundDevice(device);
			
		}catch( Throwable e ){
			
			Debug.out(e);
		}
		
		listeners.dispatch( LT_DEVICE_ADDED, device );
	}
	
	
	protected void
	deviceChanged(
		DeviceImpl		device,
		boolean			save_changes )
	{
		if ( save_changes ){
			
			configDirty();
			
		}else{
			
			config_unclean = true;
		}
				
		listeners.dispatch( LT_DEVICE_CHANGED, device );
	}
	
	protected void
	deviceRemoved(
		DeviceImpl		device )
	{
		configDirty();
		
		listeners.dispatch( LT_DEVICE_REMOVED, device );
	}
	
	protected void
	requestAttention(
		DeviceImpl		device )
	{
		listeners.dispatch( LT_DEVICE_ATTENTION, device );
	}
	
	protected URL
	getStreamURL(
		TranscodeFileImpl		file,
		String					host )
	{
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc != null ){

			try{
				DiskManagerFileInfo f = file.getTargetFile();
				
				String str = (String)ipc.invoke( "getContentURL", new Object[]{ f });
				
				if ( str != null && str.length() > 0 ){
					
					if ( host != null ){
						
						str = str.replace( "127.0.0.1", host );
					}
					
					return( new URL( str ));
				}
			}catch( Throwable e ){
				
			}
		}
		
		return( null );
	}
	
	protected String
	getMimeType(
		TranscodeFileImpl		file )
	{
		if ( getMimeType_fails > 5 ){
			
			return( null );
		}
		
		IPCInterface ipc = upnp_manager.getUPnPAVIPC();
		
		if ( ipc != null ){

			try{
				DiskManagerFileInfo f = file.getTargetFile();
				
				String str = (String)ipc.invoke( "getMimeType", new Object[]{ f });
				
				if ( str != null && str.length() > 0 ){
					
					return( str );
				}
			}catch( Throwable e ){
				
				getMimeType_fails++;
				
				e.printStackTrace();
			}
		}
		
		return( null );
	}
	
	public File
	getDefaultWorkingDirectory()
	{
		return( getDefaultWorkingDirectory( false ));
	}
	
	public File
	getDefaultWorkingDirectory(
		boolean		persist )
	{
		String def = COConfigurationManager.getStringParameter( CONFIG_DEFAULT_WORK_DIR, "" ).trim();
		
		if ( def.length() == 0 ){
			
			String	def_dir = COConfigurationManager.getStringParameter( "Default save path" );

			def = def_dir + File.separator + "transcodes";
		}
		
		File f = new File( def );
		
		if ( !f.exists()){
		
			if ( persist ){
			
				f.mkdirs();
			}
		}
		
		return( f );
	}
	
	public void
	setDefaultWorkingDirectory(
		File		dir )
	{
		File existing = getDefaultWorkingDirectory( false );
		
		if ( !existing.getAbsolutePath().equals( dir.getAbsolutePath())){
		
			// default has changed, reset all device save locations so that they pick up the change
			
			
			synchronized( this ){
				
				for ( DeviceImpl d: device_list ){

					d.resetWorkingDirectory();
				}
			}
		}
		
		COConfigurationManager.setParameter( CONFIG_DEFAULT_WORK_DIR, dir.getAbsolutePath());
	}
	
	public TranscodeManagerImpl
	getTranscodeManager()
	{
		return( transcode_manager );
	}
	
	public UnassociatedDevice[]
	getUnassociatedDevices()
	{
		return( upnp_manager.getUnassociatedDevices());
	}
	
  	public void
  	addListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.addListener( listener );
  		
  		if (initialized) {

  			listeners.dispatch( listener, LT_INITIALIZED, null );
  		}
  	}
  	
  	public void
  	removeListener(
  		DeviceManagerListener		listener )
  	{
  		listeners.removeListener( listener );
  	}
  	
	protected synchronized AEDiagnosticsLogger
	getLogger()
	{
		if ( logger == null ){
			
			logger = AEDiagnostics.getLogger( LOGGER_NAME );
		}
		
		return( logger );
	}
	
	public void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
		diag_logger.log( e );
	}
	
	public void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
	}
 	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Devices" );
			
		try{
			writer.indent();
			
			DeviceImpl[] devices = getDevices();
			
			for ( DeviceImpl device: devices ){
				
				device.generate( writer );
			}
			
			if ( transcode_manager != null ){
			
				transcode_manager.generate( writer );
			}
		}finally{
			
			writer.exdent();
		}
	}
	
	protected static class
	DeviceManufacturerImpl
		implements DeviceManufacturer
	{
		private String 	name;
		
		private List<DeviceTemplate>	templates = new ArrayList<DeviceTemplate>();
		
		protected
		DeviceManufacturerImpl(
			String		_name )
		{
			name	= _name;
		}
		
		protected void
		addTemplate(
			DeviceTemplate	t )
		{
			templates.add( t );
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		public DeviceTemplate[]
		getDeviceTemplates()
		{
			return( templates.toArray( new DeviceTemplate[ templates.size()] ));
		}
	}
}
