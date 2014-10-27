package com.pinggusoft.listitem;


public class EntryItem implements Item{

	public final String title;
	public String subtitle;
	public final int    drawable;
	public final int    id;
	public boolean enabled;

	public EntryItem(int drawable, String title, String subtitle, int id) {
		this.title = title;
		this.subtitle = subtitle;
		this.drawable = drawable;
		this.id      = id;
		this.enabled = true;
	}
	
    public EntryItem(int drawable, String title, String subtitle, int id, boolean en) {
        this.title = title;
        this.subtitle = subtitle;
        this.drawable = drawable;
        this.id      = id;
        this.enabled = en;
    }	
	
	public void setEnabled(boolean en) {
	    enabled = en;
	}
	
	public boolean isEnabled() {
	    return enabled;
	}
	
	@Override
	public int getMode() {
		return MODE_ITEM_DEFAULT;
	}

}
