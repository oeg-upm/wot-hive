package directory.events;

public enum DirectoryEvent {

	CREATE("create"),
	DELETE("delete"),
	UPDATE("update"),
	ALL("all");
	String event;
	
	DirectoryEvent(String event) {
		this.event = event;
	}

	public String getEvent() {
		return event;
	}
	
	
}
