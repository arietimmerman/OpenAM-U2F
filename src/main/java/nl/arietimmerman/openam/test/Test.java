package nl.arietimmerman.openam.test;

import java.io.IOException;

import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonObject;

public class Test {

	
	public static void main(String[] args) throws IOException {
		
		
		
		JsonValue jsonValue = new JsonValue(Json.readJson("[{\"test\":\"test\",\"test2\":\"pietje\"}]"));
		
		System.out.println(jsonValue.get(0).get("test2"));
		
	}
}
