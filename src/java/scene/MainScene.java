package scene;

import org.joml.Vector2i;
import org.joml.Vector3f;
import piengine.core.architecture.scene.domain.Scene;
import piengine.core.base.type.color.Color;
import piengine.core.input.manager.InputManager;
import piengine.core.utils.ColorUtils;
import piengine.object.asset.manager.AssetManager;
import piengine.object.asset.plan.GuiRenderAssetContextBuilder;
import piengine.object.asset.plan.WorldRenderAssetContextBuilder;
import piengine.object.camera.asset.CameraAsset;
import piengine.object.camera.asset.CameraAssetArgument;
import piengine.object.camera.domain.Camera;
import piengine.object.camera.domain.CameraAttribute;
import piengine.object.camera.domain.FirstPersonCamera;
import piengine.object.canvas.domain.Canvas;
import piengine.object.canvas.manager.CanvasManager;
import piengine.object.terrain.domain.Terrain;
import piengine.object.terrain.manager.TerrainManager;
import piengine.visual.framebuffer.domain.Framebuffer;
import piengine.visual.framebuffer.manager.FramebufferManager;
import piengine.visual.lighting.directional.light.domain.DirectionalLight;
import piengine.visual.lighting.directional.light.manager.DirectionalLightManager;
import piengine.visual.render.domain.plan.GuiRenderPlanBuilder;
import piengine.visual.render.domain.plan.RenderPlan;
import piengine.visual.render.domain.plan.WorldRenderPlanBuilder;
import piengine.visual.render.manager.RenderManager;
import piengine.visual.window.manager.WindowManager;
import puppeteer.annotation.premade.Wire;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static piengine.core.base.type.property.ApplicationProperties.get;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_FAR_PLANE;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_FOV;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_DOWN_LIMIT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_SPEED;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_UP_LIMIT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_MOVE_SPEED;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_NEAR_PLANE;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_VIEWPORT_HEIGHT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_VIEWPORT_WIDTH;
import static piengine.core.input.domain.KeyEventType.PRESS;
import static piengine.core.utils.ColorUtils.createNormalizedColor;
import static piengine.core.utils.ColorUtils.interpolateColors;
import static piengine.object.camera.domain.ProjectionType.PERSPECTIVE;
import static piengine.visual.framebuffer.domain.FramebufferAttachment.COLOR_BUFFER_MULTISAMPLE_ATTACHMENT;
import static piengine.visual.framebuffer.domain.FramebufferAttachment.DEPTH_BUFFER_MULTISAMPLE_ATTACHMENT;
import static piengine.visual.postprocessing.domain.EffectType.ANTIALIAS_EFFECT;

public class MainScene extends Scene {

    private static final int TERRAIN_SCALE = 512;
    private static final Vector2i VIEWPORT = new Vector2i(get(CAMERA_VIEWPORT_WIDTH), get(CAMERA_VIEWPORT_HEIGHT));

    private static final Color SUN_COLOR = new Color(1.0f, 1.0f, 1.0f);
    private static final Color MIN_BIOM_COLOR = createNormalizedColor(0, 255, 200);
    private static final Color MAX_BIOM_COLOR = createNormalizedColor(100, 255, 255);
    private static final Color[] BIOM_COLORS = {
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.0f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.25f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.5f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.75f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 1.0f),
    };

    private final InputManager inputManager;
    private final WindowManager windowManager;
    private final FramebufferManager framebufferManager;
    private final CanvasManager canvasManager;
    private final TerrainManager terrainManager;
    private final DirectionalLightManager directionalLightManager;

    private CameraAsset cameraAsset;
    private Framebuffer mainFramebuffer;
    private Canvas mainCanvas;
    private Camera camera;
    private Terrain terrain;
    private DirectionalLight sun;

    @Wire
    public MainScene(final RenderManager renderManager, final AssetManager assetManager,
                     final InputManager inputManager, final WindowManager windowManager,
                     final FramebufferManager framebufferManager, final CanvasManager canvasManager,
                     final TerrainManager terrainManager, final DirectionalLightManager directionalLightManager) {
        super(renderManager, assetManager);

        this.inputManager = inputManager;
        this.windowManager = windowManager;
        this.framebufferManager = framebufferManager;
        this.canvasManager = canvasManager;
        this.terrainManager = terrainManager;
        this.directionalLightManager = directionalLightManager;
    }

    @Override
    public void initialize() {
        super.initialize();
        inputManager.addEvent(GLFW_KEY_ESCAPE, PRESS, windowManager::closeWindow);
    }

    @Override
    protected void createAssets() {
        terrain = terrainManager.supply(new Vector3f(-TERRAIN_SCALE / 2, 0, -TERRAIN_SCALE / 2), new Vector3f(TERRAIN_SCALE, 40, TERRAIN_SCALE), "heightmap4", BIOM_COLORS);

        cameraAsset = createAsset(CameraAsset.class, new CameraAssetArgument(
                terrain,
                get(CAMERA_LOOK_UP_LIMIT),
                get(CAMERA_LOOK_DOWN_LIMIT),
                get(CAMERA_LOOK_SPEED),
                get(CAMERA_MOVE_SPEED)));

        camera = new FirstPersonCamera(cameraAsset, VIEWPORT, new CameraAttribute(get(CAMERA_FOV), get(CAMERA_NEAR_PLANE), get(CAMERA_FAR_PLANE)), PERSPECTIVE);

        mainFramebuffer = framebufferManager.supply(VIEWPORT, COLOR_BUFFER_MULTISAMPLE_ATTACHMENT, DEPTH_BUFFER_MULTISAMPLE_ATTACHMENT);

        mainCanvas = canvasManager.supply(this, mainFramebuffer, ANTIALIAS_EFFECT);

        sun = directionalLightManager.supply(this, SUN_COLOR, camera, new Vector2i(2048));
        sun.setPosition(1000, 1000, 300);
    }

    @Override
    public void update(float v) {

    }

    @Override
    protected RenderPlan createRenderPlan() {
        return GuiRenderPlanBuilder
                .createPlan(VIEWPORT)
                .bindFrameBuffer(
                        mainFramebuffer,
                        WorldRenderPlanBuilder
                                .createPlan(camera)
                                .loadAssetContext(
                                        WorldRenderAssetContextBuilder
                                                .create()
                                                .loadTerrains(terrain)
                                                .loadDirectionalLights(sun)
                                                .build()
                                )
                                .clearScreen(ColorUtils.BLACK)
                                .render()
                )
                .loadAssetContext(
                        GuiRenderAssetContextBuilder
                                .create()
                                .loadCanvases(mainCanvas)
                                .build()
                )
                .clearScreen(ColorUtils.BLACK)
                .render();
    }
}
