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
			host.addContext("/request", new DefaultContextHandler());
			
			try {
				server.start();
			} catch (IOException e) {
				Log.critical("httpserver start failed");
				System.exit(-1);
			}
			Log.success("httpserver start succesful");
			
		} else {
			Log.critical("Database connection failed.");
			System.exit(-1);
		}
	}
	
	private static class DefaultContextHandler implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Map<String, String> params = request.getParams();
			
			System.out.println();
			System.out.println(request.getURI());
			
			String lang1 = params.get("lang1");
			String lang2 = params.get("lang2");
			Integer list = Integer.parseInt(params.get("list"));
			
			ResultSet resultLang1 = db.execute(DB.getListQuery(lang1, list));
			ResultSet resultLang2 = db.execute(DB.getListQuery(lang2, list));
			
			JSONObject responseObject = new JSONObject();
			
			JSONObject header = new JSONObject();
			header.put("lang1", lang1);
			header.put("lang2", lang2);
			header.put("list", list);
			header.put("status", 200);
			responseObject.put("header", header);
			
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
			responseObject.put("results", results);
			
			
			response.getHeaders().add("Content-Type", "application/json");
			response.send(200, responseObject.toString());
			return 0;
		}
	}
}
