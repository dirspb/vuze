/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.aelitis.azureus.ui.swt.subscriptions;

import java.net.URL;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.TableColumn;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.subs.SubscriptionUtils.SubscriptionDownloadDetails;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class SubscriptionWizard {
	
	private static final int MODE_OPT_IN = 1;
	private static final int MODE_SUBSCRIBE = 2;
	private static final int MODE_CREATE_SEARCH = 3;
	private static final int MODE_CREATE_RSS = 4;
	
	private static final int RANK_COLUMN_WIDTH = 85;
	private static final String TABLE_SUB_WIZ = "SubscriptionWizard";
	
	private final String TITLE_OPT_IN = MessageText.getString("Wizard.Subscription.optin.title");
	private final String TITLE_SUBSCRIBE = MessageText.getString("Wizard.Subscription.subscribe.title");
	private final String TITLE_CREATE = MessageText.getString("Wizard.Subscription.create.title");
	
	Display display;
	Shell shell;
	
	Image rankingBars;
	Color rankingBorderColor;
	
	Label title;
	
	Button cancelButton;
	Button searchButton;
	Button saveButton;
	Button yesButton;
	Button addButton;
	Button availableButton;
	Button createButton;
	
	Font boldFont;
	Font titleFont;
	Font subTitleFont;
	Font textInputFont;
	
	
	Composite main;
	StackLayout mainLayout;
	Composite optinComposite;
	Composite createComposite;
	TabFolder createTabFolder;
	TabItem   createRSSTabItem;
	TabItem   createSearchTabItem;
	Composite availableSubscriptionComposite;
	
	Table libraryTable;
	Listener rssSaveListener;
	Listener searchListener;
	
	Text searchInput;
	Text feedUrl;
	
	
	SubscriptionDownloadDetails[] availableSubscriptions;
	Subscription[] subscriptions;
	
	DownloadManager download;
	URL				rss_feed_url;
	
	private ImageLoader imageLoader;
	private TableViewSWT<Subscription> tvSubscriptions;
	private static boolean columnsAdded = false;
	
	public SubscriptionWizard() {
		init();
	}
	
	public 
	SubscriptionWizard(
		URL		url )
	{
		rss_feed_url	= url;
		
		init();
	}
	
	public SubscriptionWizard(
		DownloadManager _download) 
	{
		download = _download;

		init();
	}
	
	protected void 
	init()
	{
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				init(core);
			}
		});
	}

	protected void init(AzureusCore core) {
		imageLoader = ImageLoader.getInstance();
		
		/*SubscriptionDownloadDetails[] allSubscriptions = SubscriptionUtils.getAllCachedDownloadDetails();
		List notYetSubscribed = new ArrayList(allSubscriptions.length);
		for(int i = 0 ; i < allSubscriptions.length ; i++) {
			Subscription[] subs = allSubscriptions[i].getSubscriptions();
			boolean subscribedToAll = true;
			for(int j = 0 ; j < subs.length ; j++) {
				subscribedToAll = subscribedToAll && subs[j].isSubscribed();
			}
			if(!subscribedToAll) {
				notYetSubscribed.add(allSubscriptions[i]);
			}
		}
		availableSubscriptions = (SubscriptionDownloadDetails[]) notYetSubscribed.toArray(new SubscriptionDownloadDetails[notYetSubscribed.size()]);*/
		availableSubscriptions = SubscriptionUtils.getAllCachedDownloadDetails(core);
		Arrays.sort(availableSubscriptions,new Comparator<SubscriptionDownloadDetails>() {
			public int compare(SubscriptionDownloadDetails o1, SubscriptionDownloadDetails o2) {
				if (o1 == null || o2 == null) return 0;
				return o1.getDownload().getDisplayName().compareTo(o2.getDownload().getDisplayName());
			}
		});
		
		
		shell = ShellFactory.createMainShell(SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		shell.setSize(650,400);
		Utils.centreWindow(shell);
		
		shell.setMinimumSize(550,400);
		
		display = shell.getDisplay();
		
		Utils.setShellIcon(shell);
		
		rankingBars = imageLoader.getImage("ranking_bars");
		rankingBorderColor = new Color(display,200,200,200);
		
		createFonts();
		
		shell.setText(MessageText.getString("Wizard.Subscription.title"));
		
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				imageLoader.releaseImage("ranking_bars");
				imageLoader.releaseImage("wizard_header_bg");
				imageLoader.releaseImage("icon_rss");
				
				if(titleFont != null && !titleFont.isDisposed()) {
					titleFont.dispose();
				}
				
				if(textInputFont != null && !textInputFont.isDisposed()) {
					textInputFont.dispose();
				}
				
				if(boldFont != null && !boldFont.isDisposed()) {
					boldFont.dispose();
				}
				
				if(subTitleFont != null && !subTitleFont.isDisposed()) {
					subTitleFont.dispose();
				}
				
				if(rankingBorderColor != null && !rankingBorderColor.isDisposed()) {
					rankingBorderColor.dispose();
				}
				
			}
		});
		
		Composite header = new Composite(shell, SWT.NONE);
		header.setBackgroundMode(SWT.INHERIT_DEFAULT);
		header.setBackgroundImage(imageLoader.getImage("wizard_header_bg"));
		Label topSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		main = new Composite(shell, SWT.NONE);
		Label bottomSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		Composite footer = new Composite(shell, SWT.NONE);
		
		FormLayout layout = new FormLayout();
		shell.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		//data.height = 50;
		header.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(header,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		topSeparator.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(topSeparator,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(bottomSeparator,0);
		main.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(footer,0);
		bottomSeparator.setLayoutData(data);
		
		data = new FormData();
		data.bottom = new FormAttachment(100,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		//data.height = 100;
		footer.setLayoutData(data);
		
		populateHeader(header);
		populateFooter(footer);
		
		mainLayout = new StackLayout();
		main.setLayout(mainLayout);
		
		optinComposite = createOptInComposite(main);
		createComposite = createCreateComposite(main);
		availableSubscriptionComposite = createAvailableSubscriptionComposite(main);
		
		
		setDefaultAvailableMode();
		
		shell.layout();
		shell.open();
		
		setInitialViews();
	}
	
	protected void
	setInitialViews()
	{
		if ( availableSubscriptions != null ){
			
			for (int i=0;i<availableSubscriptions.length;i++){
				
				SubscriptionDownloadDetails details = availableSubscriptions[i];
				
				if ( details.getDownload()== download ){
						
					final int f_i = i;
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void 
							run() 
							{	
								libraryTable.setTopIndex( f_i );
							}
						});
				}
			}
		}
	}
	private void populateHeader(Composite header) {
		header.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		title = new Label(header, SWT.WRAP);
		
		title.setFont(titleFont);
		
		FillLayout layout = new FillLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		header.setLayout(layout);
		
	}
	
	private Composite createOptInComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		composite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		Label description = new Label(composite,SWT.WRAP);
		description.setFont(boldFont);
		description.setText(MessageText.getString("Wizard.Subscription.optin.description"));
		
		/*Label descLibraryIcon = new Label(composite, SWT.NONE);
		descLibraryIcon.setImage(ImageRepository.getImage("btn_rss_add"));
		
		Label descLibraryText = new Label(composite, SWT.NONE);
		descLibraryText.setText(MessageText.getString("Wizard.Subscription.optin.description.library"));
		
		Label descSidebarIcon = new Label(composite, SWT.NONE);
		descSidebarIcon.setImage(ImageRepository.getImage("btn_sidebar_add"));
		
		Label descSidebarText = new Label(composite, SWT.NONE);
		descSidebarText.setText(MessageText.getString("Wizard.Subscription.optin.description.sidebar"));
		
		Label help = new Label(composite, SWT.NONE);
		help.setFont(boldFont);
		help.setText(MessageText.getString("Wizard.Subscription.optin.help"));*/
		
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0,40);
		data.left = new FormAttachment(0,50);
		data.right= new FormAttachment(100,-50);
		description.setLayoutData(data);
		
		/*data = new FormData();
		data.top = new FormAttachment(description,10);
		data.left = new FormAttachment(0,50);
		descLibraryIcon.setLayoutData(data);
		
		data = new FormData();
		//data.top = new FormAttachment(description,10);
		data.left = new FormAttachment(descLibraryIcon,10);
		data.right= new FormAttachment(100,-50);
		data.bottom= new FormAttachment(descLibraryIcon,-3,SWT.BOTTOM);
		descLibraryText.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(descLibraryText,10);
		//data.left = new FormAttachment(descLibraryIcon,-10,SWT.CENTER);
		data.left = new FormAttachment(0,50);
		descSidebarIcon.setLayoutData(data);
		
		data = new FormData();
		//data.top = new FormAttachment(descLibraryText,10);
		data.left = new FormAttachment(descLibraryText,0,SWT.LEFT);
		data.right= new FormAttachment(100);
		data.bottom= new FormAttachment(descSidebarIcon,-3,SWT.BOTTOM);
		descSidebarText.setLayoutData(data);
		
		data = new FormData();
		data.right= new FormAttachment(100,-20);
		data.bottom= new FormAttachment(100,-10);
		help.setLayoutData(data);*/
		
		return composite;
	}
	
	private Composite createCreateComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		FillLayout layout = new FillLayout();
		layout.marginHeight = 8;
		layout.marginWidth  = 8;
		
		composite.setLayout(layout);
		
		createTabFolder = new TabFolder(composite,SWT.NONE);
		createTabFolder.setFont(subTitleFont);
		
		createSearchTabItem = new TabItem(createTabFolder,SWT.NONE);
		createSearchTabItem.setText(MessageText.getString("Wizard.Subscription.create.search"));
		createSearchTabItem.setControl(createCreateSearchComposite(createTabFolder));
		
		createRSSTabItem = new TabItem(createTabFolder,SWT.NONE);
		createRSSTabItem.setText("  " + MessageText.getString("Wizard.Subscription.create.rss"));
		createRSSTabItem.setControl(createCreateRSSComposite(createTabFolder));
		
		createTabFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TabItem[] selectedItems = createTabFolder.getSelection();
				if(selectedItems.length != 1) {
					return;
				}
				TabItem selectedItem = selectedItems[0];
				if(selectedItem == createRSSTabItem) {
					setMode(MODE_CREATE_RSS);
				} else {
					setMode(MODE_CREATE_SEARCH);
				}
			}
		});
		
		return composite;
	}
	
	private Composite createCreateRSSComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);

		Label subTitle1 = new Label(composite,SWT.WRAP);
		subTitle1.setFont(subTitleFont);
		subTitle1.setText(MessageText.getString("Wizard.Subscription.rss.subtitle1"));
		
		feedUrl = new Text(composite, SWT.SINGLE);
		feedUrl.setFont(textInputFont);
		feedUrl.setText("http://");
//		feedUrl.setData("visited",new Boolean(false));
//		
//		feedUrl.addListener(SWT.FocusIn, new Listener() {
//			public void handleEvent(Event arg0) {
//				boolean visited = ((Boolean) feedUrl.getData("visited")).booleanValue();
//				if(visited) return;
//				feedUrl.setData("visited",new Boolean(true));
//				feedUrl.setText("");
//			}
//		});
		
		feedUrl.addListener (SWT.DefaultSelection, rssSaveListener);
		
		feedUrl.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				boolean valid_url = false;
				try {
					URL url = new URL(feedUrl.getText());
					String protocol = url.getProtocol().toLowerCase();
					valid_url = protocol.equals( "azplug" ) || protocol.equals( "file") || url.getHost().trim().length() > 0;
				} catch (Exception e) {}
				
				saveButton.setEnabled(valid_url);
			}
		});
		
		Label rssBackground = new Label(composite,SWT.NONE);
		imageLoader.setLabelImage(rssBackground, "rss_bg");
		int width = rssBackground.getImage().getBounds().width;
		
		Label subTitle2 = new Label(composite,SWT.WRAP);
		//subTitle2.setFont(subTitleFont);
		subTitle2.setText(MessageText.getString("Wizard.Subscription.rss.subtitle2"));

		Label rssBullet = new Label(composite, SWT.NONE);
		imageLoader.setLabelImage(rssBullet, "rss");

		Label subTitle3 = new Label(composite,SWT.WRAP);
		subTitle3.setFont(subTitleFont);
		subTitle3.setText(MessageText.getString("Wizard.Subscription.rss.subtitle3"));
		
		FormLayout layout = new FormLayout();
		layout.marginWidth = 50;
		layout.marginTop = 25;
		composite.setLayout(layout);
		
		FormData data;

		data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		subTitle1.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle1,5);
		data.left = new FormAttachment(50,-width/2);
		rssBackground.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(rssBackground,7,SWT.TOP);
		data.left = new FormAttachment(rssBackground, 45,SWT.LEFT);
		data.right = new FormAttachment(rssBackground, -8,SWT.RIGHT);
		feedUrl.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(rssBackground,15);
		data.left = new FormAttachment(0);
		rssBullet.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(rssBullet,-3,SWT.TOP);
		data.left = new FormAttachment(rssBullet,5);
		data.right = new FormAttachment(100);
		subTitle2.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle2,20);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		subTitle3.setLayoutData(data);


		return composite;
	}
	
	private Composite createCreateSearchComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		Label subTitle1 = new Label(composite,SWT.WRAP);
		subTitle1.setFont(subTitleFont);
		subTitle1.setText(MessageText.getString("Wizard.Subscription.search.subtitle1"));
		
		searchInput = new Text(composite, SWT.SINGLE);
		searchInput.setFont(textInputFont);
//		searchInput.setText(MessageText.getString("Wizard.Subscription.search.inputPrompt"));
//		searchInput.setData("visited",new Boolean(false));
//		
//		searchInput.addListener(SWT.FocusIn, new Listener() {
//			public void handleEvent(Event arg0) {
//				boolean visited = ((Boolean) searchInput.getData("visited")).booleanValue();
//				if(visited) return;
//				searchInput.setData("visited",new Boolean(true));
//				searchInput.setText("");
//			}
//		});
		
		searchInput.addListener (SWT.DefaultSelection, searchListener);
		
		Label searchBackground = new Label(composite,SWT.NONE);
		imageLoader.setLabelImage(searchBackground, "search_bg");
		int width = searchBackground.getImage().getBounds().width;
		
		Label subTitle2 = new Label(composite,SWT.WRAP);
		subTitle2.setFont(subTitleFont);
		subTitle2.setText(MessageText.getString("Wizard.Subscription.search.subtitle2"));

		Label checkBullet1 = new Label(composite, SWT.NONE);
		imageLoader.setLabelImage(checkBullet1, "icon_check");
		Label checkBullet2 = new Label(composite, SWT.NONE);
		imageLoader.setLabelImage(checkBullet2, "icon_check");

		Label description1 = new Label(composite,SWT.NONE);
		description1.setText(MessageText.getString("Wizard.Subscription.search.subtitle2.sub1"));
		Label description2 = new Label(composite,SWT.NONE);
		description2.setText(MessageText.getString("Wizard.Subscription.search.subtitle2.sub2"));
		
		Label subTitle3 = new Label(composite,SWT.WRAP);
		subTitle3.setFont(subTitleFont);
		subTitle3.setText(MessageText.getString("Wizard.Subscription.search.subtitle3"));
		
		FormLayout layout = new FormLayout();
		layout.marginLeft = 50;
		layout.marginRight = 50;
		layout.marginTop = 25;
		//layout.spacing = 10;
		composite.setLayout(layout);
		
		FormData data;

		data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		subTitle1.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle1,5);
		data.left = new FormAttachment(50,-width/2);
		searchBackground.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(searchBackground,7,SWT.TOP);
		data.left = new FormAttachment(searchBackground, 45,SWT.LEFT);
		data.right = new FormAttachment(searchBackground, -8,SWT.RIGHT);
		searchInput.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(searchBackground,15);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		subTitle2.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle2,5);
		data.left = new FormAttachment(0);
		checkBullet1.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(checkBullet1,5);
		data.left = new FormAttachment(0);
		checkBullet2.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(checkBullet1, 0, SWT.TOP);
		data.left = new FormAttachment(checkBullet1, 5);
		description1.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(checkBullet2, 0, SWT.TOP);
		data.left = new FormAttachment(checkBullet2, 5);
		description2.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(checkBullet2,15);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		subTitle3.setLayoutData(data);

		return composite;
	}
	
	private Composite createAvailableSubscriptionComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		Label hsep1 = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		Label hsep2 = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		
		Label vsep = new Label(composite, SWT.SEPARATOR | SWT.VERTICAL);
		
		Label subtitle1 = new Label(composite, SWT.NONE);
		Label subtitle2 = new Label(composite, SWT.NONE);
		subtitle1.setFont(subTitleFont);
		subtitle2.setFont(subTitleFont);
		subtitle1.setText(MessageText.getString("Wizard.Subscription.subscribe.library"));
		subtitle2.setText(MessageText.getString("Wizard.Subscription.subscribe.subscriptions"));

		libraryTable = new Table(composite, SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.V_SCROLL | SWT.SINGLE);
		
		final TableColumn torrentColumn = new TableColumn(libraryTable, SWT.NONE);
		torrentColumn.setWidth(Utils.adjustPXForDPI(50));
		
		final Composite compEmpty = new Composite(composite,SWT.NONE);
		compEmpty.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		compEmpty.setBackgroundMode(SWT.INHERIT_DEFAULT);
		FillLayout fl = new FillLayout();
		fl.marginHeight = 15;
		fl.marginWidth = 15;
		compEmpty.setLayout(fl);
		compEmpty.setVisible(false);
		
		final Link labelEmpty = new Link(compEmpty,SWT.WRAP);
		labelEmpty.setText(MessageText.getString("Wizard.Subscription.subscribe.library.empty"));
		labelEmpty.setFont(subTitleFont);
		labelEmpty.setForeground(ColorCache.getColor(composite.getDisplay(), "#6D6F6E"));
		
		labelEmpty.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if(event.text != null && (event.text.startsWith("http://") || event.text.startsWith("https://") ) ) {
					Utils.launch(event.text);
				}
			}
		});
		
		initColumns();
		
		final Composite cTV = new Composite(composite, SWT.NONE);
		cTV.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		cTV.setLayout(layout);

		tvSubscriptions = TableViewFactory.createTableViewSWT(Subscription.class,
				TABLE_SUB_WIZ, TABLE_SUB_WIZ, new TableColumnCore[0], "SubWizRank",
				SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.V_SCROLL | SWT.SINGLE);
		tvSubscriptions.setMenuEnabled(false);
		tvSubscriptions.setHeaderVisible(false);
		tvSubscriptions.setRowDefaultHeightEM(1.4f);
		
		tvSubscriptions.initialize(cTV);

		tvSubscriptions.getComposite().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				org.gudy.azureus2.plugins.ui.tables.TableColumn tcName = tvSubscriptions.getTableColumn("SubWizName");
				org.gudy.azureus2.plugins.ui.tables.TableColumn tcRank = tvSubscriptions.getTableColumn("SubWizRank");
				Rectangle clientArea = ((Composite) event.widget).getClientArea();
				tcName.setWidthPX(clientArea.width - tcRank.getWidth() - 1);
			}
		});
		tvSubscriptions.addSelectionListener(new TableSelectionListener() {
			
			public void selected(TableRowCore[] row) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (tvSubscriptions.getSelectedRowsSize() == 0) {
							addButton.setEnabled(false);
						} else {
							addButton.setEnabled(true);
							TableRowCore[] rows = tvSubscriptions.getSelectedRows();
							Subscription subscription = (Subscription) rows[0].getDataSource();
							if (subscription.isSubscribed()) {
								addButton.setEnabled(false);
							} else {
								addButton.setEnabled(true);
							}
							addButton.setData("subscription", subscription);
						}
					}
				});
			}
			
			public void mouseExit(TableRowCore row) {
			}
			
			public void mouseEnter(TableRowCore row) {
			}
			
			public void focusChanged(TableRowCore focus) {
			}
			
			public void deselected(TableRowCore[] rows) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (tvSubscriptions.getSelectedRowsSize() == 0) {
							addButton.setEnabled(false);
						}
					}
				});
			}
			
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);
		

		UIUpdaterSWT.getInstance().addUpdater(new UIUpdatable() {
			
			public void updateUI() {
				if (tvSubscriptions != null) {
					tvSubscriptions.refreshTable(false);
				}
			}
			
			public String getUpdateUIName() {
				return "SubWiz";
			}
		});
		
		Listener resizeListener = new Listener() {
			
			int last_width;
			
			public void handleEvent(Event event) {
				Table table = (Table)event.widget ;
				Rectangle rect = table.getClientArea();
				int width = rect.width - 3;
				
				if ( width == last_width ){
					return;
				}
				
				last_width = width;
				int nbColumns = table.getColumnCount();
				
				if(nbColumns == 1) {
					table.getColumns()[0].setWidth(width);
				}
				
				((Table)event.widget).update();
			}
		};
			
		//subscriptionTable.addListener(SWT.Resize , resizeListener);
		libraryTable.addListener(SWT.Resize , resizeListener);
		
		final Listener selectionListener = new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				subscriptions = (Subscription[]) item.getData("subscriptions");
				
				tvSubscriptions.removeDataSources(tvSubscriptions.getDataSources().toArray(new Subscription[0]));
				if(subscriptions != null) {
					tvSubscriptions.addDataSources(subscriptions);
				}
				tvSubscriptions.processDataSourceQueueSync();
				
				addButton.setEnabled(false);
				addButton.setData("subscription",null);
				tvSubscriptions.setSelectedRows(new TableRowCore[0]);
				if (subscriptions != null && subscriptions.length > 0) {
					TableRowCore row = tvSubscriptions.getRow(subscriptions[0]);
					if (row != null) {
						row.setSelected(true);
					}
				}
			}
		};
		
		libraryTable.addListener(SWT.Selection, selectionListener);
		
		if(availableSubscriptions != null) {
			libraryTable.addListener(SWT.SetData, new Listener() {
				public void handleEvent(Event event) {
					  TableItem item = (TableItem) event.item;
			          int index = libraryTable.indexOf (item);
			         
			          SubscriptionDownloadDetails subInfo = availableSubscriptions[index];
			          item.setText (subInfo.getDownload().getDisplayName());
			          item.setData("subscriptions",subInfo.getSubscriptions());
			          boolean isSubscribed = false;
			          Subscription[] subs = subInfo.getSubscriptions();
			          for(int i = 0 ; i < subs.length ; i++) {
			        	  if(subs[i].isSubscribed()) isSubscribed = true;
			          }
			          if(isSubscribed) {
			        	  item.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			          }
			          
			          if(subInfo.getDownload() == download) {
			        	  libraryTable.setSelection(item);
			        	  selectionListener.handleEvent(event);
			          }
			          if(index == 0 && download == null) {
			        	  libraryTable.setSelection(item);
			        	  selectionListener.handleEvent(event);
			          }
			          if(libraryTable.getSelectionIndex() == index) {
			        	  //If the item was already selected and we got the SetData afterwards, then let's populate the 
			        	  //subscriptionsTable
			        	  selectionListener.handleEvent(event);
			          }
				}
			});
			
			libraryTable.setItemCount(availableSubscriptions.length);
			if(availableSubscriptions.length == 0) {
				libraryTable.setVisible(false);
				compEmpty.setVisible(true);
			}
		} else {
			//Test code
			libraryTable.addListener(SWT.SetData, new Listener() {
				public void handleEvent(Event event) {
					  TableItem item = (TableItem) event.item;
			          int index = libraryTable.indexOf (item);
			          item.setText ("test " + index);
				}
			});
			
			libraryTable.setItemCount(20);
		}
		
		addButton.setEnabled(false);
		addButton.setData("subscription",null);
		
		
		//final Image rssIcon = imageLoader.getImage("icon_rss");

		libraryTable.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				event.height = 20;
			}
		});
		
		FormLayout formLayout = new FormLayout();
		composite.setLayout(formLayout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.left = new FormAttachment(40, 0);
		data.bottom = new FormAttachment(100, 0);
		vsep.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(0, 5);
		data.right = new FormAttachment(vsep, 0);
		data.left = new FormAttachment(0, 5);
		subtitle1.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(vsep, 5);
		data.right = new FormAttachment(100, 0);
		subtitle2.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(subtitle1, 5);
		data.right = new FormAttachment(vsep, 0);
		data.left = new FormAttachment(0, 0);
		hsep1.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(subtitle2, 5);
		data.left = new FormAttachment(vsep, -1);
		data.right = new FormAttachment(100, 0);
		hsep2.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(hsep1, 0);
		data.right = new FormAttachment(vsep, 0);
		data.left = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		
		if(availableSubscriptions != null && availableSubscriptions.length > 0) {
			libraryTable.setLayoutData(data);
		} else {
			// hack: dispose libraryTable as it's not needed and draws over controls
			//       (makes a white box covering text).  Would be smarter to not
			//       create the libraryTable at all..
			libraryTable.dispose();
			cancelButton.setFocus();
			shell.setDefaultButton(cancelButton);
			compEmpty.setLayoutData(data);
		}

		data = new FormData();
		data.top = new FormAttachment(hsep2, 0);
		data.left = new FormAttachment(vsep, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		cTV.setLayoutData(data);

		
		return composite;
	}
	
	private static void initColumns() {
		if (columnsAdded) {
			return;
		}
		columnsAdded = true;
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		TableManager tableManager = uiManager.getTableManager();
		tableManager.registerColumn(Subscription.class, "SubWizName",
				new TableColumnCreationListener() {
					private Image rssIcon;

					public void tableColumnCreated(
							org.gudy.azureus2.plugins.ui.tables.TableColumn column) {
						
							// this'll get triggered for the Subscriptions Overview table too - easiest fix is to default to hidden there
						
						column.setVisible( column.getTableID().equals( "SubscriptionWizard" ));
						ImageLoader imageLoader = ImageLoader.getInstance();
						rssIcon = imageLoader.getImage("icon_rss");

						column.addCellAddedListener(new TableCellAddedListener() {
							public void cellAdded(TableCell cell) {
								Subscription sub = (Subscription) cell.getDataSource();
								if (sub.isSubscribed()) {
									cell.setForeground(0xa0, 0xa0, 0xa0);
								}
								cell.setText(sub.getName());
								((TableCellSWT) cell).setIcon(rssIcon);
								cell.setToolTip(sub.getNameEx());
							}
						});
					}
				});
		tableManager.registerColumn(Subscription.class, "SubWizRank",
				new TableColumnCreationListener() {
					public void tableColumnCreated(
							org.gudy.azureus2.plugins.ui.tables.TableColumn column) {
						column.setWidthLimits(RANK_COLUMN_WIDTH, RANK_COLUMN_WIDTH);
						column.setVisible(column.getTableID().equals( "SubscriptionWizard" ));	// as above
						column.addCellRefreshListener(new TableCellRefreshListener() {
							public void refresh(TableCell cell) {
								Subscription sub = (Subscription) cell.getDataSource();
								cell.setSortValue(sub.getCachedPopularity());
							}
						});
						if (column instanceof TableColumnCore) {
							TableColumnCore columnCore = (TableColumnCore) column;
							columnCore.setSortAscending(false);
							columnCore.addCellOtherListener("SWTPaint",
									new TableCellSWTPaintListener() {
										public void cellPaint(GC gc, TableCellSWT cell) {
											Subscription sub = (Subscription) cell.getDataSource();

											Rectangle bounds = cell.getBounds();
											bounds.width -= 5;
											bounds.height -= 7;
											bounds.x += 2;
											bounds.y += 3;
											gc.setBackground(ColorCache.getColor(gc.getDevice(), 255,
													255, 255));
											gc.fillRectangle(bounds);
											gc.setForeground(ColorCache.getColor(gc.getDevice(), 200,
													200, 200));
											gc.drawRectangle(bounds);
											bounds.width -= 2;
											bounds.height -= 2;
											bounds.x += 1;
											bounds.y += 1;

											long popularity = sub.getCachedPopularity();
											//Rank in pixels between 0 and 80
											//0 -> no subscriber
											//80 -> 1000 subscribers

											int rank = 80 * (int) popularity / 1000;
											if (rank > 80)
												rank = 80;
											if (rank < 5)
												rank = 5;

											Rectangle clipping = gc.getClipping();

											bounds.width = rank;
											bounds.height -= 1;
											bounds.x += 1;
											bounds.y += 1;
											Utils.setClipping(gc, bounds);

											ImageLoader imageLoader = ImageLoader.getInstance();
											Image rankingBars = imageLoader.getImage("ranking_bars");
											gc.drawImage(rankingBars, bounds.x, bounds.y);
											imageLoader.releaseImage("ranking_bars");

											Utils.setClipping(gc, clipping);
										}

									});
						}
					}
				});

		TableColumnManager tcm = TableColumnManager.getInstance();
		tcm.setDefaultColumnNames(TABLE_SUB_WIZ, new String[] {
			"SubWizName",
			"SubWizRank",
		});

	}

	private void createFonts() {
		
		FontData[] fDatas = shell.getFont().getFontData();
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setStyle(SWT.BOLD);
		}
		boldFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(12);
			} else {
				fDatas[i].setHeight(10);
			}
		}
		subTitleFont = new Font(display,fDatas);
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(17);
			} else {
				fDatas[i].setHeight(14);
			}
		}
		titleFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			if(org.gudy.azureus2.core3.util.Constants.isOSX) {
				fDatas[i].setHeight(14);
			} else {
				fDatas[i].setHeight(12);
			}
			fDatas[i].setStyle(SWT.NONE);
		}
		textInputFont = new Font(display,fDatas);
		
		
	}
	
	private void populateFooter(Composite footer) {
		yesButton = new Button(footer, SWT.PUSH);
		yesButton.setText(MessageText.getString("Button.yes"));
		yesButton.setFont(boldFont);
		
		addButton = new Button(footer, SWT.PUSH);
		addButton.setText(MessageText.getString("Button.add"));
		addButton.setFont(boldFont);
		
		saveButton = new Button(footer, SWT.PUSH);
		saveButton.setText(MessageText.getString("Button.save"));
		saveButton.setEnabled(false);
		saveButton.setFont(boldFont);
		
		searchButton = new Button(footer, SWT.PUSH);
		searchButton.setText(MessageText.getString("Button.search"));
		searchButton.setFont(boldFont);
		
		cancelButton = new Button(footer,SWT.PUSH);
		//cancelButton.setText(MessageText.getString("Button.cancel"));
			
		createButton = new Button(footer,SWT.PUSH);
		createButton.setText(MessageText.getString("Button.createNewSubscription"));
		
		availableButton = new Button(footer,SWT.PUSH);
		availableButton.setText(MessageText.getString("Button.availableSubscriptions"));
		
		FormLayout layout = new FormLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.spacing = 5;
		
		footer.setLayout(layout);
		FormData data;
		
		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		
		yesButton.setLayoutData(data);
		addButton.setLayoutData(data);
		searchButton.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		saveButton.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(saveButton);
		data.width = 100;
		cancelButton.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0);
		data.width = 175;
		createButton.setLayoutData(data);
		availableButton.setLayoutData(data);
		
		
		yesButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				COConfigurationManager.setParameter("subscriptions.opted_in",true);
				COConfigurationManager.save();
				setMode(MODE_SUBSCRIBE);
			}
		});
		
		createButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				setMode(MODE_CREATE_SEARCH);
			}
		});
		
		availableButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				setDefaultAvailableMode();
			}			
		});
		
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				shell.close();
			}
		});
		
		
		rssSaveListener = new Listener() {
			public void handleEvent(Event event) {
				try {
					String url_str = feedUrl.getText();
					URL	url = new URL(url_str);
					
					Map user_data = new HashMap();
					
					user_data.put( SubscriptionManagerUI.SUB_EDIT_MODE_KEY, new Boolean( true ));
					
					Subscription subRSS = SubscriptionManagerFactory.getSingleton().createRSS( url_str, url, SubscriptionHistory.DEFAULT_CHECK_INTERVAL_MINS, user_data );
					shell.close();

					final String key = "Subscription_" + ByteFormatter.encodeString(subRSS.getPublicKey());				

					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					mdi.showEntryByID(key);

				} catch (Throwable e) {
					
					Utils.reportError( e );
				}
			}
		};
		
		saveButton.addListener(SWT.Selection, rssSaveListener);
		
		addButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Subscription subscription = (Subscription) addButton.getData("subscription");
				if(subscription != null) {
					subscription.setSubscribed(true);
					shell.close();
				}
			}
		});
		
		searchListener = new Listener() {
			public void handleEvent(Event event) {
				UIFunctionsManager.getUIFunctions().doSearch(searchInput.getText(),true);
				shell.close();
			}
		};
		
		searchButton.addListener(SWT.Selection, searchListener);
		
	}
	
	private void setDefaultAvailableMode() {
		boolean opted_in = COConfigurationManager.getBooleanParameter("subscriptions.opted_in");
		if(!opted_in) {
			setMode(MODE_OPT_IN);
		} else {
			setMode(MODE_SUBSCRIBE);
		}
	}
	
	private void setMode(int mode) {
		addButton.setVisible(false);
		searchButton.setVisible(false);
		saveButton.setVisible(false);
		yesButton.setVisible(false);
		createButton.setVisible(false);
		availableButton.setVisible(false);
		cancelButton.setText(MessageText.getString("Button.cancel"));
		
		String titleText = TITLE_OPT_IN;
		
		if ( mode != MODE_OPT_IN ){
			if ( rss_feed_url != null ){
				mode = MODE_CREATE_RSS;
				feedUrl.setText( rss_feed_url.toExternalForm());
				rss_feed_url = null;
			}
		}
		switch (mode) {
		case MODE_SUBSCRIBE :
			mainLayout.topControl = availableSubscriptionComposite;
			titleText = TITLE_SUBSCRIBE;
			createButton.setVisible(true);
			addButton.setVisible(true);
			shell.setDefaultButton(addButton);
			break;
			
		case MODE_CREATE_RSS :
			mainLayout.topControl = createComposite;
			createTabFolder.setSelection(createRSSTabItem);
			titleText = TITLE_CREATE;
			availableButton.setVisible(true);
			saveButton.setVisible(true);
			shell.setDefaultButton(saveButton);
			break;
			
		case MODE_CREATE_SEARCH :
			mainLayout.topControl = createComposite;
			createTabFolder.setSelection(createSearchTabItem);
			titleText = TITLE_CREATE;
			availableButton.setVisible(true);
			searchButton.setVisible(true);
			shell.setDefaultButton(searchButton);
			break;
			
		case MODE_OPT_IN:
		default:
			mainLayout.topControl = optinComposite;
			cancelButton.setText(MessageText.getString("Button.no"));
			createButton.setVisible(true);
			yesButton.setVisible(true);
			shell.setDefaultButton(yesButton);
			break;
		}
			
		main.layout(true,true);
		
		title.setText(titleText);
	}
	
	public static void main(String args[]) {
		final SubscriptionWizard sw = new SubscriptionWizard();
				
		while( ! sw.shell.isDisposed()) {
			if(! sw.display.readAndDispatch()) {
				sw.display.sleep();
			}
		}
		
		sw.display.dispose();		
	}
}
