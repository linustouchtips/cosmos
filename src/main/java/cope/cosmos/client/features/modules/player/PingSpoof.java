package cope.cosmos.client.features.modules.player;

import cope.cosmos.client.events.network.PacketEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.util.math.Timer;
import cope.cosmos.util.math.Timer.Format;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author aesthetical, linustouchtips
 * @since 10/26/2021
 */
public class PingSpoof extends Module {
    public static PingSpoof INSTANCE;

    public PingSpoof() {
        super("PingSpoof", Category.PLAYER, "Spoofs your latency to the server");
        INSTANCE = this;
    }

    public static Setting<Double> delay = new Setting<>("Delay", 0.1, 0.5, 5.0, 1).setDescription("The delay in seconds to hold off sending packets");
    public static Setting<Boolean> transactions = new Setting<>("Transactions", false).setDescription("If to cancel confirm transaction packets as well");

    // list of queued packets
    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();

    // timer for packet delay
    private final Timer packetTimer = new Timer();

    @Override
    public void onUpdate() {
        // if we've cleared our time to send
        if (packetTimer.passedTime(delay.getValue().longValue(), Format.SECONDS)) {

            // in case this function is called too many times
            if (!packets.isEmpty()) {

                // we can now begin processing packets
                packets.forEach(packet -> {
                    if (packet != null) {
                        // send all queued packets
                        mc.player.connection.sendPacket(packet);
                    }
                });

                packets.clear();
            }
            
            // reset our packetTimer
            packetTimer.resetTime();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // send our packets before disabling
        if (!packets.isEmpty()) {

            // we can now begin processing packets
            packets.forEach(packet -> {
                if (packet != null) {
                    // send all queued packets
                    mc.player.connection.sendPacket(packet);
                }
            });

            packets.clear();
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        // packet that tells the server to keep the connection alive
        // we can also cancel CPacketConfirmTransaction packets if the user specified they want to cancel transaction packets
        if (event.getPacket() instanceof CPacketKeepAlive || (transactions.getValue() && event.getPacket() instanceof CPacketConfirmTransaction)) {

            // make sure it's not one of our packets
            if (!packets.contains(event.getPacket())) {

                // cancel the packet, queue the packet to be sent later
                event.setCanceled(true);
                packets.add(event.getPacket());
            }
        }
    }
}
