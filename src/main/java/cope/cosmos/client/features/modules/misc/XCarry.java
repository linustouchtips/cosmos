package cope.cosmos.client.features.modules.misc;

import cope.cosmos.asm.mixins.accessor.ICPacketCloseWindow;
import cope.cosmos.client.events.PacketEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.event.annotation.Subscription;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author linustouchtips
 * @since 07/21/2021
 */
@SuppressWarnings("unused")
public class XCarry extends Module {
    public static XCarry INSTANCE;

    public XCarry() {
        super("XCarry", Category.MISC, "Prevents the server from knowing when you open your inventory");
        INSTANCE = this;
    }

    @Subscription
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        if (event.getPacket() instanceof CPacketCloseWindow) {
            // prevent the client from sending the packet that lets the server know when you've closed your inventory
            event.setCanceled(((ICPacketCloseWindow) event.getPacket()).getWindowID() == mc.player.inventoryContainer.windowId);
        }
    }
}
