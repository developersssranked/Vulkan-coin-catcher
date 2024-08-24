package com.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

// Class
public class Main extends ApplicationAdapter {
    // const
    static final float SCREEN_WIDTH = 1920; // default screen width
    static final float SCREEN_HEIGHT = 1440; // default screen height
    static final float PPM = 32; // pixels per meter in Box2D world
    final boolean SHOW_DEBUG = false; // show debug
    final float BRIGHTNESS_PRESSED = 0.9f; // button brightness when pressed
    final float BG_VOLUME = 0.2f; // background music volume
    final int TIME = 30; // time limit in seconds
    final int ADD_TIME = 1; // add time in seconds after coin touch
    final float MIN_SHOW_COIN_INTERVAL = 0.05f; // min time to show coin in seconds
    final float MAX_SHOW_COIN_INTERVAL = 0.5f; // max time to show coin in seconds

    // vars
    static Stage stage;
    static World world;
    static AssetManager assetManager;
    static InputListener controlListener;
    static float ratio;
    static Array<Body> destroyBodies;
    static Array<Joint> destroyJoints;
    OrthographicCamera cam;
    JsonValue map;
    float mapWidth;
    float mapHeight;
    Preferences pref;
    Box2DDebugRenderer debug;
    String screenColor;
    SpriteBatch batch;
    int currentWidth;
    int currentHeight;
    Viewport viewport;
    InterfaceListener nativePlatform;
    Music sndBg;
    float taskDelay;
    boolean gamePaused;
    Act btnSound;
    Act btnPause;
    Group groupPause, groupRate;
    int score;
    Vector2 point;
    float currentVolume = 0;
    boolean isForeground = true;
    Task TIMER, COIN;
    String screen = ""; // screen

    Group groupGameOver;
    Act txtScore;
    Act txtReady;
    Act progressOver;
    Act progressLine;
    Act progressBg;
    Act vulcan;
    TextureAtlas numbers;
    Array<Act> clouds;
    ParticleEffect effect;
    Animation coinAnimation;
    int currentTime;
    Array<Act> coins;

    // Constructor
    public Main(InterfaceListener nativePlatform) {
        this.nativePlatform = nativePlatform;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        controlListener = new CONTROL();
        destroyBodies = new Array<>();
        destroyJoints = new Array<>();
        currentWidth = Gdx.graphics.getWidth();
        currentHeight = Gdx.graphics.getHeight();
        assetManager = new AssetManager();
        Gdx.input.setCatchKey(Keys.BACK, true); // prevent back on mobile
        TIMER();
        COIN();

        // load assets
        Lib.loadAssets();

        // debug
        if (SHOW_DEBUG)
            debug = new Box2DDebugRenderer();

        // preferences
        pref = Gdx.app.getPreferences("preferences");



        // camera & viewport
        cam = new OrthographicCamera(SCREEN_WIDTH / PPM, SCREEN_HEIGHT / PPM);
        viewport = new FillViewport(SCREEN_WIDTH, SCREEN_HEIGHT);

        // world
        world = new World(new Vector2(0, -10), true);
        world.setContactListener(new CONTACT());

        // stage
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // bg music
        sndBg = assetManager.get("sndBg.ogg", Music.class);
        bgSound();

        // numbers
        numbers = assetManager.get("number.atlas", TextureAtlas.class);

        // coinAnimation
        coinAnimation = new Animation(0.04f, assetManager.get("coin.atlas", TextureAtlas.class).getRegions(),
                PlayMode.LOOP);

        // smoke effect
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("effect"), Gdx.files.internal(""));

        coins = new Array<>();
        showScreen("main");
    }

    // showScreen
    void showScreen(String screen) {
        clearScreen();
        this.screen = screen;

        if (screen.equals("main")) { // MAIN
            // load screen
            map = new JsonReader().parse(Gdx.files.internal(screen + ".hmp"));

            Lib.addLayer("sky", map, stage.getRoot());

            // clouds
            clouds = Lib.addLayer("cloud", map, stage.getRoot());
            for (int i = 0; i < clouds.size; i++)
                clouds.get(i).body.setLinearVelocity((float) (-0.05f - Math.random() * 0.5f), 0);

            Lib.addLayer("ground", map, stage.getRoot());

            // menu buttons array
            Array<Act> buttons = new Array<>();

            // btnStart
            buttons.add(Lib.addLayer("btnStart", map, stage.getRoot()).first());

            // sign button


            // btnLeaders


            // sound buttons
            btnSound = Lib.addLayer("btnSound", map, stage.getRoot()).first();
            btnSound.tex = new TextureRegion(assetManager.get(
                    pref.getBoolean("mute", false) ? "btnSound.png" : "btnMute.png", Texture.class));
            buttons.add(btnSound);

            // btnQuit
            buttons.add(Lib.addLayer("btnQuit", map, stage.getRoot()).first());

            // buttons animation
            Vector2 point = stage.screenToStageCoordinates(new Vector2((float) Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight()));
            float animSpeed = 0.3f; // animation speed
            for (int i = 0; i < buttons.size; i++) {
                buttons.get(i).setAlpha(0);
                buttons.get(i).setRotation((float) (Math.random() * 360));
                buttons.get(i).setScale(0.5f);
                buttons.get(i)
                        .addAction(
                                Actions.sequence(Actions.moveTo(point.x - buttons.get(i).getWidth() / 2, point.y
                                        - buttons.get(i).getHeight() / 2), Actions.delay(i * animSpeed * 0.5f), Actions.parallel(
                                        Actions.alpha(1, animSpeed), Actions.rotateTo(0, animSpeed), Actions.scaleTo(1, 1,
                                                animSpeed), Actions.moveTo(buttons.get(i).getX(), buttons.get(i).getY(),
                                                animSpeed, Interpolation.swingOut))));
            }
        } else if (screen.equals("game")) { // GAME
            // load screen
            map = new JsonReader().parse(Gdx.files.internal(screen + ".hmp"));

            // sky
            Lib.addLayer("sky", map, stage.getRoot());

            coins.clear();

            // clouds
            clouds = Lib.addLayer("cloud", map, stage.getRoot());
            for (int i = 0; i < clouds.size; i++)
                clouds.get(i).body.setLinearVelocity((float) (-0.05f - Math.random() * 0.5f), 0);

            // smoke
            Lib.addLayer("smoke", map, stage.getRoot()).first().effect = effect;

            // vulcan
            vulcan = Lib.addLayer("vulcan", map, stage.getRoot()).first();

            // ground
            Lib.addLayer("ground", map, stage.getRoot());

            // txtScore
            txtScore = Lib.addLayer("txtScore", map, stage.getRoot()).first();
            txtScore.setAlpha(0);

            // txtReady
            txtReady = Lib.addLayer("txtReady", map, stage.getRoot()).first();
            txtReady.addAction(Actions.sequence(Actions.delay(1), Actions.alpha(0, 0.2f), new Action() {
                @Override
                public boolean act(float delta) {
                    // show progress time
                    progressBg.addAction(Actions.alpha(1, 0.2f));
                    progressLine.addAction(Actions.alpha(1, 0.2f));
                    progressOver.addAction(Actions.alpha(1, 0.2f));

                    // timer
                    currentTime = TIME;
                    COIN.cancel();
                    TIMER.cancel();
                    Timer.schedule(TIMER, 0, 1);
                    Timer.schedule(COIN, (float) (MIN_SHOW_COIN_INTERVAL + Math.random() * MAX_SHOW_COIN_INTERVAL));
                    return true;
                }
            }));

            // progress
            progressBg = Lib.addLayer("progressBg", map, stage.getRoot()).first();
            progressLine = Lib.addLayer("progressLine", map, stage.getRoot()).first();
            progressOver = Lib.addLayer("progressOver", map, stage.getRoot()).first();
            progressBg.setAlpha(0);
            progressLine.setAlpha(0);
            progressOver.setAlpha(0);

            // btnPause
            btnPause = Lib.addLayer("btnPause", map, stage.getRoot()).first();

            // groupGameOver
            groupGameOver = Lib.addGroup("groupGameOver", map, stage.getRoot());
            groupGameOver.setVisible(false);

            // groupPause
            groupPause = Lib.addGroup("groupPause", map, stage.getRoot());
            groupPause.setVisible(false);
            btnSound = groupPause.findActor("btnSound");
            btnSound.tex = new TextureRegion(assetManager.get(pref.getBoolean("mute", false) ? "btnSound.png" : "btnMute.png",
                    Texture.class));
        }

        // map config
        mapWidth = map.getInt("map_width", 0);
        mapHeight = map.getInt("map_height", 0);
        screenColor = map.getString("map_color", null);

        // stage keyboard focus
        Act a = new Act("");
        stage.addActor(a);
        a.addListener(controlListener);
        stage.setKeyboardFocus(a);

        render();
    }

    // clearScreen
    void clearScreen() {
        screen = "";
        screenColor = null;
        gamePaused = false;
        score = 0;
        TIMER.cancel();
        COIN.cancel();

        // clear world
        if (world != null) {
            world.clearForces();
            world.getJoints(destroyJoints);
            world.getBodies(destroyBodies);
        }
        render();

        // clear stage
        stage.clear();
    }

    @Override
    public void render() {
        // bg music volume
        if (!pref.getBoolean("mute", false) && isForeground && currentVolume < BG_VOLUME) {
            currentVolume += 0.001f;
            sndBg.setVolume(currentVolume);
        }

        // screen color
        if (screenColor != null) {
            Color color = Color.valueOf(screenColor);
            Gdx.gl.glClearColor(color.r, color.g, color.b, 1);
        }

        // clear
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // current screen render
        if (screen.equals("game"))
            renderGame();
        else if (!screen.isEmpty()) {
            // clouds
            for (int i = 0; i < clouds.size; i++)
                if (clouds.get(i).body.getPosition().x * PPM < -clouds.get(i).getWidth() * 0.5f) {
                    clouds.get(i).body.setTransform((mapWidth + clouds.get(i).getWidth() * 0.5f) / PPM,
                            clouds.get(i).body.getPosition().y, clouds.get(i).body.getAngle());
                    clouds.get(i).body.setLinearVelocity((float) (-0.1f - Math.random() * 0.9f), 0);
                }

            // groupRate
            if (groupRate != null)
                groupRate.setPosition(-stage.getRoot().getX(), -stage.getRoot().getY());

            // world render
            world.step(1 / 30f, 8, 3);

            // camera position
            stage.getRoot().setX(
                    MathUtils.clamp((SCREEN_WIDTH - mapWidth) * 0.5f, SCREEN_WIDTH - mapWidth + viewport.getLeftGutterWidth()
                            / ratio, -viewport.getLeftGutterWidth() / ratio));

            stage.getRoot().setY(
                    MathUtils.clamp(SCREEN_HEIGHT * 0.5f - 660,
                            SCREEN_HEIGHT - mapHeight + viewport.getTopGutterHeight() / ratio, -viewport.getTopGutterHeight()
                                    / ratio));
            cam.position.set((SCREEN_WIDTH * 0.5f - stage.getRoot().getX()) / PPM,
                    (SCREEN_HEIGHT * 0.5f - stage.getRoot().getY()) / PPM, 0);
            cam.update();

            // stage render
            stage.act(Math.min(Gdx.graphics.getDeltaTime(), 0.02f));
            stage.draw();
        }

        // destroy
        if (!world.isLocked()) {
            for (int i = 0; i < destroyJoints.size; i++)
                world.destroyJoint(destroyJoints.get(i));
            for (int i = 0; i < destroyBodies.size; i++)
                world.destroyBody(destroyBodies.get(i));
            destroyJoints.clear();
            destroyBodies.clear();
        }

        // debug render
        if (SHOW_DEBUG)
            debug.render(world, cam.combined);
    }

    // renderGame
    void renderGame() {
        if (!gamePaused) {
            // clouds
            for (int i = 0; i < clouds.size; i++)
                if (clouds.get(i).body.getPosition().x * PPM < -clouds.get(i).getWidth() * 0.5f) {
                    clouds.get(i).body.setTransform((mapWidth + clouds.get(i).getWidth() * 0.5f) / PPM,
                            clouds.get(i).body.getPosition().y, clouds.get(i).body.getAngle());
                    clouds.get(i).body.setLinearVelocity((float) (-0.05f - Math.random() * 0.5f), 0);
                }

            // coins
            for (int i = 0; i < coins.size; i++) {
                coins.get(i).setScale(coins.get(i).getScaleX() * 1.005f);
                coins.get(i).setAlpha(Math.min(coins.get(i).getAlpha() + 0.1f, 1));
            }

            // txtScore
            if (txtScore.getAlpha() > 0) {
                txtScore.setY(txtScore.getY() + 5f);
                txtScore.setScale(txtScore.getScaleX() + 0.01f);
                txtScore.setAlpha(Math.max(txtScore.getAlpha() - 0.05f, 0));
            }

            // groups
            groupPause.setPosition(-stage.getRoot().getX(), -stage.getRoot().getY());
            groupGameOver.setPosition(-stage.getRoot().getX(), -stage.getRoot().getY());

            // btnPause
            point = stage.screenToStageCoordinates(new Vector2(Gdx.graphics.getWidth(), 0));
            btnPause.setPosition(point.x - btnPause.getWidth() - 20 - stage.getRoot().getX(), point.y - btnPause.getHeight() - 20
                    - stage.getRoot().getY());

            // render
            world.step(1 / 30f, 8, 3);
            stage.act(Math.min(Gdx.graphics.getDeltaTime(), 0.02f));

            // progress
            point = stage.screenToStageCoordinates(new Vector2(0, 0));
            progressBg.setPosition(point.x + 10 - stage.getRoot().getX(), point.y - progressBg.getHeight() - 10
                    - stage.getRoot().getY());
            progressOver.setPosition(progressBg.getX(), progressBg.getY());
            progressLine.setPosition(point.x + 17 - stage.getRoot().getX(), point.y - progressLine.getHeight() - 17
                    - stage.getRoot().getY());
            progressLine.setOrigin(0, 0);
            progressLine.setScaleX((float) currentTime / TIME);

            // camera position
            stage.getRoot().setX(
                    MathUtils.clamp((SCREEN_WIDTH - mapWidth) * 0.5f, SCREEN_WIDTH - mapWidth + viewport.getLeftGutterWidth()
                            / ratio, -viewport.getLeftGutterWidth() / ratio));

            stage.getRoot().setY(
                    MathUtils.clamp(SCREEN_HEIGHT * 0.5f - 660,
                            SCREEN_HEIGHT - mapHeight + viewport.getTopGutterHeight() / ratio, -viewport.getTopGutterHeight()
                                    / ratio));
            cam.position.set((SCREEN_WIDTH * 0.5f - stage.getRoot().getX()) / PPM,
                    (SCREEN_HEIGHT * 0.5f - stage.getRoot().getY()) / PPM, 0);
            cam.update();
        }

        stage.draw();
    }

    @Override
    public void pause() {
        sndBg.pause();
        isForeground = false;
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
        isForeground = true;

        // finish load assets
        if (!assetManager.update())
            assetManager.finishLoading();

        bgSound();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height);
        ratio = Math.max((float) viewport.getScreenWidth() / SCREEN_WIDTH, (float) viewport.getScreenHeight() / SCREEN_HEIGHT);

        if (!Gdx.graphics.isFullscreen()) {
            currentWidth = width;
            currentHeight = height;
        }
    }

    @Override
    public void dispose() {
        clearScreen();
        batch.dispose();
        stage.dispose();
        assetManager.clear();

        // clear effect
        if (effect != null)
            effect.dispose();

        if (debug != null)
            debug.dispose();

        if (world != null)
            world.dispose();

        System.gc();
    }

    // bgSound
    void bgSound() {
        if (!pref.getBoolean("mute", false) && isForeground) {
            sndBg.setVolume(currentVolume);
            sndBg.setLooping(true);
            sndBg.play();
        }
    }

    // gameOver
    void gameOver() {
        hideElements();
        TIMER.cancel();
        COIN.cancel();

        // sound
        if (!pref.getBoolean("mute", false) && isForeground)
            assetManager.get("sndGameOver.ogg", Sound.class).play(1f);

        showGroup(groupGameOver);
    }

    // showGroup
    void showGroup(Group group) {
        float delay = 0; // delay before show group

        // add score numbers
        String str = String.valueOf(score);
        Array<Act> actors = new Array<>();
        int numbersWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            Act actor = new Act("", 0, 400, numbers.findRegion(str.substring(i, i + 1)));
            actors.add(actor);
            group.addActor(actor);
            numbersWidth += (int) actor.getWidth();
        }

        // set numbers position
        float x_pos = (SCREEN_WIDTH - numbersWidth) / 2;
        for (int i = 0; i < actors.size; i++) {
            actors.get(i).setX(x_pos);
            x_pos += actors.get(i).getWidth();
        }

        // show
        group.setVisible(true);
        SnapshotArray<Actor> allActors = group.getChildren();
        allActors.get(0).addAction(Actions.sequence(Actions.alpha(0, 0), Actions.delay(delay), Actions.alpha(1, 1)));
        for (int i = 1; i < allActors.size; i++) {
            allActors.get(i).setScale(0, 0);
            if (i != allActors.size - 1)
                allActors.get(i).addAction(
                        Actions.sequence(Actions.delay(delay + (i - 1) * 0.2f), Actions.scaleBy(1, 1, 1, Interpolation.elasticOut)));
            else
                allActors.get(i).addAction(
                        Actions.sequence(Actions.delay(delay + (i - 1) * 0.2f), Actions.scaleBy(1, 1, 1, Interpolation.elasticOut), new Action() {
                            @Override
                            public boolean act(float delta) {

                                return true;
                            }
                        }));
        }
    }

    // hideElements
    void hideElements() {
        // remove listener
        btnPause.removeListener(controlListener);

        // hide
        btnPause.addAction(Actions.alpha(0, 0.2f));
        progressBg.addAction(Actions.alpha(0, 0.2f));
        progressLine.addAction(Actions.alpha(0, 0.2f));
        progressOver.addAction(Actions.alpha(0, 0.2f));
    }

    // TIMER
    void TIMER() {
        TIMER = new Task() {
            @Override
            public void run() {
                currentTime--;

                if (currentTime == 0)
                    gameOver();
                else if (isForeground && currentTime <= 3 && !pref.getBoolean("mute", false))
                    assetManager.get("sndTime.ogg", Sound.class).play(0.3f);
            }
        };
    }

    // COIN
    void COIN() {
        COIN = new Task() {
            @Override
            public void run() {
                // add coin
                Act actor = Lib.addLayer("coin", map, stage.getRoot()).first();
                actor.body.setLinearVelocity((float) (-8f + Math.random() * 16f), (float) (12f + Math.random() * 5f));
                actor.animation = coinAnimation;
                actor.setAlpha(0f);
                actor.setZIndex(txtScore.getZIndex());
                actor.addListener(controlListener);
                coins.add(actor);

                // vulcan animation
                vulcan.clearActions();
                vulcan.addAction(Actions.sequence(Actions.scaleTo(1.01f, 1.02f, 0.1f, Interpolation.elasticOut), Actions.scaleTo(1f, 1f, 0.2f)));

                // sound
                if (!pref.getBoolean("mute", false) && isForeground)
                    assetManager.get("sndFire.ogg", Sound.class).play(0.1f);

                // delay next coin
                Timer.schedule(COIN, (float) (MIN_SHOW_COIN_INTERVAL + Math.random() * MAX_SHOW_COIN_INTERVAL));
            }
        };
    }

    // rateDialog
    void rateDialog() {
        if (groupRate == null) {
            groupRate = Lib.addGroup("groupRate", map, stage.getRoot());
            groupRate.addAction(sequence(alpha(0), alpha(1, 0.2f)));

            // sound
            if (!pref.getBoolean("mute", false) && isForeground)
                assetManager.get("sndRate.ogg", Sound.class).play(1);
        }
    }

    // CONTACT
    class CONTACT implements ContactListener {
        @Override
        public void beginContact(Contact contact) {
            Act actor1 = (Act) contact.getFixtureA().getBody().getUserData();
            Act actor2 = (Act) contact.getFixtureB().getBody().getUserData();
            Act otherActor;

            // coin & ground
            if ((actor1.getName().equals("coin") && actor2.getName().equals("ground")) || (actor2.getName().equals("coin") && actor1.getName().equals("ground"))) {
                otherActor = actor1.getName().equals("coin") ? actor1 : actor2;
                destroyBodies.add(otherActor.body);
                otherActor.remove();
                coins.removeIndex(coins.indexOf(otherActor, true));
            }
        }

        @Override
        public void endContact(Contact contact) {
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
        }
    }

    // CONTROL
    class CONTROL extends InputListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (((Act) event.getTarget()).enabled) {
                // each button
                if (event.getTarget().getName().substring(0, Math.min(3, event.getTarget().getName().length())).equals("btn")) {
                    ((Act) event.getTarget()).brightness = BRIGHTNESS_PRESSED;

                    // sound
                    if (!pref.getBoolean("mute", false) && isForeground)
                        assetManager.get("sndBtn.ogg", Sound.class).play(0.9f);
                }

                if (screen.equals("game") && !gamePaused && currentTime > 0) {
                    // coin
                    if (event.getTarget().getName().equals("coin")) {
                        event.getTarget().setVisible(false);

                        // score
                        score += 10;
                        txtScore.setX(event.getTarget().getX());
                        txtScore.setY(event.getTarget().getY());
                        txtScore.setScale(1);
                        txtScore.setAlpha(1);

                        // save score


                        // add time
                        currentTime = Math.min(TIME, currentTime + ADD_TIME);

                        // sound
                        if (!pref.getBoolean("mute", false) && isForeground)
                            assetManager.get("sndCoin.ogg", Sound.class).play(0.5f);

                        return true;
                    }
                }
            }

            return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            super.touchUp(event, x, y, pointer, button);
            if (((Act) event.getTarget()).enabled) {
                // each button
                if (event.getTarget().getName().substring(0, Math.min(3, event.getTarget().getName().length())).equals("btn"))
                    ((Act) event.getTarget()).brightness = 1;

                // if actor in focus
                if (stage.hit(event.getStageX(), event.getStageY(), true) == event.getTarget()) {
                    // btnPause
                    if (event.getTarget().getName().equals("btnPause")) {
                        gamePaused = true;
                        groupPause.setVisible(true);
                        btnPause.setVisible(false);
                        taskDelay = (TIMER.getExecuteTimeMillis() - TimeUtils.nanosToMillis(TimeUtils.nanoTime())) / 1000f;
                        TIMER.cancel();
                        COIN.cancel();
                        return;
                    }





                    // btnStart
                    if (event.getTarget().getName().equals("btnStart")) {
                        showScreen("game");
                        return;
                    }

                    // btnSound
                    if (event.getTarget().getName().equals("btnSound")) {
                        if (pref.getBoolean("mute", false)) {
                            // sound
                            pref.putBoolean("mute", false);
                            pref.flush();
                            btnSound.tex = new TextureRegion(assetManager.get("btnMute.png", Texture.class));
                            bgSound();
                        } else {
                            // mute
                            pref.putBoolean("mute", true);
                            pref.flush();
                            btnSound.tex = new TextureRegion(assetManager.get("btnSound.png", Texture.class));
                            sndBg.pause();
                            currentVolume = 0;
                        }
                        return;
                    }

                    // btnQuit
                    if (event.getTarget().getName().equals("btnQuit")) {
                        if (screen.equals("game"))
                            showScreen("main");
                        else if (!pref.contains("rate"))
                            rateDialog();
                        else
                            Gdx.app.exit();
                        return;
                    }

                    // btnResume
                    if (event.getTarget().getName().equals("btnResume")) {
                        gamePaused = false;
                        groupPause.setVisible(false);
                        btnPause.setVisible(true);
                        if (currentTime > 0) {
                            Timer.schedule(TIMER, taskDelay, 1);
                            Timer.schedule(COIN, (float) (MIN_SHOW_COIN_INTERVAL + Math.random() * MAX_SHOW_COIN_INTERVAL));
                        }
                        return;
                    }

                    // btnRateYes
                    if (event.getTarget().getName().equals("btnRateYes")) {
                        pref.putBoolean("rate", true).flush();
                        Gdx.app.exit();
                        nativePlatform.rate(); // rate
                        return;
                    }

                    // btnRateNo
                    if (event.getTarget().getName().equals("btnRateNo")) {
                        Gdx.app.exit();
                    }
                }
            }
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            super.enter(event, x, y, pointer, fromActor);

            // mouse over button
            if (((Act) event.getTarget()).enabled
                    && event.getTarget().getName().substring(0, Math.min(3, event.getTarget().getName().length())).equals("btn"))
                ((Act) event.getTarget()).brightness = BRIGHTNESS_PRESSED;
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            super.exit(event, x, y, pointer, toActor);

            // mouse out button
            if (event.getTarget().getName().substring(0, Math.min(3, event.getTarget().getName().length())).equals("btn"))
                ((Act) event.getTarget()).brightness = 1;
        }

        @Override
        public boolean keyUp(InputEvent event, int keycode) {
            switch (keycode) {
                case Keys.ESCAPE: // exit from fullscreen mode
                    if (Gdx.graphics.isFullscreen())
                        Gdx.graphics.setWindowedMode(currentWidth, currentHeight);
                    break;
                case Keys.ENTER: // switch to fullscreen mode
                    if (!Gdx.graphics.isFullscreen())
                        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    break;
                case Keys.BACK: // back
                    if (screen.equals("main"))
                        Gdx.app.exit();
                    else if (screen.equals("game"))
                        showScreen("main");
                    break;
            }

            return true;
        }
    }
}