package org.ngengine.platform;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.ios.IGLESContext;
import com.jme3.system.ios.JmeIosSystem;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.app.Main;
import org.ngengine.platform.jvm.JVMAsyncPlatform;

public final class IosLauncher {
    private NGEApplication app;

    public void start() {
        NGEPlatform.set(new JVMAsyncPlatform());
        JmeSystem.setSystemDelegate(new JmeIosSystem());

        NGEAppRunner runner = Main.main(new String[0]);
        app = runner.app();
        configureIosSettings(app.getSettings());
        runner.start();
    }

    public void frame() {
        if (app == null) {
            return;
        }
        runFrame(app.getJme3App());
    }

    public void update() {
        frame();
    }

    public void resize(int width, int height) {
        if (app == null) {
            return;
        }
        JmeContext context = app.getJme3App().getContext();
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).resizeFramebuffer(width, height);
        } else if (app.getJme3App() instanceof SystemListener) {
            ((SystemListener) app.getJme3App()).reshape(width, height);
        }
    }

    public void stop(boolean waitFor) {
        if (app != null) {
            app.getJme3App().stop(waitFor);
            app = null;
        }
    }

    private static void configureIosSettings(AppSettings settings) {
        settings.setRenderer(AppSettings.ANGLE_GLES3);
        settings.setUseJoysticks(true);
        settings.setVirtualJoystick(AppSettings.VIRTUAL_JOYSTICK_ENABLED);
        settings.setVirtualJoystickDefaultLayout(AppSettings.VIRTUAL_JOYSTICK_LAYOUT_DYNAMIC);
        settings.setOnDeviceJoystickRumble(true);
    }

    private static void runFrame(Application application) {
        JmeContext context = application.getContext();
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).runFrame();
        } else if (application instanceof SystemListener) {
            ((SystemListener) application).update();
        }
    }
}
