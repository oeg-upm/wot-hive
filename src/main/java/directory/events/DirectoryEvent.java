package directory.events;

public enum DirectoryEvent {

	CREATE("thing_created"),
	DELETE("thing_deleted"),
	UPDATE("thing_updated"),
	ALL("all");
	String event;
	
	DirectoryEvent(String event) {
		this.event = event;
	}

	public String getEvent() {
		return event;
	}
	
	
}
