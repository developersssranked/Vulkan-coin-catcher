package com.app.desktop;

import com.app.InterfaceListener;
import com.app.Main;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

// DesktopLauncher
public abstract class DesktopLauncher implements InterfaceListener {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "App"; // window title
        config.width = 800; // default screen width
        config.height = 450; // default screen height

        // fullscreen mode

        // run
        new LwjglApplication(new Main(new DesktopLauncher() {
            @Override
            public void rate() {

            }
        }), config);
    }
}