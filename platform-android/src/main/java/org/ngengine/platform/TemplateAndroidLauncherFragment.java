package org.ngengine.platform;

import android.content.Context;
import org.ngengine.app.Main;
import org.ngengine.platform.android.AndroidThreadedPlatform;

public class TemplateAndroidLauncherFragment extends AndroidLauncherFragment {

    public TemplateAndroidLauncherFragment() {
        super(Main::main, TemplateAndroidLauncherFragment::createPlatform);
    }

    private static NGEPlatform createPlatform(Context context) {
        return new AndroidThreadedPlatform(context);
    }
}
