package com.aelitis.azureus.ui.swt.views.skin;

public abstract class AbstractDetailPage
	implements IDetailPage
{

	protected String pageID = null;

	private DetailPanel detailPanel = null;

	protected ActivationListener activationListener = null;

	public AbstractDetailPage(DetailPanel detailPanel, String pageID) {
		this.pageID = pageID;
		this.detailPanel = detailPanel;
	}

	public String getPageID() {
		return pageID;
	}

	public DetailPanel getDetailPanel() {
		return detailPanel;
	}

	public void setActivationListener(ActivationListener activationListener) {
		this.activationListener = activationListener;
	}

}
