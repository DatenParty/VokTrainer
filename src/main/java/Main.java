import net.freeutils.httpserver.HTTPServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static net.freeutils.httpserver.HTTPServer.*;

public class Main {
	static Database db = new Database();
	
	public static void main(String[] args) {
		Log.status("starting server");
		if (db.isValid()) {
			Log.success("Database connection established");
			
			HTTPServer server = new HTTPServer(1337);
			VirtualHost host = server.getVirtualHost(null);
			host.addContext("/get/list", new getList());
			host.addContext("/add/words", new addWords());
			host.addContext("/add/list", new addList());
			
			try {
				server.start();
			} catch (IOException e) {
				Log.critical("httpserver start failed");
				Log.critical("Aborting Server");
				System.exit(-1);
			}
			Log.success("httpserver start succesful");
			
		} else {
			Log.critical("Database connection failed.");
			Log.critical("Aborting Server");
			System.exit(-1);
		}
	}
	
	private static void sendResponse(HTTPServer.Response response, int status, JSONObject responseObject) {
		response.getHeaders().add("Content-Type", "application/json");
		try {
			response.send(status, responseObject.toString());
		} catch (IOException e) {
			Log.error("Response cannot be sent");
		}
	}
	
	private static class getList implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("NEW LIST REQUEST");
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			
			String lang1;
			String lang2;
			Integer list;
			
			try {
				lang1 = params.get("lang1");
				lang2 = params.get("lang2");
				
				/*	This is super weird code but it works.
				/
				/	Expl:	The "try" already catches and responses when the list parameter has a bad request.
				/				I'm just duplication a bad request for list when lang1 or lang2 are nulls, it's the same error anyway.
				/				(Had to write the explanation in case I forget what I have done here)
				*/
				if (lang1.isEmpty() || lang2.isEmpty()) {
					params.get("not existing");
				}
				
				list = Integer.parseInt(params.get("list"));
				
				Log.status("[API] good request");
			} catch (Exception e) {
				Log.error("[API] bad request");
				header.put("status", 400);
				sendResponse(response, 400, responseObject.put("header", header));
				return 400;
			}
			
			ResultSet resultLang1 = db.execute(DB.getListQuery(lang1, list));
			ResultSet resultLang2 = db.execute(DB.getListQuery(lang2, list));
			
			JSONArray results = new JSONArray();
			try {
				while (resultLang1.next() && resultLang2.next()) {
					JSONObject item = new JSONObject();
					item.put("lang1", resultLang1.getString("word"));
					item.put("lang2", resultLang2.getString("word"));
					
					results.put(item);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (results.isEmpty()) {
				Log.error("[SQL] ResultSet is empty");
				header.put("status", 404);
				sendResponse(response, 400, responseObject.put("header", header));
				return 400;
			} else {
				Log.status("[SQL] found data");
			}
			
			
			header.put("lang1", lang1);
			header.put("lang2", lang2);
			header.put("list", list);
			header.put("status", 200);
			
			responseObject.put("header", header);
			responseObject.put("results", results);
			
			sendResponse(response, 200, responseObject);
			Log.success("[API] request handeled");
			return 0;
		}
	}
	
	private static class addWords implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			return 0;
		}
	}
	
	private static class addList implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			return 0;
		}
	}
}
