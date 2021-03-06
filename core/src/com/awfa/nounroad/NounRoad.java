package com.awfa.nounroad;

import java.util.ArrayList;
import java.util.List;

import com.awfa.nounroad.MessageSystem.Message;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Interpolation;

public class NounRoad extends ApplicationAdapter implements MessageListener {
	private MessageSystem messageSystem;
	private GameManager gameManager;
	private GameInputManager gameInputManager;
	private GameConfig gameConfig;
	
	private TextureAtlas atlas;
	private SpriteBatch batch;
	private ShapeRenderer shape;
	private OrthographicCamera camera;
	private BitmapFont gameFont;
	private BitmapFont smallFont;
	
	private NinePatch nameBox;
	
	private NinePatch textBox;
	
	private NinePatch redTextBox;
	private NinePatch greenTextBox;
	private NinePatch yellowTextBox;
	
	private ParticleEffect sparks;
	private ParticleEmitter sparkEmitter;
	
	private ParticleEffect strikeSparks;
	private ParticleEffectPool strikeSparksEffectPool;
	List<PooledEffect> strikeSparkEffects;
	
	private InterpolatedPosition recentWordsPosition;
	private AlphaController redAlpha;
	private AlphaController greenAlpha;
	private AlphaController yellowAlpha;
	
	@Override
	public void create() {
		messageSystem = new MessageSystem();
		
		messageSystem.register(this, Message.TEXT_ENTERED);
		messageSystem.register(this, Message.STATE_CHANGE);
		messageSystem.register(this, Message.PLAYER_SCORED);
		messageSystem.register(this, Message.PLAYER_STRIKED);
		messageSystem.register(this, Message.INVALID_INPUT);
		messageSystem.register(this, Message.PLAYER_NAME_ENTERED);
		messageSystem.register(this, Message.GAME_RESET);
		
		LoggingSystem logger = new LoggingSystem(messageSystem);
		AudioSystem audioSystem = new AudioSystem(messageSystem);
		
		gameInputManager = new GameInputManager(messageSystem);
		gameManager = new GameManager(messageSystem);
		Gdx.input.setInputProcessor(gameInputManager);
		GameConfig.loadConfig(Gdx.files.internal("config.json"));
		
		atlas = new TextureAtlas("graphicAssets.atlas");
		batch = new SpriteBatch();
		shape = new ShapeRenderer();
		camera = new OrthographicCamera();
		gameFont = new BitmapFont(Gdx.files.internal("font.txt"));
		smallFont = new BitmapFont(Gdx.files.internal("fontSmall.txt"));
		
		nameBox = atlas.createPatch("nameBox");
		
		textBox = atlas.createPatch("typeWordBox");
		
		redTextBox = atlas.createPatch("redTypeWordBox");
		yellowTextBox = atlas.createPatch("yellowTypeWordBox");
		greenTextBox = atlas.createPatch("greenTypeWordBox");
		
		
		sparks = new ParticleEffect();
		sparks.load(Gdx.files.internal("sparks.p"), Gdx.files.internal(""));
		sparkEmitter = sparks.findEmitter("sparks");
		sparkEmitter.setContinuous(true);
		
		strikeSparks = new ParticleEffect();
		strikeSparks.load(Gdx.files.internal("strikeSparks.p"), atlas);
		strikeSparksEffectPool = new ParticleEffectPool(strikeSparks, 6, 6);
		strikeSparkEffects = new ArrayList<PooledEffect>(6);
		
		float width = Gdx.graphics.getWidth();
		float height = Gdx.graphics.getHeight();

		camera.setToOrtho(false, width, height);
		batch.setProjectionMatrix(camera.combined);
		shape.setProjectionMatrix(camera.combined);
		
		// interpolated positions
		DrawPosition recentWordsDP = GameConfig.drawPositions.get("recentWords");
		recentWordsPosition = new InterpolatedPosition(
			recentWordsDP.xPos,
			recentWordsDP.yPos,
			Interpolation.exp10In
		);
		
		FlashInterpolation f = new FlashInterpolation();
		redAlpha = new AlphaController(f);
		greenAlpha = new AlphaController(f);
		yellowAlpha = new AlphaController(f);
	}

	@Override
	public void render() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		
		gameManager.update(deltaTime);
		Gdx.gl.glClearColor(203.f/255, 240.f/255, 241.f/255, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		camera.update();
		
		shape.begin(ShapeType.Filled);
		shape.setColor(new Color(229.f/255, 248.f/255, 248.f/255, 1.0f));
		// draw the light bar at the top that holds recent words
		shape.rect(0, 720-94, 1280, 79);
		
		// draw the progress bar and set the position for the bar sparks
		if(gameManager.getState() != GameManager.State.ENTERING_PLAYER_NAMES) {
			DrawPosition textBoxPosition = GameConfig.drawPositions.get("textBox");
			shape.setColor(new Color(60.f/255, 144.f/255, 179.f/255, 1.0f));
			float progressWidth = 0.0f;
			if(gameManager.getState() == GameManager.State.GAME_INITIALIZE) {
				progressWidth = 823*(gameManager.getTimeLeft()/GameManager.INIT_TIME);
			} else {
				progressWidth = 823*(gameManager.getTimeLeft()/GameManager.TIME_LIMIT);
			}
			shape.rect(textBoxPosition.xPos, textBoxPosition.yPos-12, progressWidth, 3);
			shape.rect(textBoxPosition.xPos, textBoxPosition.yPos-12-3, 3, 9);
			sparkEmitter.setPosition(textBoxPosition.xPos + progressWidth, textBoxPosition.yPos-12);
		}
		shape.end();
		
		batch.begin();
		// draw strike shadows
		drawFromConfig(batch, "strikeOutShadow", "strikePositionPlayer1", GameManager.MAX_STRIKES);
		drawFromConfig(batch, "strikeOutShadow", "strikePositionPlayer2", GameManager.MAX_STRIKES);
		
		// draw the text box background
		DrawPosition textBoxPosition = GameConfig.drawPositions.get("textBox");
		textBox.draw(batch, textBoxPosition.xPos, textBoxPosition.yPos, 823, 92);

		// draw the text box overlays
		redAlpha.update(deltaTime/0.5f);
		greenAlpha.update(deltaTime/0.5f);
		yellowAlpha.update(deltaTime/0.5f);
		
		redTextBox.setColor(new Color(1.0f, 1.0f, 1.0f, redAlpha.getAlpha()));
		greenTextBox.setColor(new Color(1.0f, 1.0f, 1.0f, greenAlpha.getAlpha()));
		yellowTextBox.setColor(new Color(1.0f, 1.0f, 1.0f, yellowAlpha.getAlpha()));
		
		redTextBox.draw(batch, textBoxPosition.xPos, textBoxPosition.yPos, 823, 92);
		greenTextBox.draw(batch, textBoxPosition.xPos, textBoxPosition.yPos, 823, 92);
		yellowTextBox.draw(batch, textBoxPosition.xPos, textBoxPosition.yPos, 823, 92);		
		
		if(gameManager.getState() != GameManager.State.ENTERING_PLAYER_NAMES) {
			// draw the score box
			DrawPosition scoreBoxPosition = GameConfig.drawPositions.get("scoreBox");
			textBox.draw(batch, scoreBoxPosition.xPos, scoreBoxPosition.yPos, 391, 51);
			
			String scoreBoxText = "Words Played: " + gameManager.getWordsPlayed();
			smallFont.draw(batch, scoreBoxText,
					scoreBoxPosition.xPos + (391.f/2) - smallFont.getBounds(scoreBoxText).width/2,
					scoreBoxPosition.yPos + 45);
			
			// draw player 1's box and name
			Player player1 = gameManager.getPlayer(0);
			String p1Name = player1.getName();
			DrawPosition leftNameBoxPos = GameConfig.drawPositions.get("leftNameBox");
			nameBox.draw(batch, leftNameBoxPos.xPos, leftNameBoxPos.yPos,
					gameFont.getBounds(p1Name).width+27+18+30, 74);
			gameFont.draw(batch, p1Name, 27, leftNameBoxPos.yPos+66);
			
			// draw player 2's box and name
			Player player2 = gameManager.getPlayer(1);
			String p2Name = player2.getName();
			DrawPosition rightNameBoxPos = GameConfig.drawPositions.get("rightNameBox");
			nameBox.draw(batch, rightNameBoxPos.xPos-27-18-30-gameFont.getBounds(p2Name).width, 
					rightNameBoxPos.yPos,
					1000, 74);
			gameFont.draw(batch, p2Name, 1280-27-gameFont.getBounds(p2Name).width, leftNameBoxPos.yPos+66);
			
			// draw the arrow pointed at the current player
			Sprite arrow = atlas.createSprite("arrow");
			if (gameManager.getCurrentPlayer() == gameManager.getPlayer(0)) {
				arrow.flip(true, false);
				arrow.setPosition(27+gameFont.getBounds(p1Name).width+18+30+10, leftNameBoxPos.yPos+10);	
			} else {
				arrow.setPosition(1280-27-gameFont.getBounds(p2Name).width-arrow.getWidth()-18-30-10, leftNameBoxPos.yPos+10);
			}
			arrow.draw(batch);
			
			// draw the recent words making the most recent word appear on the very right
			recentWordsPosition.update(deltaTime/0.5f);
			gameFont.draw(batch, gameManager.getRecentWords(),
					recentWordsPosition.getCurrX(),
					recentWordsPosition.getCurrY());
			
			// draw the strikes the player has
			drawFromConfig(batch, "strikeOut", "strikePositionPlayer1", player1.getStrikes());
			drawFromConfig(batch, "strikeOut", "strikePositionPlayer2", player2.getStrikes());
			
			// render the sparks
			sparkEmitter.draw(batch, deltaTime);
			for (int i = strikeSparkEffects.size()-1; i >= 0; i--) {
				PooledEffect effect = strikeSparkEffects.get(i);
				effect.draw(batch, deltaTime);
				if (effect.isComplete()) {
					effect.free();
					strikeSparkEffects.remove(i);
				}
			}
		}
		
		// draw the text in the main textbox in the center
		gameFont.draw(batch, gameInputManager.getInput(),
				Gdx.graphics.getWidth()/2 - gameFont.getBounds(gameInputManager.getInput()).width/2,
				textBoxPosition.yPos + 75);
		batch.end();
	}

	private void drawFromConfig(SpriteBatch batch, String texName, String posName, int amountToDraw) {
		Sprite drawTexture = atlas.createSprite(texName);
		for(int i = 0; i < amountToDraw; ++i) {
			DrawPosition drawPosition = GameConfig.drawPositions.get(posName);
			
			batch.draw(
				drawTexture,
				drawPosition.xPos + drawPosition.xOffset * i,
				drawPosition.yPos + drawPosition.yOffset * i
			);
		}
	}
	
	@Override
	public void recieveMessage(Message message) {
		if (message == Message.STATE_CHANGE) {
			gameInputManager.setInput("");
			if (gameManager.getState() == GameManager.State.GAME_OVER) {
				gameInputManager.setInput(gameManager.getCurrentPlayer().getName() + " wins!");
			}
		} else if (message == Message.PLAYER_NAME_ENTERED && gameManager.getPlayerIndex() == 0) {
			gameInputManager.setInput("Player2");
		} else if (message == Message.GAME_RESET) {
			gameInputManager.setInput("Player1");
		}
	}

	@Override
	public void recieveMessage(Message message, MessageExtra extra) {
		if (message == Message.TEXT_ENTERED) {
			gameManager.takeInput(extra.message);
		} else if (message == Message.PLAYER_STRIKED || message == Message.PLAYER_SCORED) {
			String lastWords = gameManager.getRecentWords();
			if(!lastWords.isEmpty()) {
				DrawPosition recentWordsDP = GameConfig.drawPositions.get("recentWords");
				// set the input to start with a correct letter if possible
				String lastLetter = lastWords.substring(lastWords.length()-1, lastWords.length());
				gameInputManager.setInput(lastLetter);
				
				// make sure sliding doesn't glitch when the recent words limit is reached
				String[] lastWordsList = lastWords.split(GameManager.RECENT_DELIMITER);
				if(lastWordsList.length == WordManager.MAX_RECENT_WORDS+1) {
					recentWordsPosition.setNewPosition(
							recentWordsDP.xPos
								-gameFont.getBounds(lastWords).width
								+gameFont.getBounds(GameManager.RECENT_DELIMITER + lastWordsList[lastWordsList.length-1]).width,
							recentWordsDP.yPos);
				}
				
				// set the slide target for recent words
				recentWordsPosition.setNewTarget(
						recentWordsDP.xPos-gameFont.getBounds(lastWords).width,
						recentWordsDP.yPos);
			} else {
				gameInputManager.setInput("");
			}
		}
		if (message == Message.PLAYER_SCORED) {
			greenAlpha.startInterpolation();
		}
		if (message == Message.PLAYER_STRIKED) {
			redAlpha.startInterpolation();
			if (extra.number == 0) { // first player striked
				int numOfStrikes = gameManager.getPlayer(0).getStrikes();
				PooledEffect effect = strikeSparksEffectPool.obtain();
				DrawPosition dp = GameConfig.drawPositions.get("strikePositionPlayer1");
				effect.setPosition(dp.xPos + dp.xOffset * numOfStrikes - 33, dp.yPos + 30);
				strikeSparkEffects.add(effect);
			} else { // second player striked
				int numOfStrikes = gameManager.getPlayer(1).getStrikes();
				PooledEffect effect = strikeSparksEffectPool.obtain();
				DrawPosition dp = GameConfig.drawPositions.get("strikePositionPlayer2");
				effect.setPosition(dp.xPos + dp.xOffset * numOfStrikes + 90, dp.yPos + 30);
				strikeSparkEffects.add(effect);
			}
		}
		
		if (message == Message.INVALID_INPUT) {
			yellowAlpha.startInterpolation();
		}
	}
}
