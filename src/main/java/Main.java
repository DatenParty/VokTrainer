import net.freeutils.httpserver.HTTPServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static net.freeutils.httpserver.HTTPServer.*;

public class Main {
	private static Database db;
	
	
	public static void main(String[] args) {
		Log.status("starting vokTrainer Server".toUpperCase());
		
		db  = new Database();
		if (db.isValid()) {
			Log.success("Database connection established");
			
			HTTPServer server = new HTTPServer(1337);
			VirtualHost host = server.getVirtualHost(null);
			//All responses have statuses in header to check for errors
			
			//params: --												| returns header(request params),results(lists)
			//really, all it does is returning all lists from list_index
			host.addContext("/get/lists", new getLists());
			
			//params: list_id										| returns header(request params),results(vocabulary)
			//returns vocabulary for specific list
			host.addContext("/get/list", new getList());
			
			//params: lang1,word1,lang2,word2	| returns header(request params)
			//adds new word to directory
			host.addContext("/add/words", new addWord());
			
			//params: titles,lang1,lang2				| returns header(list_id,request params)
			//creates list
			host.addContext("/add/list", new addList());
			
			//params: id_list,id_translation		| returns header(request params)
			//connects vocabulary to lists
			host.addContext("/add/content", new addContent());
			
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
	
	/**
	 * @param 	response	httpserver response
	 * @return	json with status 400, http status 400
	 * @see			HTTPServer.Response
	 */
	private static Integer sendBadApiReq(HTTPServer.Response response) {
		Log.error("[API] bad request");
		
		JSONObject object = new JSONObject();
		object.put("header", new JSONObject().put("status", 400));
		
		sendResponse(response, 400, object);
		return 400;
	}
	
	/**
	 *
	 */
	private static class getList implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("NEW LIST REQUEST");
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			
			Integer list;
			
			try {
				list = Integer.parseInt(params.get("list"));
				
				Log.status("[API] good request");
			} catch (Exception e) {
				return sendBadApiReq(response);
			}
			
			String lang1;
			String lang2;
			
			try {
				ResultSet getLangs = db.execute("SELECT lang_1,lang_2 FROM list_Index WHERE id_list=?", list);
				getLangs.next();
				lang1 = getLangs.getString("lang_1");
				lang2 = getLangs.getString("lang_2");
				
			} catch (SQLException e) {
				e.printStackTrace();
				sendBadApiReq(response);
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
	
	private static class getLists implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray results = new JSONArray();
			
			try {
				ResultSet resultSet = db.execute("SELECT * FROM list_Index");
				
				while (resultSet.next()) {
					JSONObject result = new JSONObject();
					
					result.put("id", resultSet.getInt("id_list"));
					result.put("name", resultSet.getString("name"));
					result.put("lang_1", resultSet.getString("lang_1"));
					result.put("lang_2", resultSet.getString("lang_2"));
					
					results.put(result);
				}
				
				header.put("status", 200);
				responseObject.put("results", results);
				responseObject.put("header", header);
				
				sendResponse(response, 200, responseObject);
				return 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	}
		
		private static class addWord implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("ADD WORD REQUEST");
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			
			String lang1;
			String word1;
			String lang2;
			String word2;
			
			try {
				lang1 = params.get("lang1");
				word1 = params.get("word1");
				lang2 = params.get("lang2");
				word2 = params.get("word2");
				
				Log.status("[API] good request");
			} catch (Exception e) {
				return sendBadApiReq(response);
			}
			
			try {
				db.update("INSERT INTO translation_words VALUES (DEFAULT,?,?)",lang1,word1);
				ResultSet word1_set = db.execute("SELECT id_word FROM translation_words ORDER BY id_word DESC LIMIT 1;");
				word1_set.next();
				int word1_ID = word1_set.getInt("id_word");
				
				db.update("INSERT INTO translation_words VALUES (DEFAULT,?,?)",lang2,word2);
				ResultSet word2_set = db.execute("SELECT id_word FROM translation_words ORDER BY id_word DESC LIMIT 1;");
				word2_set.next();
				int word2_ID = word2_set.getInt("id_word");
				
				
				db.execute("INSERT INTO dictionary SET " + lang1 + "='" + word1_ID + "'," + lang2 + "='" + word2_ID + "'");
				//INSERT INTO translation_index SET de = '21',en = '33'
				header.put("id_dictionary", db.execute("SELECT id_dictionary AS id FROM dictionary ORDER BY id_dictionary DESC LIMIT 1;").getInt("id"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			header.put("lang1", lang1);
			header.put("word1", word1);
			header.put("lang2", lang2);
			header.put("word2", word2);
			header.put("status", 200);
			responseObject.put("header", header);
			
			sendResponse(response, 200, responseObject);
			Log.success("[API] request handeled");
			return 0;
		}
	}
	
	private static class addList implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Log.warning("ADD LIST REQUEST");
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			
			String lang1;
			String lang2;
			String title;
			
			try {
				lang1 = params.get("lang1");
				lang2 = params.get("lang2");
				title = params.get("title");
				
				Log.status("[API] good request");
			} catch (Exception e) {
				return sendBadApiReq(response);
			}
			
			int list_id;
			
			try {
				db.update("INSERT INTO list_Index (id_list, name, lang_1, lang_2) VALUES (DEFAULT,?,?,?)",title,lang1,lang2);
				list_id = db.execute("SELECT id_list FROM list_Index ORDER BY id_list DESC LIMIT 1;").getInt("id_list");
			
			} catch (Exception e) {
				Log.critical("[SQL] Something went wrong");
				return 500;
			}
			Log.status("[SQL] New list created");
			
			header.put("lang1", lang1);
			header.put("lang2", lang2);
			header.put("title", title);
			header.put("list_id", list_id);
			header.put("status", 200);
			
			responseObject.put("header", header);
			sendResponse(response, 200, responseObject);
			Log.success("[API] request handeled");
			return 0;
		}
	}
	
	private static class addContent implements ContextHandler {
		@Override
		public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
			Map<String, String> params = request.getParams();
			
			JSONObject responseObject = new JSONObject();
			JSONObject header = new JSONObject();
			JSONArray wordsResp = new JSONArray();
			
			int list_id = Integer.parseInt(params.get("list"));
			String words = params.get("words");
			
			
			String[] wordsArray = words.split(",");
			for (String aWordsArray : wordsArray) {
				try {
					db.execute("INSERT INTO list_items VALUES (" + list_id + ", " + aWordsArray + ")");
					wordsResp.put(aWordsArray);
				} catch (Exception e) {
					sendBadApiReq(response);
				}
			}
			
			header.put("status", 200);
			responseObject.put("header", header.put("added", wordsResp));
			
			sendResponse(response, 200, responseObject);
			return 0;
		}
	}
}
