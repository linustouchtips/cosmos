package cope.cosmos.client.manager.managers;

import cope.cosmos.asm.mixins.accessor.ICPacketPlayer;
import cope.cosmos.client.Cosmos;
import cope.cosmos.client.events.MotionUpdateEvent;
import cope.cosmos.client.events.PacketEvent;
import cope.cosmos.client.manager.Manager;
import cope.cosmos.event.annotation.Subscription;
import cope.cosmos.event.listener.Priority;
import cope.cosmos.util.Wrapper;
import cope.cosmos.util.player.Rotation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.TreeMap;

@SuppressWarnings("unused")
public class RotationManager extends Manager implements Wrapper {

    // all client rotations
    private final TreeMap<Integer, Rotation> rotationMap = new TreeMap<>();

    // the current server rotation
    private final Rotation.MutableRotation serverRotation = new Rotation.MutableRotation(Float.NaN, Float.NaN);

    public RotationManager() {
        super("RotationManager", "Keeps track of server rotations");
        Cosmos.EVENT_BUS.subscribe(this);
    }

    @Subscription(priority = Priority.HIGHEST)
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        if (event.getPacket() instanceof CPacketPlayer) {
            CPacketPlayer packet = (CPacketPlayer) event.getPacket();
            if (((ICPacketPlayer) packet).isRotating()) {
                serverRotation.setYaw(packet.getYaw(0));
                serverRotation.setPitch(packet.getPitch(0));
            }
        }
    }

    @Subscription
    public void onMotionUpdate(MotionUpdateEvent event) {
        if (!rotationMap.isEmpty()) {
            // rotation with the highest priority
            Rotation rotation = rotationMap.lastEntry().getValue();

            // cancel, we'll send our own rotations
            event.setCanceled(true);

            // assign vanilla values
            event.setOnGround(mc.player.onGround);
            event.setX(mc.player.posX);
            event.setY(mc.player.getEntityBoundingBox().minY);
            event.setZ(mc.player.posZ);

            // set the rotation to be our custom value
            event.setYaw(rotation.getYaw());
            event.setPitch(rotation.getPitch());

            // clear our rotations
            rotationMap.clear();
        }
    }

    public void addRotation(Rotation rotation, int priority) {
        rotationMap.put(priority, rotation);
    }

    public Rotation.MutableRotation getServerRotation() {
        return serverRotation;
    }
}
