package directory.search;

import spark.Request;
import spark.Response;
import spark.Route;



public class XPathController {


	private XPathController() {
		super();
	}
	
	public static final Route solveXPath = (Request request, Response response) -> {		
		response.status(501);
		return "";
	};
    
}
