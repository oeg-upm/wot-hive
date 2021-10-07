package directory.events;

import info.macias.sse.servlet3.ServletEventTarget;

public class Subscriber {
	
	private ServletEventTarget client;
	private DirectoryEvent eventType;
	private Boolean diff;
	
	
	public Subscriber(ServletEventTarget client, DirectoryEvent eventType, Boolean diff) {
		super();
		this.client = client;
		this.eventType = eventType;
		this.diff = diff;
	}


	public ServletEventTarget getClient() {
		return client;
	}


	public void setClient(ServletEventTarget client) {
		this.client = client;
	}


	public DirectoryEvent getEventType() {
		return eventType;
	}


	public void setEventType(DirectoryEvent eventType) {
		this.eventType = eventType;
	}


	public Boolean getDiff() {
		return diff;
	}


	public void setDiff(Boolean diff) {
		this.diff = diff;
	}
	
	
	

}
