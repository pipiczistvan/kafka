package scene;

import org.joml.Vector2i;
import piengine.core.architecture.scene.domain.Scene;
import piengine.core.input.manager.InputManager;
import piengine.object.asset.manager.AssetManager;
import piengine.visual.render.domain.plan.GuiRenderPlanBuilder;
import piengine.visual.render.domain.plan.RenderPlan;
import piengine.visual.render.manager.RenderManager;
import piengine.visual.window.manager.WindowManager;
import puppeteer.annotation.premade.Wire;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static piengine.core.base.type.property.ApplicationProperties.get;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_VIEWPORT_HEIGHT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_VIEWPORT_WIDTH;
import static piengine.core.input.domain.KeyEventType.PRESS;
import static piengine.core.utils.ColorUtils.BLACK;

public class MainScene extends Scene {

    private static final Vector2i VIEWPORT = new Vector2i(get(CAMERA_VIEWPORT_WIDTH), get(CAMERA_VIEWPORT_HEIGHT));

    private final InputManager inputManager;
    private final WindowManager windowManager;

    @Wire
    public MainScene(final RenderManager renderManager, final AssetManager assetManager,
                     final InputManager inputManager, final WindowManager windowManager) {
        super(renderManager, assetManager);

        this.inputManager = inputManager;
        this.windowManager = windowManager;
    }

    @Override
    public void initialize() {
        super.initialize();
        inputManager.addEvent(GLFW_KEY_ESCAPE, PRESS, windowManager::closeWindow);
    }

    @Override
    public void update(float v) {

    }

    @Override
    protected RenderPlan createRenderPlan() {
        return GuiRenderPlanBuilder
                .createPlan(VIEWPORT)
                .clearScreen(BLACK)
                .render();
    }
}
