package cope.cosmos.client.features.modules.visual;

import cope.cosmos.asm.mixins.accessor.IEntityRenderer;
import cope.cosmos.asm.mixins.accessor.IRenderGlobal;
import cope.cosmos.asm.mixins.accessor.IRenderManager;
import cope.cosmos.asm.mixins.accessor.IShaderGroup;
import cope.cosmos.client.events.client.SettingUpdateEvent;
import cope.cosmos.client.events.render.entity.RenderCrystalEvent;
import cope.cosmos.client.events.render.entity.RenderLivingEntityEvent;
import cope.cosmos.client.events.render.entity.tile.RenderTileEntityEvent;
import cope.cosmos.client.events.render.entity.ShaderColorEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.modules.client.ColorsModule;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.client.shader.shaders.DotShader;
import cope.cosmos.client.shader.shaders.FillShader;
import cope.cosmos.client.shader.shaders.OutlineShader;
import cope.cosmos.client.shader.shaders.RainbowOutlineShader;
import cope.cosmos.util.string.ColorUtil;
import cope.cosmos.util.entity.EntityUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * @author linustouchtips
 * @since 07/21/2021
 */
public class ESPModule extends Module {
    public static ESPModule INSTANCE;

    public ESPModule() {
        super("ESP", Category.VISUAL, "Allows you to see entities through walls");
        INSTANCE = this;
    }

    public static Setting<Mode> mode = new Setting<>("Mode", Mode.SHADER).setDescription("The mode for the render style");
    public static Setting<FragmentShader> shader = new Setting<>("Shader", FragmentShader.OUTLINE).setDescription("The shader to draw on the entity").setVisible(() -> mode.getValue().equals(Mode.SHADER));
    public static Setting<Double> width = new Setting<>("Width", 0.0, 1.25, 5.0, 1).setDescription( "Line width for the visual").setVisible(() -> !mode.getValue().equals(Mode.SHADER));

    // entities
    public static Setting<Boolean> players = new Setting<>("Players", true).setDescription("Highlight players");
    public static Setting<Boolean> passives = new Setting<>("Passives", true).setDescription("Highlight passives");
    public static Setting<Boolean> neutrals = new Setting<>("Neutrals", true).setDescription("Highlight neutrals");
    public static Setting<Boolean> hostiles = new Setting<>("Hostiles", true).setDescription("Highlight hostiles");
    public static Setting<Boolean> items = new Setting<>("Items", true).setDescription("Highlight items");
    public static Setting<Boolean> crystals = new Setting<>("Crystals", true).setDescription("Highlight crystals");
    public static Setting<Boolean> vehicles = new Setting<>("Vehicles", true).setDescription("Highlight vehicles");

    // storages
    public static Setting<Boolean> chests = new Setting<>("Chests", true).setDescription("Highlight chests");
    public static Setting<Boolean> enderChests = new Setting<>("EnderChests", true).setDescription("Highlight chests");
    public static Setting<Boolean> shulkers = new Setting<>("Shulkers", true).setDescription("Highlight shulkers");
    public static Setting<Boolean> hoppers = new Setting<>("Hoppers", true).setDescription("Highlight hoppers");
    public static Setting<Boolean> furnaces = new Setting<>("Furnaces", true).setDescription("Highlight furnaces");

    // framebuffer
    private Framebuffer framebuffer;
    private int lastScaleFactor;
    private int lastScaleWidth;
    private int lastScaleHeight;

    // shaders
    private final OutlineShader outlineShader = new OutlineShader();
    private final RainbowOutlineShader rainbowOutlineShader = new RainbowOutlineShader();
    private final DotShader dotShader = new DotShader();
    private final FillShader fillShader = new FillShader();

    @Override
    public void onUpdate() {
        if (mode.getValue().equals(Mode.GLOW)) {
            // set all entities in the world glowing
            mc.world.loadedEntityList.forEach(entity -> {
                if (entity != null && !entity.equals(mc.player) && hasHighlight(entity)) {
                    entity.setGlowing(true);
                }
            });

            // get the shaders
            ShaderGroup outlineShaderGroup = ((IRenderGlobal) mc.renderGlobal).getEntityOutlineShader();
            List<Shader> shaders = ((IShaderGroup) outlineShaderGroup).getListShaders();

            // update the shader radius
            shaders.forEach(shader -> {
                ShaderUniform outlineRadius = shader.getShaderManager().getShaderUniform("Radius");

                if (outlineRadius != null) {
                    outlineRadius.set(width.getValue().floatValue());
                }
            });
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // remove glow effect from all entities
        if (mode.getValue().equals(Mode.GLOW)) {
            mc.world.loadedEntityList.forEach(entity -> {
                if (entity != null && entity.isGlowing()) {
                    entity.setGlowing(false);
                }
            });
        }
    }

    @SubscribeEvent
    public void onSettingUpdate(SettingUpdateEvent event) {
        if (event.getSetting().equals(mode) && !event.getSetting().getValue().equals(Mode.GLOW)) {
            // remove glow effect from all entities
            mc.world.loadedEntityList.forEach(entity -> {
                if (entity != null && entity.isGlowing()) {
                    entity.setGlowing(false);
                }
            });
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)) {
            if (mode.getValue().equals(Mode.SHADER)) {
                GlStateManager.enableAlpha();
                GlStateManager.pushMatrix();
                GlStateManager.pushAttrib();

                // delete our old framebuffer, we'll create a new one
                if (framebuffer != null) {
                    framebuffer.framebufferClear();

                    // resolution info
                    ScaledResolution scaledResolution = new ScaledResolution(mc);

                    if (lastScaleFactor != scaledResolution.getScaleFactor()|| lastScaleWidth != scaledResolution.getScaledWidth() || lastScaleHeight != scaledResolution.getScaledHeight()) {
                        framebuffer.deleteFramebuffer();

                        // create a new framebuffer
                        framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
                        framebuffer.framebufferClear();
                    }

                    // update scale info
                    lastScaleFactor = scaledResolution.getScaleFactor();
                    lastScaleWidth = scaledResolution.getScaledWidth();
                    lastScaleHeight = scaledResolution.getScaledHeight();
                }

                else {
                    // create a new framebuffer
                    framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
                }

                // bind our new framebuffer (i.e. set it as the current active buffer)
                framebuffer.bindFramebuffer(false);

                // prevent entity shadows from rendering
                boolean previousShadows = mc.gameSettings.entityShadows;
                mc.gameSettings.entityShadows = false;

                // https://hackforums.net/showthread.php?tid=4811280
                ((IEntityRenderer) mc.entityRenderer).setupCamera(event.getPartialTicks(), 0);

                // draw all entities
                mc.world.loadedEntityList.forEach(entity -> {
                    if (entity != null && entity != mc.player && hasHighlight(entity)) {
                        mc.getRenderManager().renderEntityStatic(entity, event.getPartialTicks(), true);
                    }
                });

                // draw all storages
                mc.world.loadedTileEntityList.forEach(tileEntity -> {
                    if (tileEntity != null && hasStorageHighlight(tileEntity)) {
                        // get our render offsets.
                        double renderX = ((IRenderManager) mc.getRenderManager()).getRenderX();
                        double renderY = ((IRenderManager) mc.getRenderManager()).getRenderY();
                        double renderZ = ((IRenderManager) mc.getRenderManager()).getRenderZ();

                        TileEntityRendererDispatcher.instance.render(tileEntity, tileEntity.getPos().getX() - renderX, tileEntity.getPos().getY() - renderY, tileEntity.getPos().getZ() - renderZ, mc.getRenderPartialTicks());
                    }
                });

                // reset shadows
                mc.gameSettings.entityShadows = previousShadows;

                GlStateManager.enableBlend();
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                // rebind the mc framebuffer
                framebuffer.unbindFramebuffer();
                mc.getFramebuffer().bindFramebuffer(true);

                // remove lighting
                mc.entityRenderer.disableLightmap();
                RenderHelper.disableStandardItemLighting();

                GlStateManager.pushMatrix();

                // draw the rainbow shader
                if (!ColorsModule.rainbow.getValue().equals(ColorsModule.Rainbow.NONE)) {
                    switch (shader.getValue()) {
                        case DOTTED:
                            dotShader.startShader();
                            break;
                        case OUTLINE:
                            rainbowOutlineShader.startShader();
                            break;
                        case OUTLINE_FILL:
                            fillShader.startShader();
                            break;
                    }
                }

                // draw the shader
                else {
                    switch (shader.getValue()) {
                        case DOTTED:
                            dotShader.startShader();
                            break;
                        case OUTLINE:
                            outlineShader.startShader();
                            break;
                        case OUTLINE_FILL:
                            fillShader.startShader();
                            break;
                    }
                }

                // prepare overlay render
                mc.entityRenderer.setupOverlayRendering();

                ScaledResolution scaledResolution = new ScaledResolution(mc);

                // draw the framebuffer
                glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
                glBegin(GL_QUADS);
                glTexCoord2d(0, 1);
                glVertex2d(0, 0);
                glTexCoord2d(0, 0);
                glVertex2d(0, scaledResolution.getScaledHeight());
                glTexCoord2d(1, 0);
                glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
                glTexCoord2d(1, 1);
                glVertex2d(scaledResolution.getScaledWidth(), 0);
                glEnd();

                // stop drawing our shader
                glUseProgram(0);
                glPopMatrix();

                // reset lighting
                mc.entityRenderer.enableLightmap();

                GlStateManager.popMatrix();
                GlStateManager.popAttrib();
            }
        }
    }

    @SubscribeEvent
    public void onRenderEntity(RenderLivingEntityEvent event) {
        if (mode.getValue().equals(Mode.OUTLINE)) {
            if (hasHighlight(event.getEntityLivingBase())) {
                
                // setup framebuffer
                if (mc.getFramebuffer().depthBuffer > -1) {

                    // delete old framebuffer extensions
                    EXTFramebufferObject.glDeleteRenderbuffersEXT(mc.getFramebuffer().depthBuffer);

                    // generates a new render buffer ID for the depth and stencil extension
                    int stencilFrameBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();

                    // bind a new render buffer
                    EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                    // add the depth and stencil extension
                    EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight);

                    // add the depth and stencil attachment
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                    // reset depth buffer
                    mc.getFramebuffer().depthBuffer = -1;
                }

                // begin drawing the stencil
                glPushAttrib(GL_ALL_ATTRIB_BITS);
                glDisable(GL_ALPHA_TEST);
                glDisable(GL_TEXTURE_2D);
                glDisable(GL_LIGHTING);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glLineWidth(width.getValue().floatValue());
                glEnable(GL_LINE_SMOOTH);
                glEnable(GL_STENCIL_TEST);
                glClear(GL_STENCIL_BUFFER_BIT);
                glClearStencil(0xF);
                glStencilFunc(GL_NEVER, 1, 0xF);
                glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                // render the entity model
                event.getModelBase().render(event.getEntityLivingBase(), event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

                // fill the entity model
                glStencilFunc(GL_NEVER, 0, 0xF);
                glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

                // render the entity model
                event.getModelBase().render(event.getEntityLivingBase(), event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

                // outline the entity model
                glStencilFunc(GL_EQUAL, 1, 0xF);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                // color the stencil and clear the depth
                glColor4d(ColorUtil.getPrimaryColor().getRed() / 255F, ColorUtil.getPrimaryColor().getGreen() / 255F, ColorUtil.getPrimaryColor().getBlue() / 255F, ColorUtil.getPrimaryColor().getAlpha() / 255F);
                glDepthMask(false);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_POLYGON_OFFSET_LINE);
                glPolygonOffset(3, -2000000);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

                // render the entity model
                event.getModelBase().render(event.getEntityLivingBase(), event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

                // reset stencil
                glPolygonOffset(-3, 2000000);
                glDisable(GL_POLYGON_OFFSET_LINE);
                glEnable(GL_DEPTH_TEST);
                glDepthMask(true);
                glDisable(GL_STENCIL_TEST);
                glDisable(GL_LINE_SMOOTH);
                glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
                glEnable(GL_BLEND);
                glEnable(GL_LIGHTING);
                glEnable(GL_TEXTURE_2D);
                glEnable(GL_ALPHA_TEST);
                glPopAttrib();
            }
        }
    }

    @SubscribeEvent
    public void onRenderCrystal(RenderCrystalEvent.RenderCrystalPostEvent event) {
        if (mode.getValue().equals(Mode.OUTLINE)) {
            if (crystals.getValue()) {
                // calculate model rotations
                float rotation = event.getEntityEnderCrystal().innerRotation + event.getPartialTicks();
                float rotationMoved = MathHelper.sin(rotation * 0.2F) / 2 + 0.5F;
                rotationMoved += StrictMath.pow(rotationMoved, 2);

                glPushMatrix();
                
                // translate module to position
                glTranslated(event.getX(), event.getY(), event.getZ());
                glLineWidth(1 + width.getValue().floatValue());

                // render the entity model
                if (event.getEntityEnderCrystal().shouldShowBottom()) {
                    event.getModelBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                else {
                    event.getModelNoBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                // setup framebuffer
                if (mc.getFramebuffer().depthBuffer > -1) {

                    // delete old framebuffer extensions
                    EXTFramebufferObject.glDeleteRenderbuffersEXT(mc.getFramebuffer().depthBuffer);

                    // generates a new render buffer ID for the depth and stencil extension
                    int stencilFrameBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();

                    // bind a new render buffer
                    EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                    // add the depth and stencil extension
                    EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight);

                    // add the depth and stencil attachment
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                    // reset depth buffer
                    mc.getFramebuffer().depthBuffer = -1;
                }

                // begin drawing the stencil
                glPushAttrib(GL_ALL_ATTRIB_BITS);
                glDisable(GL_ALPHA_TEST);
                glDisable(GL_TEXTURE_2D);
                glDisable(GL_LIGHTING);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glLineWidth(1 + width.getValue().floatValue());
                glEnable(GL_LINE_SMOOTH);
                glEnable(GL_STENCIL_TEST);
                glClear(GL_STENCIL_BUFFER_BIT);
                glClearStencil(0xF);
                glStencilFunc(GL_NEVER, 1, 0xF);
                glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                // render the entity model
                if (event.getEntityEnderCrystal().shouldShowBottom()) {
                    event.getModelBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                else {
                    event.getModelNoBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                // fill the entity model
                glStencilFunc(GL_NEVER, 0, 0xF);
                glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

                // render the entity model
                if (event.getEntityEnderCrystal().shouldShowBottom()) {
                    event.getModelBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                else {
                    event.getModelNoBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                // outline the entity model
                glStencilFunc(GL_EQUAL, 1, 0xF);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                // color the stencil and clear the depth
                glDepthMask(false);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_POLYGON_OFFSET_LINE);
                glPolygonOffset(3, -2000000);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                glColor4d(ColorUtil.getPrimaryColor().getRed() / 255F, ColorUtil.getPrimaryColor().getGreen() / 255F, ColorUtil.getPrimaryColor().getBlue() / 255F, ColorUtil.getPrimaryColor().getAlpha() / 255F);

                // render the entity model
                if (event.getEntityEnderCrystal().shouldShowBottom()) {
                    event.getModelBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                else {
                    event.getModelNoBase().render(event.getEntityEnderCrystal(), 0, rotation * 3, rotationMoved * 0.2F, 0, 0, 0.0625F);
                }

                // reset stencil
                glPolygonOffset(-3, 2000000);
                glDisable(GL_POLYGON_OFFSET_LINE);
                glEnable(GL_DEPTH_TEST);
                glDepthMask(true);
                glDisable(GL_STENCIL_TEST);
                glDisable(GL_LINE_SMOOTH);
                glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
                glEnable(GL_BLEND);
                glEnable(GL_LIGHTING);
                glEnable(GL_TEXTURE_2D);
                glEnable(GL_ALPHA_TEST);
                glPopAttrib();

                glPopMatrix();
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public void onRenderTileEntity(RenderTileEntityEvent event) {
        if (mode.getValue().equals(Mode.OUTLINE)) {
            if (hasStorageHighlight(event.getTileEntity())) {

                // hotbar render sets all positions to 0
                boolean hotbarRender = event.getX() == 0 && event.getY() == 0 && event.getZ() == 0;

                // check if it's rendering in hotbar
                if (!hotbarRender) {
                    if (TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()) != null) {

                        // cancel rendering
                        event.setCanceled(true);

                        glPushMatrix();

                        // render the tile entity model
                        if (event.getTileEntity().hasFastRenderer()) {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).renderTileEntityFast(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial(), event.getBuffer().getBuffer());
                        }

                        else {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).render(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial());
                        }

                        // setup framebuffer
                        if (mc.getFramebuffer().depthBuffer > -1) {

                            // delete old framebuffer extensions
                            EXTFramebufferObject.glDeleteRenderbuffersEXT(mc.getFramebuffer().depthBuffer);

                            // generates a new render buffer ID for the depth and stencil extension
                            int stencilFrameBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();

                            // bind a new render buffer
                            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                            // add the depth and stencil extension
                            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight);

                            // add the depth and stencil attachment
                            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);
                            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilFrameBufferID);

                            // reset depth buffer
                            mc.getFramebuffer().depthBuffer = -1;
                        }

                        // begin drawing the stencil
                        glPushAttrib(GL_ALL_ATTRIB_BITS);
                        glDisable(GL_ALPHA_TEST);
                        glDisable(GL_TEXTURE_2D);
                        glDisable(GL_LIGHTING);
                        glEnable(GL_BLEND);
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        glLineWidth(1 + width.getValue().floatValue());
                        glEnable(GL_LINE_SMOOTH);
                        glEnable(GL_STENCIL_TEST);
                        glClear(GL_STENCIL_BUFFER_BIT);
                        glClearStencil(0xF);
                        glStencilFunc(GL_NEVER, 1, 0xF);
                        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                        // render the tile entity model
                        if (event.getTileEntity().hasFastRenderer()) {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).renderTileEntityFast(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial(), event.getBuffer().getBuffer());
                        }

                        else {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).render(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial());
                        }

                        // fill the entity model
                        glStencilFunc(GL_NEVER, 0, 0xF);
                        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

                        // render the tile entity model
                        if (event.getTileEntity().hasFastRenderer()) {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).renderTileEntityFast(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial(), event.getBuffer().getBuffer());
                        }

                        else {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).render(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial());
                        }

                        // outline the entity model
                        glStencilFunc(GL_EQUAL, 1, 0xF);
                        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

                        glDepthMask(false);
                        glDisable(GL_DEPTH_TEST);
                        glEnable(GL_POLYGON_OFFSET_LINE);
                        glPolygonOffset(3, -2000000);
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

                        // color the stencil and clear the depth
                        glColor4d(ColorUtil.getPrimaryColor().getRed() / 255F, ColorUtil.getPrimaryColor().getGreen() / 255F, ColorUtil.getPrimaryColor().getBlue() / 255F, ColorUtil.getPrimaryColor().getAlpha() / 255F);

                        // render the tile entity model
                        if (event.getTileEntity().hasFastRenderer()) {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).renderTileEntityFast(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial(), event.getBuffer().getBuffer());
                        }

                        else {
                            TileEntityRendererDispatcher.instance.getRenderer(event.getTileEntity()).render(event.getTileEntity(), event.getX(), event.getY(), event.getZ(), event.getPartialTicks(), event.getDestroyStage(), event.getPartial());
                        }

                        // reset stencil
                        glPolygonOffset(-3, 2000000);
                        glDisable(GL_POLYGON_OFFSET_LINE);
                        glEnable(GL_DEPTH_TEST);
                        glDepthMask(true);
                        glDisable(GL_STENCIL_TEST);
                        glDisable(GL_LINE_SMOOTH);
                        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
                        glEnable(GL_BLEND);
                        glEnable(GL_LIGHTING);
                        glEnable(GL_TEXTURE_2D);
                        glEnable(GL_ALPHA_TEST);
                        glPopAttrib();
                        glPopMatrix();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onShaderColor(ShaderColorEvent event) {
        if (mode.getValue().equals(Mode.GLOW)) {
            // change the shader color
            event.setColor(ColorUtil.getPrimaryColor());

            // remove vanilla team color
            event.setCanceled(true);
        }
    }

    /**
     * Checks if the {@link Entity} entity has an ESP highlight
     * @param entity The entity to check
     * @return Whether the entity has an ESP highlight
     */
    public boolean hasHighlight(Entity entity) {
        return players.getValue() && entity instanceof EntityPlayer || passives.getValue() && EntityUtil.isPassiveMob(entity) || neutrals.getValue() && EntityUtil.isNeutralMob(entity) || hostiles.getValue() && EntityUtil.isHostileMob(entity) || vehicles.getValue() && EntityUtil.isVehicleMob(entity) || items.getValue() && (entity instanceof EntityItem || entity instanceof EntityExpBottle || entity instanceof EntityXPOrb) || crystals.getValue() && entity instanceof EntityEnderCrystal;
    }

    /**
     * Checks if the {@link TileEntity} tile entity has and ESP highlight
     * @param tileEntity the tile entity to check
     * @return Whether the tile entity has an ESP highlight
     */
    public boolean hasStorageHighlight(TileEntity tileEntity) {
        return chests.getValue() && tileEntity instanceof TileEntityChest || enderChests.getValue() && tileEntity instanceof TileEntityEnderChest || shulkers.getValue() && tileEntity instanceof TileEntityShulkerBox || hoppers.getValue() && (tileEntity instanceof TileEntityHopper || tileEntity instanceof TileEntityDropper) || furnaces.getValue() && tileEntity instanceof TileEntityFurnace;
    }

    public enum Mode {

        /**
         * Draws the Minecraft Glow shader
         */
        GLOW,

        /**
         * Draws a 2D shader on the GPU over the entity
         */
        SHADER,

        /**
         * Draws an outline over the entity
         */
        OUTLINE
    }

    public enum FragmentShader {

        /**
         * Draws an outline over the entity
         */
        OUTLINE,

        /**
         * Draws a dotted map over the entity
         */
        DOTTED,

        /**
         * Draws an outline with a transparent fill underneath
         */
        OUTLINE_FILL
    }
}
