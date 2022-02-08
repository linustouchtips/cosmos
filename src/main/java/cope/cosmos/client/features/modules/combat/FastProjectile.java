package cope.cosmos.client.features.modules.combat;

import cope.cosmos.client.events.network.PacketEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.util.player.InventoryUtil;
import cope.cosmos.util.math.Timer;
import cope.cosmos.util.math.Timer.Format;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * @author linustouchtips
 * @since 10/03/2021
 */
public class FastProjectile extends Module {
    public static FastProjectile INSTANCE;

    public FastProjectile() {
        super("FastProjectile", Category.COMBAT, "Allows your projectiles to do more damage", () -> {
            // if we are charged
            if (projectileTimer.passedTime(2, Format.SECONDS)) {
                return "Charged";
            }

            // find time till next charged shot
            long timeTillCharge = 2000 - projectileTimer.getMilliseconds();

            // clamp values
            if (timeTillCharge < 0) {
                timeTillCharge = 0;
            }

            else if (timeTillCharge > 2000) {
                timeTillCharge = 2000;
            }

            // display time
            return String.valueOf(timeTillCharge);
        });

        INSTANCE = this;
    }

    public static Setting<Double> ticks = new Setting<>("Ticks", 1.0D, 10.0D, 100.0D, 0).setDescription("How many times to send packets");
    public static Setting<Boolean> bows = new Setting<>("Bows", false).setDescription("Allow bows to do more damage");
    public static Setting<Boolean> eggs = new Setting<>("Eggs", false).setDescription("Allow eggs to do more damage");
    public static Setting<Boolean> snowballs = new Setting<>("Snowballs", false).setDescription("Allow snowballs to do more damage");

    // projectile timer, keeps track of last bow shot
    private static final Timer projectileTimer = new Timer();
    
    @SubscribeEvent
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        // packet when using projectiles
        if (event.getPacket() instanceof CPacketPlayerDigging && ((CPacketPlayerDigging) event.getPacket()).getAction().equals(CPacketPlayerDigging.Action.RELEASE_USE_ITEM)) {

            // make sure we are holding a projectile
            if (InventoryUtil.isHolding(Items.BOW) && bows.getValue() || InventoryUtil.isHolding(Items.EGG) && eggs.getValue() || InventoryUtil.isHolding(Items.SNOWBALL) && snowballs.getValue()) {

                // make sure there has been enough time since last shot
                if (projectileTimer.passedTime(2, Format.SECONDS)) {
                    // bypass
                    mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SPRINTING));

                    // direction -> randomized
                    Random projectileRandom = new Random();

                    // set the position at crazy bounds, makes server think we have crazy velocity -> more velocity = more projectile damage
                    for (int tick = 0; tick < ticks.getValue(); tick++) {
                        // player directions from rotation
                        double sin = -Math.sin(Math.toRadians(mc.player.rotationYaw));
                        double cos = Math.cos(Math.toRadians(mc.player.rotationYaw));

                        // send packets
                        if (projectileRandom.nextBoolean()) {
                            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + (sin * 100), mc.player.posY + 5, mc.player.posZ + (cos * 100), false));
                            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX - (sin * 100), mc.player.posY, mc.player.posZ - (cos * 100), true));
                        }

                        else {
                            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX - (sin * 100), mc.player.posY, mc.player.posZ - (cos * 100), true));
                            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX + (sin * 100), mc.player.posY + 5, mc.player.posZ + (cos * 100), false));
                        }
                    }

                    // reset timer
                    projectileTimer.resetTime();
                }
            }
        }
    }
}
