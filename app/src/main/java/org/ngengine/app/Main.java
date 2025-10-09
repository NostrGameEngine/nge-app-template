package org.ngengine.app;

import org.example.MainComponent;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.components.ComponentManager;
 
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.player.PlayerManagerComponent;


public class Main {   
    public static NGEAppRunner main(String arg[]){
        NGEAppRunner appBuilder = NGEApplication.createApp(
            app -> {
                ComponentManager mng = app.getComponentManager();
                mng.addAndEnableComponent(new PlayerManagerComponent());
                mng.addAndEnableComponent(new NWindowManagerComponent());

                // Add more components as needed
                // ...
            
                mng.addComponent(new MainComponent(), NWindowManagerComponent.class, PlayerManagerComponent.class);
                mng.enableComponent(MainComponent.class);
            }
        );
        return appBuilder;
    }
}
