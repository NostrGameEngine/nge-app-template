package org.example;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.ngengine.AsyncAssetManager;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NHud;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.store.DataStore;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.input.controls.UnifiedInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.KHRToneMapFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

public class MainComponent extends AbstractComponent implements AsyncAssetLoadingFragment, InputHandlerFragment, LogicFragment, UnifiedInputListener{

    private static final String MOVE_LEFT = "template.move.left";
    private static final String MOVE_RIGHT = "template.move.right";
    private static final String MOVE_FORWARD = "template.move.forward";
    private static final String MOVE_BACK = "template.move.back";
    private static final String[] JOYSTICK_ACTIONS = {MOVE_LEFT, MOVE_RIGHT, MOVE_FORWARD, MOVE_BACK};
    private static final float KEY_MOVE_SPEED = 3f;
    private static final float JOYSTICK_MOVE_SPEED = 3f;
    private static final float TOUCH_MOVE_SPEED = 3f;
    private static final float AXIS_DEAD_ZONE = 0.05f;
    
    private Spatial sky;
    private EnvironmentProbeControl evp;
    private Node characterNode;
    private NHud hud;
    private KHRToneMapFilter toneMapFilter;
    private InputManager inputManager;
    private final Set<Joystick> boundJoysticks = new HashSet<>();
    private final Vector3f keyboardMove = new Vector3f();
    private final Vector3f joystickMove = new Vector3f();
    private final Vector3f touchMove = new Vector3f();
    private int activeTouchPointerId = -1;

    @Override
    public void loadAssetsAsync(ComponentManager mng, AsyncAssetManager assetManager, DataStore assetCache, Consumer<Object> preload){
        // load resources
        sky = SkyFactory.createSky(assetManager, "Sky/citrus_orchard_puresky_4k.hdr", EnvMapType.EquirectMap);
        evp = new EnvironmentProbeControl(assetManager, 256);

        // Tag sky for environment baking
        EnvironmentProbeControl.tagGlobal(sky);

        // load character model
        characterNode = new Node("CharacterNode");
        Geometry characterGeom = new Geometry("MyCharacter", new Box(1f,1f,1f));
        characterNode.attachChild(characterGeom);

        // set up material for character
        Material characterMat = new Material(assetManager, Materials.PBR);
        characterMat.setColor("BaseColor", ColorRGBA.White);
        characterMat.setFloat("Metallic", 1.0f);
        characterMat.setFloat("Roughness", 0.0f);
        characterGeom.setMaterial(characterMat);       

        // trigger preloading
        //  this will make the engine start loading these assets asap, even before the component is ready
        preload.accept(sky);
        preload.accept(characterGeom);

    }

    @Override
    public void onEnable(ComponentManager mng, boolean firstTime) {
        inputManager = mng.getInstanceOf(InputManager.class);

        // the global ViewPortManager is used to access and manage viewports in the application
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        
        // The main viewport represent the primary view on the 3d scene
        ViewPort vp = vpm.getMainSceneViewPort();

        // filter post processor is using to append filters to the viewport
        FilterPostProcessor fpp = vpm.getFilterPostProcessor(vpm.getMainSceneViewPort());
        toneMapFilter = new KHRToneMapFilter();
        fpp.addFilter(toneMapFilter);


        // The rootNode of the scene
        Node rootNode = vpm.getRootNode(vp);

        // Compose the scene
        rootNode.attachChild(sky);
        rootNode.addControl(evp);
        rootNode.attachChild(characterNode);     

        // show a simple hud
        NWindowManagerComponent windowManager = mng.getComponent(NWindowManagerComponent.class);
        hud = windowManager.showWindow(NHud.class);
        NRow topRow = hud.getTop();
        NLabel label = new NLabel("Use WASD or click around to move the cube");
        topRow.addChild(label);            
    
    }

    @Override
    public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
        float pressed = evt.isPressed() ? 1f : 0f;
        if(evt.getKeyCode() == KeyInput.KEY_W){
            keyboardMove.z = -pressed;
        } else if(evt.getKeyCode() == KeyInput.KEY_S) {
            keyboardMove.z = pressed;
        } else if(evt.getKeyCode() == KeyInput.KEY_A) {
            keyboardMove.x = -pressed;
        } else if(evt.getKeyCode() == KeyInput.KEY_D) {
            keyboardMove.x = pressed;
        }
    }

    @Override
    public void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {
        int x = evt.getX();
        int y = evt.getY();
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        
        // move toward click
        Vector2f center = new Vector2f(vp.getCamera().getWidth()/2, vp.getCamera().getHeight()/2);
        center.subtractLocal(x, y).normalizeLocal().multLocal(-0.1f);
        moveCharacter(center.x, center.y, 0);
        
        
    }

    @Override
    public void onTouchEvent(ComponentManager mng, TouchEvent evt) {
        if (evt.getType() == TouchEvent.Type.DOWN && activeTouchPointerId == -1) {
            activeTouchPointerId = evt.getPointerId();
            updateTouchMove(mng, evt.getX(), evt.getY());
        } else if (evt.getType() == TouchEvent.Type.MOVE && evt.getPointerId() == activeTouchPointerId) {
            updateTouchMove(mng, evt.getX(), evt.getY());
        } else if (evt.getType() == TouchEvent.Type.UP && evt.getPointerId() == activeTouchPointerId) {
            activeTouchPointerId = -1;
            touchMove.set(0, 0, 0);
        }
    }

    @Override
    public void onDisable(ComponentManager mng) {
        // clean up 
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        Node rootNode = vpm.getRootNode(vp);
        rootNode.detachChild(sky);
        rootNode.removeControl(evp);
        rootNode.detachChild(characterNode);
        vpm.getFilterPostProcessor(vp).removeFilter(toneMapFilter);
        toneMapFilter = null;
        hud.close();
        hud = null;
        unbindJoystickActions();
        keyboardMove.set(0, 0, 0);
        joystickMove.set(0, 0, 0);
        touchMove.set(0, 0, 0);
        activeTouchPointerId = -1;
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        Vector3f move = keyboardMove.add(joystickMove).addLocal(touchMove);
        if (move.lengthSquared() > 1f) {
            move.normalizeLocal();
        }
        if (move.lengthSquared() > 0f) {
            moveCharacter(move.x * JOYSTICK_MOVE_SPEED * tpf, 0, move.z * JOYSTICK_MOVE_SPEED * tpf);
        }
    }

    @Override
    public void onInputAction(ComponentManager mng, String action, boolean toggled, float value, InputEvent<?> event, float tpf) {
    }

    @Override
    public void onUnifiedInput(String action, boolean toggled, float value, InputEvent<?> event, float tpf) {
        float axisValue = Math.abs(value) > AXIS_DEAD_ZONE ? Math.abs(value) : 0f;
        if (MOVE_LEFT.equals(action)) {
            if (axisValue > 0f || joystickMove.x < 0f) {
                joystickMove.x = -axisValue;
            }
        } else if (MOVE_RIGHT.equals(action)) {
            if (axisValue > 0f || joystickMove.x > 0f) {
                joystickMove.x = axisValue;
            }
        } else if (MOVE_FORWARD.equals(action)) {
            if (axisValue > 0f || joystickMove.z < 0f) {
                joystickMove.z = -axisValue;
            }
        } else if (MOVE_BACK.equals(action)) {
            if (axisValue > 0f || joystickMove.z > 0f) {
                joystickMove.z = axisValue;
            }
        }
    }

    @Override
    public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions inputActions, InputDevice device) {
        if (device instanceof Joystick) {
            this.inputManager = inputManager;
            Joystick joystick = (Joystick) device;
            if (boundJoysticks.add(joystick)) {
                bindJoystick(joystick);
            }
        }
    }

    @Override
    public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions inputActions, InputDevice device) {
        if (device instanceof Joystick) {
            this.inputManager = inputManager;
            if (boundJoysticks.remove(device)) {
                rebindJoystickActions();
            }
        }
    }

    private void moveCharacter(float x, float y, float z) {
        if (characterNode != null) {
            characterNode.move(x, y, z);
        }
    }

    private void updateTouchMove(ComponentManager mng, float x, float y) {
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        float centerX = vp.getCamera().getWidth() * 0.5f;
        float centerY = vp.getCamera().getHeight() * 0.5f;
        float dx = x - centerX;
        float dy = y - centerY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 1f) {
            touchMove.set(0, 0, 0);
            return;
        }
        touchMove.set(dx / length, 0, -dy / length).multLocal(TOUCH_MOVE_SPEED / JOYSTICK_MOVE_SPEED);
    }

    private void rebindJoystickActions() {
        if (inputManager == null) {
            return;
        }

        clearJoystickMappings();
        for (Joystick joystick : boundJoysticks) {
            bindJoystick(joystick);
        }
    }

    private void bindJoystick(Joystick joystick) {
        bindJoystickAction(MOVE_LEFT, joystick, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, true);
        bindJoystickAction(MOVE_RIGHT, joystick, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, false);
        bindJoystickAction(MOVE_FORWARD, joystick, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, true);
        bindJoystickAction(MOVE_BACK, joystick, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, false);
    }

    private void bindJoystickAction(String action, Joystick joystick, String axis, boolean negative) {
        if (joystick.getAxis(axis) == null) {
            return;
        }

        Trigger trigger = new JoyAxisTrigger(joystick, axis, negative);
        inputManager.addMapping(action, trigger);
        inputManager.addListener(this, action);
    }

    private void unbindJoystickActions() {
        clearJoystickMappings();
        boundJoysticks.clear();
    }

    private void clearJoystickMappings() {
        if (inputManager == null) {
            return;
        }

        inputManager.removeListener(this);
        for (String action : JOYSTICK_ACTIONS) {
            if (inputManager.hasMapping(action)) {
                inputManager.deleteMapping(action);
            }
        }
    }



    
}
