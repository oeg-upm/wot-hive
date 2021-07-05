package directory.events;

import java.io.OutputStream;

import directory.Utils;
import spark.Request;
import spark.Response;
import spark.Route;

public class EventsController  {
	 
	
	
	
	 public static Route subscribe = (Request request, Response response) -> {
		long startTime = System.nanoTime();

		String eventType = request.queryParams("type");
		String includeChanged = request.queryParams("include_changes");
		System.out.println("here!");
		response.header(Utils.HEADER_CONTENT_TYPE, "text/event-stream");
		//response.header("Last-Event-ID", "[id of the event]");
		response.status(200);
		String msg = "event: create\n" + 
				"data: {\"id\": \"urn:example:simple-td\"}\n" + 
				"id: event_0\n\n";
		
		OutputStream responseStream = response.raw().getOutputStream();
	
		try {
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
		
	};

	
}
