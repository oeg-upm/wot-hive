package directory.events;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import directory.Utils;
import info.macias.sse.EventBroadcast;
import info.macias.sse.events.MessageEvent;
import info.macias.sse.servlet3.ServletEventTarget;
import spark.Request;
import spark.Response;
import spark.Route;

public class EventsController  {
	
	// -- Attributes
	public static EventBroadcast broadcaster = new EventBroadcast();

	// -- Constructor
	private EventsController() {
		super();
	}
	
	// -- Methods
	 public static Route subscribe = (Request request, Response response) -> {
		
		String eventType = request.queryParams("type");
		String includeChanged = request.queryParams("include_changes");

		response.header(Utils.HEADER_CONTENT_TYPE, "text/event-stream");
		//response.header("Last-Event-ID", "[id of the event]");
		response.status(200);
		broadcaster.addSubscriber(new ServletEventTarget(request.raw()));
		igniteCreateEvent(null);
		return "";
	};

	public static void igniteCreateEvent(String thingId) {
		String msg = Utils.buildMessage("event: create\ndata: {\"id\": \"",thingId,"\"}\nc\n\n");
		
		MessageEvent welcome = new MessageEvent.Builder()
					.setId("id of the event")
					.setEvent("create")
					.setData("the message").build();
		broadcaster.broadcast(welcome);
		System.out.println("adding event");
	}
	
}
