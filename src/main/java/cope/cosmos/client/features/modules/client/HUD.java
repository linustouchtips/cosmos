package cope.cosmos.client.features.modules.client;

import com.mojang.realmsclient.gui.ChatFormatting;
import cope.cosmos.client.Cosmos;
import cope.cosmos.client.events.RenderAdvancementEvent;
import cope.cosmos.client.events.RenderPotionHUDEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.client.manager.managers.ModuleManager;
import cope.cosmos.client.manager.managers.TickManager.TPS;
import cope.cosmos.event.annotation.Subscription;
import cope.cosmos.util.client.ColorUtil;
import cope.cosmos.util.render.FontUtil;
import cope.cosmos.util.system.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Objects;

@SuppressWarnings("unused")
public class HUD extends Module {
    public static HUD INSTANCE;

    public HUD() {
        super("HUD", Category.CLIENT, "Displays the HUD");
        setDrawn(false);
        setExempt(true);
        INSTANCE = this;
    }

    public static Setting<Boolean> watermark = new Setting<>("Watermark", true).setDescription("Displays a client watermark");
    public static Setting<Boolean> activeModules = new Setting<>("ActiveModules", true).setDescription("Displays all enabled modules");
    public static Setting<Boolean> coordinates = new Setting<>("Coordinates", true).setDescription("Displays the user's coordinates");
    public static Setting<Boolean> speed = new Setting<>("Speed", true).setDescription("Displays the user's speed");
    public static Setting<Boolean> ping = new Setting<>("Ping", true).setDescription("Displays the user's server connection speed");
    public static Setting<Boolean> fps = new Setting<>("FPS", true).setDescription("Displays the current FPS");
    public static Setting<Boolean> tps = new Setting<>("TPS", true).setDescription("Displays the server TPS");
    public static Setting<Boolean> armor = new Setting<>("Armor", true).setDescription("Displays the player's armor");
    public static Setting<Boolean> potionEffects = new Setting<>("PotionEffects", false).setDescription("Displays the player's active potion effects");
    public static Setting<Boolean> potionHUD = new Setting<>("PotionHUD", false).setDescription("Displays the vanilla potion effect hud");
    public static Setting<Boolean> advancements = new Setting<>("Advancements", false).setDescription("Displays the vanilla advancement notification");

    private int globalOffset;

    private float listOffset;
    private int armorOffset;

    // bottom offsets
    private float bottomRight = 10;
    private float bottomLeft = 10;

    // test for my two way animation manager, will put this into hud editor if it gets made

    @Override
    public void onRender2D() {
        int SCREEN_WIDTH = new ScaledResolution(mc).getScaledWidth();
        int SCREEN_HEIGHT = new ScaledResolution(mc).getScaledHeight();

        // reset offsets
        globalOffset = 0;
        bottomLeft = 10;
        bottomRight = 10;

        if (watermark.getValue()) {
            FontUtil.drawStringWithShadow(Cosmos.NAME + TextFormatting.WHITE + " " + Cosmos.VERSION, 2, 2, ColorUtil.getPrimaryColor(globalOffset).getRGB());
        }

        if (mc.currentScreen == null) {
            if (activeModules.getValue()) {
                listOffset = 0;

                ModuleManager.getModules(Module::isDrawn).stream().filter(module -> module.getAnimation().getAnimationFactor() > 0.05).sorted(Comparator.comparing(module -> FontUtil.getStringWidth(module.getName() + (!module.getInfo().equals("") ? " " + module.getInfo() : "")) * -1)).forEach(module -> {
                    FontUtil.drawStringWithShadow(module.getName() + TextFormatting.WHITE + (!module.getInfo().equals("") ? " " + module.getInfo() : ""), (float) (new ScaledResolution(mc).getScaledWidth() - ((FontUtil.getStringWidth(module.getName() + (!module.getInfo().equals("") ? " " + module.getInfo() : "")) + 2) * MathHelper.clamp(module.getAnimation().getAnimationFactor(), 0, 1))), 2 + listOffset, ColorUtil.getPrimaryColor(globalOffset).getRGB());

                    // offset
                    listOffset += (mc.fontRenderer.FONT_HEIGHT + 1) * MathHelper.clamp(module.getAnimation().getAnimationFactor(), 0, 1);
                    globalOffset++;
                });
            }

            if (potionEffects.getValue()) {
                mc.player.getActivePotionEffects().forEach(potionEffect -> {

                    // potion name
                    String potionName = I18n.format(potionEffect.getEffectName());

                    // potion effects associated with modules
                    if (!potionName.equals("FullBright") && !potionName.equals("SpeedMine")) {

                        // potion formatted
                        StringBuilder potionFormatted = new StringBuilder(potionName);

                        // duration
                        float duration = potionEffect.getDuration() / getCosmos().getTickManager().getTPS(TPS.AVERAGE);
                        float durationSeconds = duration % 60;
                        float durationMinutes = (duration / 60) % 60;

                        // time formatter
                        DecimalFormat minuteFormatter = new DecimalFormat("0");
                        DecimalFormat secondsFormatter = new DecimalFormat("00");

                        // potion formatted
                        potionFormatted.append(" ").append(potionEffect.getAmplifier() + 1).append(ChatFormatting.WHITE).append(" ").append(minuteFormatter.format(durationMinutes)).append(":").append(secondsFormatter.format(durationSeconds));

                        FontUtil.drawStringWithShadow(potionFormatted.toString(), SCREEN_WIDTH - FontUtil.getStringWidth(potionFormatted.toString()) - 2, SCREEN_HEIGHT - bottomRight, potionEffect.getPotion().getLiquidColor());

                        // offset
                        bottomRight += FontUtil.getFontHeight() + 1;
                        globalOffset++;
                    }
                });
            }

            if (speed.getValue()) {
                double distanceX = mc.player.posX - mc.player.prevPosX;
                double distanceZ = mc.player.posZ - mc.player.prevPosZ;
                String speedDisplay = "Speed " + TextFormatting.WHITE + MathUtil.roundFloat((MathHelper.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceZ, 2)) / 1000) / (0.05F / 3600), 1) + " kmh";
                FontUtil.drawStringWithShadow(speedDisplay, SCREEN_WIDTH - FontUtil.getStringWidth(speedDisplay) - 2, SCREEN_HEIGHT - bottomRight, ColorUtil.getPrimaryColor(globalOffset).getRGB());

                // offset
                bottomRight += FontUtil.getFontHeight() + 1;
                globalOffset++;
            }

            if (ping.getValue()) {
                String pingDisplay = "Ping " + TextFormatting.WHITE + (!mc.isSingleplayer() ? Objects.requireNonNull(mc.getConnection()).getPlayerInfo(mc.player.getUniqueID()).getResponseTime() : 0) + "ms";
                FontUtil.drawStringWithShadow(pingDisplay, SCREEN_WIDTH - FontUtil.getStringWidth(pingDisplay) - 2, SCREEN_HEIGHT - bottomRight, ColorUtil.getPrimaryColor(globalOffset).getRGB());

                // offset
                bottomRight += FontUtil.getFontHeight() + 1;
                globalOffset++;
            }

            if (tps.getValue()) {
                String tpsDisplay = "TPS " + TextFormatting.WHITE + Cosmos.INSTANCE.getTickManager().getTPS(TPS.AVERAGE);
                FontUtil.drawStringWithShadow(tpsDisplay, SCREEN_WIDTH - FontUtil.getStringWidth(tpsDisplay) - 2, SCREEN_HEIGHT - bottomRight, ColorUtil.getPrimaryColor(globalOffset).getRGB());

                // offset
                bottomRight += FontUtil.getFontHeight() + 1;
                globalOffset++;
            }

            if (fps.getValue()) {
                String tpsDisplay = "FPS " + TextFormatting.WHITE + Minecraft.getDebugFPS();
                FontUtil.drawStringWithShadow(tpsDisplay, SCREEN_WIDTH - FontUtil.getStringWidth(tpsDisplay) - 2, SCREEN_HEIGHT - bottomRight, ColorUtil.getPrimaryColor(globalOffset).getRGB());

                // offset
                bottomRight += FontUtil.getFontHeight() + 1;
                globalOffset++;
            }

            if (coordinates.getValue()) {
                String overWorldCoords = mc.player.dimension != -1 ? "XYZ " + TextFormatting.WHITE + MathUtil.roundFloat(mc.player.posX, 1) + " " + MathUtil.roundFloat(mc.player.posY, 1) + " " + MathUtil.roundFloat(mc.player.posZ, 1) : "XYZ " + TextFormatting.WHITE + MathUtil.roundFloat(mc.player.posX * 8, 1) + " " + MathUtil.roundFloat(mc.player.posY * 8, 1) + " " + MathUtil.roundFloat(mc.player.posZ * 8, 1);
                String netherCoords = mc.player.dimension == -1 ? "XYZ " + TextFormatting.WHITE + MathUtil.roundFloat(mc.player.posX, 1) + " " + MathUtil.roundFloat(mc.player.posY, 1) + " " + MathUtil.roundFloat(mc.player.posZ, 1) : TextFormatting.RED + "XYZ " + TextFormatting.WHITE + MathUtil.roundFloat(mc.player.posX / 8, 1) + " " + MathUtil.roundFloat(mc.player.posY / 8, 1) + " " + MathUtil.roundFloat(mc.player.posZ / 8, 1);

                FontUtil.drawStringWithShadow(overWorldCoords, 2, SCREEN_HEIGHT - bottomLeft, ColorUtil.getPrimaryColor(globalOffset).getRGB());
                bottomLeft += FontUtil.getFontHeight() + 1;
                globalOffset++;

                FontUtil.drawStringWithShadow(netherCoords, 2, SCREEN_HEIGHT - bottomLeft, new Color(255, 0, 0).getRGB());
                bottomLeft += FontUtil.getFontHeight() + 1;
                globalOffset++;
            }

            if (armor.getValue()) {
                armorOffset = 0;
                mc.player.inventory.armorInventory.forEach(itemStack -> {
                    if (!itemStack.isEmpty()) {
                        GlStateManager.pushMatrix();
                        RenderHelper.enableGUIStandardItemLighting();

                        mc.getRenderItem().zLevel = 200;
                        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, (SCREEN_WIDTH / 2) + ((9 - armorOffset) * 16) - 78, SCREEN_HEIGHT - (mc.player.isInWater() ? 10 : 0) - 55);
                        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, (SCREEN_WIDTH / 2) + ((9 - armorOffset) * 16) - 78, SCREEN_HEIGHT - (mc.player.isInWater() ? 10 : 0) - 55, "");
                        mc.getRenderItem().zLevel = 0;

                        RenderHelper.disableStandardItemLighting();
                        GlStateManager.popMatrix();
                    }

                    armorOffset++;
                });
            }
        }
    }

    @Subscription
    public void onRenderPotionHUD(RenderPotionHUDEvent event) {
        if (!potionHUD.getValue()) {
            // cancel vanilla potion hud from rendering
            event.setCanceled(true);
        }
    }

    @Subscription
    public void onRenderAdvancement(RenderAdvancementEvent event) {
        if (!advancements.getValue()) {
            // cancel vanilla advancement notification from rendering
            event.setCanceled(true);
        }
    }
}
