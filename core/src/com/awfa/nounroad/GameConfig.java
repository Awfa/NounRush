package com.awfa.nounroad;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class GameConfig {
	public static Map<String, DrawPosition> drawPositions;
	
	public static void loadConfig(FileHandle file) {
		JsonReader jsonReader = new JsonReader();
		JsonValue configuration = jsonReader.parse(file);
		
		// draw positions
		JsonValue drawPositionsJson = configuration.get("positions");
		drawPositions = new HashMap<String, DrawPosition>();
		for(int i = 0; i < drawPositionsJson.size; ++i) {
			JsonValue positionJson = drawPositionsJson.get(i);
			DrawPosition drawPosition = new DrawPosition();
			drawPosition.xPos = positionJson.getInt("xPos");
			drawPosition.yPos = positionJson.getInt("yPos");
			drawPosition.xOffset = positionJson.getInt("xOffset");
			drawPosition.yOffset = positionJson.getInt("yOffset");
			drawPositions.put(positionJson.getString("name"), drawPosition);
		}
	}
}
