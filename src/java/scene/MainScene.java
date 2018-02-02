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
import piengine.object.water.domain.Water;
import piengine.object.water.manager.WaterManager;
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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static piengine.core.base.type.property.ApplicationProperties.get;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_FAR_PLANE;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_FOV;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_DOWN_LIMIT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_SPEED;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_LOOK_UP_LIMIT;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_MOVE_SPEED;
import static piengine.core.base.type.property.PropertyKeys.CAMERA_NEAR_PLANE;
import static piengine.core.base.type.property.PropertyKeys.WATER_WAVE_SPEED;
import static piengine.core.base.type.property.PropertyKeys.WINDOW_HEIGHT;
import static piengine.core.base.type.property.PropertyKeys.WINDOW_WIDTH;
import static piengine.core.input.domain.KeyEventType.PRESS;
import static piengine.core.utils.ColorUtils.createNormalizedColor;
import static piengine.core.utils.ColorUtils.interpolateColors;
import static piengine.object.camera.domain.ProjectionType.PERSPECTIVE;
import static piengine.visual.framebuffer.domain.FramebufferAttachment.COLOR_BUFFER_MULTISAMPLE_ATTACHMENT;
import static piengine.visual.framebuffer.domain.FramebufferAttachment.DEPTH_BUFFER_MULTISAMPLE_ATTACHMENT;
import static piengine.visual.postprocessing.domain.EffectType.ANTIALIAS_EFFECT;

public class MainScene extends Scene {

    private static final int TERRAIN_SCALE = 256;
    private static final int WATER_SCALE = TERRAIN_SCALE / 4;
    private static final float WAVE_SPEED = get(WATER_WAVE_SPEED);
    private static final Color WATER_COLOR = createNormalizedColor(0, 203, 255);
    private static final Vector2i VIEWPORT = new Vector2i(get(WINDOW_WIDTH), get(WINDOW_HEIGHT));

    private static final Color SUN_COLOR = new Color(1.0f, 1.0f, 1.0f);
    private static final Color MIN_BIOM_COLOR = createNormalizedColor(48, 38, 22);
    private static final Color MAX_BIOM_COLOR = createNormalizedColor(20, 255, 39);
    private static final Color[] BIOM_COLORS = {
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.0f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.125f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.375f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.5f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.625f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 0.875f),
            interpolateColors(MIN_BIOM_COLOR, MAX_BIOM_COLOR, 1.0f),
    };

    private final InputManager inputManager;
    private final WindowManager windowManager;
    private final FramebufferManager framebufferManager;
    private final CanvasManager canvasManager;
    private final TerrainManager terrainManager;
    private final WaterManager waterManager;
    private final DirectionalLightManager directionalLightManager;

    private CameraAsset cameraAsset;
    private Framebuffer mainFramebuffer;
    private Canvas mainCanvas;
    private Camera camera;
    private Terrain terrain;
    private Water water;
    private DirectionalLight sun;

    @Wire
    public MainScene(final RenderManager renderManager, final AssetManager assetManager,
                     final InputManager inputManager, final WindowManager windowManager,
                     final FramebufferManager framebufferManager, final CanvasManager canvasManager,
                     final TerrainManager terrainManager, final WaterManager waterManager,
                     final DirectionalLightManager directionalLightManager) {
        super(renderManager, assetManager);

        this.inputManager = inputManager;
        this.windowManager = windowManager;
        this.framebufferManager = framebufferManager;
        this.canvasManager = canvasManager;
        this.terrainManager = terrainManager;
        this.waterManager = waterManager;
        this.directionalLightManager = directionalLightManager;
    }

    @Override
    public void initialize() {
        super.initialize();
        inputManager.addEvent(GLFW_KEY_ESCAPE, PRESS, windowManager::closeWindow);
        inputManager.addEvent(GLFW_KEY_RIGHT_CONTROL, PRESS, () -> {
            cameraAsset.lookingEnabled = !cameraAsset.lookingEnabled;
            windowManager.setCursorVisibility(!cameraAsset.lookingEnabled);
        });
    }

    @Override
    protected void createAssets() {
        terrain = terrainManager.supply(new Vector3f(-TERRAIN_SCALE / 2, 0, -TERRAIN_SCALE / 2), new Vector3f(TERRAIN_SCALE, 10, TERRAIN_SCALE), "heightmap3", BIOM_COLORS);
        water = waterManager.supply(VIEWPORT, new Vector2i(WATER_SCALE, WATER_SCALE), new Vector3f(-TERRAIN_SCALE / 2, 0, -TERRAIN_SCALE / 2), new Vector3f(TERRAIN_SCALE, 0, TERRAIN_SCALE), WATER_COLOR);


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
    public void update(final float delta) {
        water.waveFactor += WAVE_SPEED * delta;
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
                                                .loadWaters(water)
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

    @Override
    public void resize(final int width, final int height) {
        VIEWPORT.x = width;
        VIEWPORT.y = height;

        camera.recalculateProjection();
        framebufferManager.resize(mainFramebuffer, VIEWPORT);
        canvasManager.recreateEffects(mainCanvas);
    }
}
