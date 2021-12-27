package cope.cosmos.client.features.modules.player;

import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.util.player.InventoryUtil;
import cope.cosmos.util.player.InventoryUtil.Inventory;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class NoFall extends Module {
    public static NoFall INSTANCE;

    public NoFall() {
        super("NoFall", Category.PLAYER, "Attempts to negate fall damage");
        INSTANCE = this;
    }

    public static Setting<Mode> mode = new Setting<>("Mode", Mode.PACKET).setDescription("How to negate fall damage");
    public static Setting<Double> distance = new Setting<>("Distance", 1.0, 2.0, 5.0, 1).setDescription("The minimum fall distance before doing anything");
    public static Setting<Boolean> oldfag = new Setting<>("Oldfag", false).setDescription("Bypass for oldfag.org").setVisible(() -> mode.getValue().equals(Mode.RUBBERBAND));

    public static Setting<Swap> swap = new Setting<>("Swap", Swap.LEGIT).setDescription("How to swap to a water bucket").setVisible(() -> mode.getValue().equals(Mode.WATER));
    public static Setting<Double> glideSpeed = new Setting<>("GlideSpeed", 0.1, 1.5, 5.0, 1).setDescription("The speed to glide at").setVisible(() -> mode.getValue().equals(Mode.GLIDE));

    private EnumHand hand = EnumHand.MAIN_HAND;
    private int oldSlot = -1;

    @Override
    public void onDisable() {
        super.onDisable();
        swapBack();
    }

    @Override
    public void onUpdate() {
        if (mc.player.fallDistance >= distance.getValue()) {
            switch (mode.getValue()) {
                case PACKET: {
                    mc.player.connection.sendPacket(new CPacketPlayer(true));
                    break;
                }

                case GLIDE: {
                    mc.player.motionY /= glideSpeed.getValue();
                    mc.player.connection.sendPacket(new CPacketPlayer(true));
                    break;
                }

                case WATER: {
                    doWaterBucket();
                    break;
                }

                case RUBBERBAND: {
                    if(mc.player.dimension == 1) {
                        mc.player.connection.sendPacket(new CPacketPlayer.Position(0, 64, 0, true));
                    } else {
                        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, 0, mc.player.posZ, true));
                    }
                    if(oldfag.getValue()) mc.player.fallDistance = 0;
                    break;
                }
            }
        }

        else {
            swapBack();
        }
    }

    private void doWaterBucket() {
        if (!InventoryUtil.isHolding(Items.WATER_BUCKET)) {
            int slot = InventoryUtil.getItemSlot(Items.WATER_BUCKET, Inventory.HOTBAR);
            if (slot == -1) {
                return; // get shit on
            }

            hand = slot == 45 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
            if (hand == EnumHand.MAIN_HAND) {
                oldSlot = mc.player.inventory.currentItem;
                InventoryUtil.switchToSlot(slot, swap.getValue() == Swap.LEGIT ? InventoryUtil.Switch.NORMAL : InventoryUtil.Switch.PACKET);
            }

            mc.player.setActiveHand(hand);
        }

        mc.player.connection.sendPacket(new CPacketPlayer.Rotation(mc.player.rotationYaw, -90, false));
        mc.playerController.processRightClick(mc.player, mc.world, hand);
    }

    private void swapBack() {
        if (oldSlot != -1) {
            InventoryUtil.switchToSlot(oldSlot, swap.getValue() == Swap.LEGIT ? InventoryUtil.Switch.NORMAL : InventoryUtil.Switch.PACKET);
            oldSlot = -1;
        }

        hand = EnumHand.MAIN_HAND;
    }

    public enum Mode {
        PACKET, GLIDE, WATER, RUBBERBAND
    }

    public enum Swap {
        LEGIT, SILENT
    }
}
