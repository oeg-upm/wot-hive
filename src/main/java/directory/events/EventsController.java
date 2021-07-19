package directory.events;

import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import directory.Utils;
import spark.Request;
import spark.Response;
import spark.Route;

public class EventsController  {
	 
	public static Queue<String> eventsQueue = new ConcurrentLinkedQueue<>();
	
	
	 public static Route subscribe = (Request request, Response response) -> {
		
		String eventType = request.queryParams("type");
		String includeChanged = request.queryParams("include_changes");
		System.out.println("here!");
		response.header(Utils.HEADER_CONTENT_TYPE, "text/event-stream");
		//response.header("Last-Event-ID", "[id of the event]");
		response.status(200);
		
	
		
		OutputStream out = response.raw().getOutputStream();
		
		
		while(true) {
			String event = eventsQueue.poll();
			if(event!=null) {
				out.write(event.getBytes());
				out.flush();
			}
		}
	};

	public static void igniteCreateEvent(String thingId) {
		String msg = Utils.buildMessage("event: create\ndata: {\"id\": \"",thingId,"\"}\nid: event_0\n\n");
		eventsQueue.add(msg);
	}
	
}
