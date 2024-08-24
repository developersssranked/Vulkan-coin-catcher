package com.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.Actor;

// custom actor
public class Act extends Actor {
	Body body;
	TextureRegion tex; // texture
	Animation animation; // animation
	float stateTime = 0; // state time
	boolean enabled = true; // enabled or disabled
	float brightness = 1; // brightness
	float flipX = 1; // flip texture horizontally (1 or -1)
	float flipY = 1; // flip texture vertically (1 or -1)
	ParticleEffect effect;

	// name
	public Act(String name) {
		setName(name);
	}

	// name, x, y, tex
	public Act(String name, float x, float y, TextureRegion tex) {
		this.tex = tex;
		setName(name);
		setBounds(x, y, tex.getRegionWidth(), tex.getRegionHeight());
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// frame of animation
		if (animation != null)
			tex = (TextureRegion) animation.getKeyFrame(stateTime);

		// particle effect
		if (effect != null) {
			if (body != null)
				effect.setPosition(body.getPosition().x * Main.PPM, body.getPosition().y * Main.PPM);
			effect.draw(batch, Gdx.graphics.getDeltaTime());
		}

		if (tex != null) {
			// draw
			Color color = getColor();
			batch.setColor(color.r * brightness, color.g * brightness, color.b * brightness, color.a * parentAlpha);
			batch.draw(tex, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX() * flipX, getScaleY()
					* flipY, getRotation());
			batch.setColor(color.r, color.g, color.b, 1);
		}
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		stateTime += delta;

		// size & center point
		if (tex != null) {
			setSize(tex.getRegionWidth(), tex.getRegionHeight());
			setOrigin(getWidth() * 0.5f, getHeight() * 0.5f);
		}

		// position & rotation
		if (body != null) {
			setPosition(body.getPosition().x * Main.PPM - getWidth() * 0.5f, body.getPosition().y * Main.PPM - getHeight() * 0.5f);
			setRotation((float) Math.toDegrees(body.getAngle()));
		}
	}

	@Override
	public boolean remove() {
		clear();
		return super.remove();
	}

	@Override
	protected void finalize() throws Throwable {
		body = null;
		tex = null;
		animation = null;
		super.finalize();
	}

	// getAlpha
	float getAlpha() {
		return getColor().a;
	}

	// setAlpha
	void setAlpha(float alpha) {
		getColor().a = alpha;
	}

}
