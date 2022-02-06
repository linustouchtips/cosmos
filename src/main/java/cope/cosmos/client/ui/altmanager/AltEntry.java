package cope.cosmos.client.ui.altmanager;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import cope.cosmos.asm.mixins.accessor.IMinecraft;
import cope.cosmos.client.ui.util.InterfaceUtil;
import cope.cosmos.util.render.FontUtil;
import cope.cosmos.util.render.RenderUtil;
import cope.cosmos.util.string.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.util.Map;

/**
 * @author Wolfsurge
 */
public class AltEntry implements InterfaceUtil {

    // The alt
    private Alt alt;
    // The offset of the button
    private float offset;
    // Is this the selected alt
    public boolean isSelected;

    public AltEntry(Alt alt, float offset) {
        setAlt(alt);
        setOffset(offset);
    }

    /**
     * Draws the alt entry
     * @param mouseX The mouse's X
     * @param mouseY The mouse's Y
     */
    public void drawAltEntry(int mouseX, int mouseY) {
        // Gets the scaled resolution
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());

        // Background
        RenderUtil.drawRect((scaledResolution.getScaledWidth() / 2f) - 150, getOffset(), 300, 30, 0x95000000);

        if(isMouseOverButton(mouseX, mouseY))
            RenderUtil.drawBorder((scaledResolution.getScaledWidth() / 2f) - 150, getOffset(), 300, 30, ColorUtil.getPrimaryColor().darker().darker());

        // Selected outline
        if(isSelected)
            RenderUtil.drawBorder((scaledResolution.getScaledWidth() / 2f) - 150, getOffset(), 300, 30, ColorUtil.getPrimaryColor());

        // Rotate arrow
        if(getAlt().getAltSession() != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(((scaledResolution.getScaledWidth() / 2f) - 147) + 10, getOffset() + 17, 1);
            GL11.glRotatef(-90, 0, 0, 1);
            GL11.glTranslatef(-(((scaledResolution.getScaledWidth() / 2f) - 147) + 10), -(getOffset() + 17), 1);
            GL11.glColor4f(255, 255, 255, 255);
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("cosmos", "textures/icons/dropdown.png"));
            Gui.drawModalRectWithCustomSizedTexture((scaledResolution.getScaledWidth() / 2) - 147, (int) (getOffset() + 10), 0, 0, 25, 25, 25, 25);
            GL11.glPopMatrix();
        }

        // Email / Error Message
        if(getAlt().getAltSession() != null)
            FontUtil.drawStringWithShadow(getAlt().getEmail(), (scaledResolution.getScaledWidth() / 2f) - 120, getOffset() + 5, 0xFFFFFF);
        else
            FontUtil.drawStringWithShadow(TextFormatting.DARK_RED + "Invalid User. Possible Rate Limit.", (scaledResolution.getScaledWidth() / 2f) - 120, getOffset() + 5, 0xFFFFFF);

        // Password
        FontUtil.drawStringWithShadow(getAlt().getPassword().replaceAll(".", "*"), (scaledResolution.getScaledWidth() / 2f) - 120, getOffset() + 17, 0xFFFFFF);

        // Alt Type
        FontUtil.drawStringWithShadow(getAlt().getAltSession() != null ? getAlt().getAltType().name() : "[INVALID]", (scaledResolution.getScaledWidth() / 2f) + (145 - FontUtil.getStringWidth(getAlt().getAltSession() != null ? getAlt().getAltType().name() : "[-]")), getOffset() + 11, 0xFFFFFF);
    }

    /**
     * Returns whether the mouse is over the button
     * @return Is the mouse is over the button
     */
    public boolean isMouseOverButton(int mouseX, int mouseY) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        return AltManagerGUI.mouseOver((scaledResolution.getScaledWidth() / 2f) - 150, getOffset(), 300, 30, mouseX, mouseY);
    }

    /**
     * Called when the button is clicked
     * @param mouseX The mouse's X
     * @param mouseY The mouse's Y
     * @param mouseButton The button that is pressed
     */
    public void whenClicked(int mouseX, int mouseY, int mouseButton) {
        // Login if the session isn't null
        if(getAlt().getAltSession() != null) {
            ((IMinecraft) Minecraft.getMinecraft()).setSession(getAlt().getAltSession());
        }
    }

    /**
     * Gets the alt
     * @return The alt
     */
    public Alt getAlt() {
        return alt;
    }

    /**
     * Sets the alt
     * @param alt The new alt
     */
    public void setAlt(Alt alt) {
        this.alt = alt;
    }

    /**
     * Gets the offset of the entry
     * @return The offset of the entry
     */
    public float getOffset() {
        return offset;
    }

    /**
     * Sets the offset of the entry
     * @param offset The new offset
     */
    public void setOffset(float offset) {
        this.offset = offset;
    }
}
