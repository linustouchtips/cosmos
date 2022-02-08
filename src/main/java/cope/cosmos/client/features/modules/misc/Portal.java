package cope.cosmos.client.features.modules.misc;

import cope.cosmos.asm.mixins.accessor.IEntity;
import cope.cosmos.client.events.network.PacketEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author linustouchtips
 * @since 06/08/2021
 */
@SuppressWarnings("unused")
public class Portal extends Module {
    public static Portal INSTANCE;

    public Portal() {
        super("Portal", Category.MISC, "Modifies portal behavior");
        INSTANCE = this;
    }

    public static Setting<Boolean> godMode = new Setting<>("GodMode", false).setDescription("Cancels teleport packets");
    public static Setting<Boolean> screens = new Setting<>("Screens", true).setDescription("Allow the use of screens in portals");
    public static Setting<Boolean> effect = new Setting<>("Effect", true).setDescription("Cancels the portal overlay effect");
    public static Setting<Boolean> sounds = new Setting<>("Sounds", false).setDescription("Cancels portal sounds");

    @Override
    public void onUpdate() {
        // allows you to send messages while in portals
        ((IEntity) mc.player).setInPortal(!screens.getValue() && ((IEntity) mc.player).getInPortal());

        // disable portal rendering
        GuiIngameForge.renderPortal = !effect.getValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // reset overlay state
        GuiIngameForge.renderPortal = true;
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent event) {
        if (sounds.getValue()) {

            // prevents portal ambience sounds from playing
            if (event.getName().equals("block.portal.ambient") || event.getName().equals("block.portal.travel") || event.getName().equals("block.portal.trigger")) {
                event.setResultSound(null);
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        // allows you to become invincible while in portals on some servers
        if (event.getPacket() instanceof CPacketConfirmTeleport) {

            // make sure you are in a portal
            if (((IEntity) mc.player).getInPortal() && godMode.getValue()) {
                event.setCanceled(true);
            }
        }
    }
}
