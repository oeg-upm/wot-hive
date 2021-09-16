package directory.events;

import directory.Utils;
import info.macias.sse.servlet3.ServletEventTarget;
import spark.Request;
import spark.Response;
import spark.Route;

public class EventsController {

	// -- Attributes

    public static final EventSystem eventSystem = new EventSystem();
    private static final String EVENT_MIME_TYPE = "text/event-stream";
	
    // -- Constructor
	private EventsController() {
		super();
	}

	// -- Routing methods
	
	public static final Route subscribe = (Request request, Response response) -> {
		 instantiateSubscriber(request, DirectoryEvent.ALL);
		 prepareResponse(response);
		return "";
	};
	
	public static final Route subscribeCreate = (Request request, Response response) -> {
		 instantiateSubscriber(request, DirectoryEvent.CREATE);
		 prepareResponse(response);
		return "";
	};
	
	public static final Route subscribeUpdate = (Request request, Response response) -> {
		 instantiateSubscriber(request, DirectoryEvent.UPDATE);
		 prepareResponse(response);
		return "";
	};
	
	public static final Route subscribeDelete = (Request request, Response response) -> {
		 instantiateSubscriber(request, DirectoryEvent.DELETE);
		 prepareResponse(response);
		return "";
	};
	
	private static void prepareResponse(Response response) {
		response.header(Utils.HEADER_CONTENT_TYPE, EVENT_MIME_TYPE);
		response.status(200);
	}

	private static final String DIFF_PARAMETER = "diff";
	private static void instantiateSubscriber(Request request, DirectoryEvent eventType) {
		Boolean diff = request.queryParams(DIFF_PARAMETER)!=null && request.queryParams(DIFF_PARAMETER).equals("true");
		ServletEventTarget client = new ServletEventTarget(request.raw());
		Subscriber newSubscriber = new Subscriber(client, eventType, diff);
		String lastEventId = request.headers("Last-Event-ID");
		eventSystem.addSubscriber(newSubscriber, lastEventId);
	}
	

	
}
