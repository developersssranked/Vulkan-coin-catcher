package com.app;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;

// custom library
public class Lib {
    static final short[] categoryBits = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, (short) 32768}; // categoryBits

    // addLayer
    static Array<Act> addLayer(String layerName, JsonValue map, Group parent) {
        Array<Act> actors = new Array<>();
        JsonValue images = null;
        JsonValue layer;
        JsonValue objects;
        JsonValue object;
        Act actor;
        TextureRegion tex;
        BodyDef bdef;
        FixtureDef fdef;
        Body body;
        String filePath;

        // images
        if (map.has("images"))
            images = map.get("images");

        // layers
        JsonValue layers = map.get("layers");
        for (int i = 0; i < layers.size; i++) {
            layer = layers.get(i);

            // if add only one layer
            if (layerName != null && !layer.getString("name", "").equals(layerName))
                continue;

            // objects
            if (layer.has("objects")) {
                objects = layer.get("objects");

                for (int j = 0; j < objects.size; j++) {
                    object = objects.get(j);

                    // actor
                    actor = new Act(object.getString("name", layer.getString("name", "")));
                    if (object.has("alpha"))
                        actor.setAlpha(object.getFloat("alpha", 1));
                    if (object.has("angle"))
                        actor.setRotation(-object.getFloat("angle", 0));
                    parent.addActor(actor);
                    actors.add(actor);

                    // if image exists
                    if (object.getInt("image", -1) != -1) {
                        // tex
                        filePath = map.getString("folder", "") + images.getString(object.getInt("image", -1));
                        tex = new TextureRegion(Main.assetManager.get(filePath, Texture.class));
                        actor.tex = tex;

                        // flip
                        actor.flipX = object.getBoolean("flip_x", false) ? -1 : 1;
                        actor.flipY = object.getBoolean("flip_y", false) ? -1 : 1;

                        // size & center point
                        actor.setSize(actor.tex.getRegionWidth(), actor.tex.getRegionHeight());
                        actor.setOrigin(actor.getWidth() * 0.5f, actor.getHeight() * 0.5f);

                        // position
                        actor.setPosition(object.getFloat("x", 0) - tex.getRegionWidth() * 0.5f, map.getInt("map_height", 0)
                                - object.getFloat("y", 0) - tex.getRegionHeight() * 0.5f);

                        // touchable
                        if (object.getBoolean("touchable", false))
                            actor.addListener(Main.controlListener);
                    } else
                        actor.setPosition(object.getFloat("x", 0), map.getInt("map_height", 0) - object.getFloat("y", 0));

                    // physics
                    if (object.getBoolean("physics", false) && !object.getString("shape_type", "").isEmpty()) {
                        // BodyDef
                        bdef = new BodyDef();
                        if (object.getString("body_type", "").equals("dynamic"))
                            bdef.type = BodyType.DynamicBody;
                        else if (object.getString("body_type", "").equals("kinematic"))
                            bdef.type = BodyType.KinematicBody;
                        else
                            bdef.type = BodyType.StaticBody;
                        bdef.angle = -(float) Math.toRadians(object.getFloat("angle", 0));
                        bdef.fixedRotation = object.getBoolean("fixed_rotation", false);
                        bdef.position.set(object.getFloat("x", 0) / Main.PPM,
                                (map.getInt("map_height", 0) - object.getFloat("y", 0)) / Main.PPM);

                        // FixtureDef
                        fdef = new FixtureDef();
                        fdef.density = object.getFloat("density", 0);
                        fdef.friction = object.getFloat("friction", 0);
                        fdef.restitution = object.getFloat("restitution", 0);
                        fdef.isSensor = object.getBoolean("sensor", false);
                        fdef.filter.categoryBits = categoryBits[object.getInt("category_bit", 1) - 1];

                        // maskBits
                        if (object.has("mask_bits")) {
                            int[] bitsArray = object.get("mask_bits").asIntArray();
                            if (bitsArray.length != 0) {
                                fdef.filter.maskBits = 0;
                                for (int k : bitsArray) fdef.filter.maskBits += categoryBits[k - 1];
                            }
                        }

                        // body
                        body = Main.world.createBody(bdef);
                        body.setUserData(actor);
                        actor.body = body;

                        // shape
                        if (object.has("shape_separate")) {
                            for (int k = 0; k < object.get("shape_separate").size; k++) {
                                fdef.shape = getShapeSeparate(object, k);
                                body.createFixture(fdef);
                            }
                        } else {
                            fdef.shape = getShape(object);
                            body.createFixture(fdef);
                        }

                        // linear velocity
                        body.setLinearVelocity(object.getFloat("velocity_x", 0), -object.getFloat("velocity_y", 0));
                    }
                }
            }

            // if add only one layer
            if (layerName != null && layer.getString("name", "").equals(layerName))
                break;
        }

        return actors;
    }

    // addGroup
    static Group addGroup(String name, JsonValue map, Group parent) {
        Group group = new Group();
        group.setName(name);
        parent.addActor(group);
        addLayer(name, map, group);
        return group;
    }

    // getShape
    static Shape getShape(JsonValue object) {
        float[] shape_values = object.get("shape_values").asFloatArray();
        Shape shape = null;

        if (object.getString("shape_type", "").equals("circle")) {
            // circle
            shape = new CircleShape();
            shape.setRadius(shape_values[2] * 0.5f / Main.PPM);
            ((CircleShape) shape).setPosition(new Vector2(shape_values[0] / Main.PPM, -shape_values[1] / Main.PPM));
        } else if (object.getString("shape_type", "").equals("rectangle")) {
            // rectangle
            shape = new PolygonShape();
            ((PolygonShape) shape).setAsBox(shape_values[2] * 0.5f / Main.PPM, shape_values[3] * 0.5f / Main.PPM, new Vector2(
                    shape_values[0] / Main.PPM, -shape_values[1] / Main.PPM), 0);
        } else if (object.getString("shape_type", "").equals("polygon")) {
            // polygon
            shape = new PolygonShape();
            Vector2[] vertices = new Vector2[shape_values.length / 2];
            for (int i = 0; i < shape_values.length / 2; i++)
                vertices[i] = new Vector2(shape_values[i * 2] / Main.PPM, -shape_values[i * 2 + 1] / Main.PPM);
            ((PolygonShape) shape).set(vertices);
        } else if (object.getString("shape_type", "").equals("polyline")) {
            // polyline
            shape = new ChainShape();
            Vector2[] vertices = new Vector2[shape_values.length / 2];
            for (int i = 0; i < shape_values.length / 2; i++)
                vertices[i] = new Vector2(shape_values[i * 2] / Main.PPM, -shape_values[i * 2 + 1] / Main.PPM);
            ((ChainShape) shape).createChain(vertices);
        }

        return shape;
    }

    // getShapeSeparate
    static Shape getShapeSeparate(JsonValue object, int numPolygon) {
        float[] shape_values = object.get("shape_separate").get(numPolygon).asFloatArray();
        PolygonShape shape = new PolygonShape();

        Vector2[] vertices = new Vector2[shape_values.length / 2];
        for (int i = 0; i < shape_values.length / 2; i++)
            vertices[i] = new Vector2(shape_values[i * 2] / Main.PPM, -shape_values[i * 2 + 1] / Main.PPM);
        shape.set(vertices);

        return shape;
    }

    // loadAssets
    static void loadAssets() {
        // auto load assets from assets folder
        FileHandle[] list;
        if (Gdx.app.getType().equals(ApplicationType.Desktop))
            list = Gdx.files.internal("./bin").exists() ? Gdx.files.internal("./bin").list() : Gdx.files.local("").list();
        else
            list = Gdx.files.internal("").list();

        for (FileHandle fileHandle : list)
            if (fileHandle.nameWithoutExtension().equals("sndBg")) {
                Main.assetManager.load(fileHandle.name(), Music.class);
            } else if (fileHandle.extension().equalsIgnoreCase("mp3") || fileHandle.extension().equalsIgnoreCase("wav")
                    || fileHandle.extension().equalsIgnoreCase("ogg")) {
                Main.assetManager.load(fileHandle.name(), Sound.class);
            } else if ((fileHandle.extension().equalsIgnoreCase("png") || fileHandle.extension().equalsIgnoreCase("jpg")
                    || fileHandle.extension().equalsIgnoreCase("jpeg") || fileHandle.extension().equalsIgnoreCase("bmp") || fileHandle
                    .extension().equalsIgnoreCase("gif"))
                    && !new File(fileHandle.pathWithoutExtension() + ".atlas").exists()
                    && !new File(fileHandle.pathWithoutExtension() + ".fnt").exists()
                    && !new File(fileHandle.pathWithoutExtension()).exists()) {
                Main.assetManager.load(fileHandle.name(), Texture.class);
            } else if (fileHandle.extension().equalsIgnoreCase("atlas")) {
                Main.assetManager.load(fileHandle.name(), TextureAtlas.class);
            } else if (fileHandle.extension().equalsIgnoreCase("fnt")) {
                Main.assetManager.load(fileHandle.name(), BitmapFont.class);
            }

        Main.assetManager.finishLoading();
        texturesFilter(); // textures smoothing
    }

    // loadMapAssets
	/*-static void loadMapAssets(JsonValue map) {
		if (map.has("images")) {
			JsonValue images = map.get("images");
			for (int i = 0; i < images.size; i++) {
				String filePath = map.getString("folder", "") + images.getString(i);
				Main.assetManager.load(filePath, Texture.class);

				// Pressed
				filePath = filePath.substring(0, filePath.length() - 4) + "Pressed" + filePath.substring(filePath.length() - 4);
				if (Gdx.files.getFileHandle(filePath, FileType.Internal).exists())
					Main.assetManager.load(filePath, Texture.class);
			}
		}

		Main.assetManager.finishLoading();
		texturesFilter(); // textures smoothing
	}*/

    // texturesFilter
    static void texturesFilter() {
        // atlas
        Array<TextureAtlas> arrayAtlas = new Array<>();
        Array<AtlasRegion> arrayRegion;
        Main.assetManager.getAll(TextureAtlas.class, arrayAtlas);
        for (TextureAtlas atlas : arrayAtlas) {
            arrayRegion = atlas.getRegions();
            for (AtlasRegion region : arrayRegion) {
                region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
        }

        // texture
        Array<Texture> arrayTexture = new Array<>();
        Main.assetManager.getAll(Texture.class, arrayTexture);
        for (Texture texture : arrayTexture) {
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }

        // font
        Array<BitmapFont> arrayFont = new Array<>();
        Main.assetManager.getAll(BitmapFont.class, arrayFont);
        for (BitmapFont font : arrayFont) {
            font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
    }

}