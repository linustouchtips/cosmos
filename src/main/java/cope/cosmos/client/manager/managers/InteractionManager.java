package cope.cosmos.client.manager.managers;

import cope.cosmos.asm.mixins.accessor.IEntityPlayerSP;
import cope.cosmos.asm.mixins.accessor.IMinecraft;
import cope.cosmos.client.manager.Manager;
import cope.cosmos.util.Wrapper;
import cope.cosmos.util.player.Rotation;
import cope.cosmos.util.player.Rotation.Rotate;
import cope.cosmos.util.world.AngleUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InteractionManager extends Manager implements Wrapper {
    public InteractionManager() {
        super("InteractionManager", "Manages all player interactions");
    }

    // list of blocks which need to be shift clicked to be placed on
    public static final List<Block> sneakBlocks = Arrays.asList(
            Blocks.ENDER_CHEST,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.CRAFTING_TABLE,
            Blocks.ANVIL,
            Blocks.BREWING_STAND,
            Blocks.HOPPER,
            Blocks.DROPPER,
            Blocks.DISPENSER,
            Blocks.TRAPDOOR,
            Blocks.ENCHANTING_TABLE,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.SILVER_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX
    );

    /**
     * Places a block at a specified position
     * @param position Position of the block to place on
     * @param rotate Mode for rotating
     */
    public void placeBlock(BlockPos position, Rotate rotate) {
        for (EnumFacing direction : EnumFacing.values()) {
            // find a block to place against
            BlockPos directionOffset = position.offset(direction);

            // make sure the offset is empty
            if (mc.world.getBlockState(directionOffset).getMaterial().isReplaceable()) {
                continue;
            }

            // stop sprinting before preforming actions
            boolean sprint = mc.player.isSprinting();
            if (sprint) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SPRINTING));
            }

            // sneak if the block is not right-clickable
            boolean sneak = sneakBlocks.contains(mc.world.getBlockState(directionOffset).getBlock()) && !mc.player.isSneaking();
            if (sneak) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
            }

            // facing directions
            double facingX = 0;
            double facingY = 0;
            double facingZ = 0;

            // rotate to block
            if (!rotate.equals(Rotate.NONE)) {
                float[] blockAngles = AngleUtil.calculateAngles(new Vec3d(directionOffset).addVector(0.5, 0.5, 0.5).add(new Vec3d(direction.getOpposite().getDirectionVec()).scale(0.5)));

                // rotate via packet, server should confirm instantly?
                switch (rotate) {
                    case CLIENT:
                        mc.player.rotationYaw = blockAngles[0];
                        mc.player.rotationYawHead = blockAngles[0];
                        mc.player.rotationPitch = blockAngles[1];
                        break;
                    case PACKET:
                        mc.player.connection.sendPacket(new CPacketPlayer.Rotation(blockAngles[0], blockAngles[1], mc.player.onGround));
                        // ((IEntityPlayerSP) mc).setLastReportedYaw(blockAngles[0]);
                        // ((IEntityPlayerSP) mc).setLastReportedPitch(blockAngles[1]);
                        break;
                }

                // make sure our facing directions are consistent with our rotations
                Vec3d placeVector = AngleUtil.getVectorForRotation(new Rotation(blockAngles[0], blockAngles[1]));
                RayTraceResult vectorResult = mc.world.rayTraceBlocks(mc.player.getPositionEyes(1), mc.player.getPositionEyes(1).addVector(placeVector.x * mc.playerController.getBlockReachDistance(), placeVector.y * mc.playerController.getBlockReachDistance(), placeVector.z * mc.playerController.getBlockReachDistance()), false, false, true);

                if (vectorResult != null && vectorResult.hitVec != null) {
                    facingX = vectorResult.hitVec.x - position.getX();
                    facingY = vectorResult.hitVec.y - position.getY();
                    facingZ = vectorResult.hitVec.z - position.getZ();
                }
            }

            // right click direction offset block
            EnumActionResult placeResult = mc.playerController.processRightClickBlock(mc.player, mc.world, directionOffset, direction.getOpposite(), new Vec3d(facingX, facingY, facingZ), EnumHand.MAIN_HAND);

            // reset sneak
            if (sneak) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
            }

            // reset sprint
            if (sprint) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SPRINTING));
            }

            // swing hand
            if (placeResult != EnumActionResult.FAIL) {
                mc.player.swingArm(EnumHand.MAIN_HAND);
                ((IMinecraft) mc).setRightClickDelayTimer(4);
                return;
            }
        }
    }

    public void placeItem() {

    }

    /**
     * Finds all the visible sides of a certain position
     * @param position The position to find all the visible sides of
     * @return List of visible sides
     */
    public List<EnumFacing> getVisibleSides(BlockPos position) {
        List<EnumFacing> visibleSides = new ArrayList<>();

        // pos vector
        Vec3d positionVector = new Vec3d(position).addVector(0.5, 0.5, 0.5);

        // facing
        double facingX = mc.player.getPositionEyes(1).x - positionVector.x;
        double facingY = mc.player.getPositionEyes(1).y - positionVector.y;
        double facingZ = mc.player.getPositionEyes(1).z - positionVector.z;

        // x
        {
            if (facingX < -0.5) {
                visibleSides.add(EnumFacing.WEST);
            }

            else if (facingX > 0.5) {
                visibleSides.add(EnumFacing.EAST);
            }

            else if (!mc.world.getBlockState(position).isFullBlock()) {
                visibleSides.add(EnumFacing.WEST);
                visibleSides.add(EnumFacing.EAST);
            }
        }

        // y
        {
            if (facingY < -0.5) {
                visibleSides.add(EnumFacing.DOWN);
            }

            else if (facingY > 0.5) {
                visibleSides.add(EnumFacing.UP);
            }

            else {
                visibleSides.add(EnumFacing.DOWN);
                visibleSides.add(EnumFacing.UP);
            }
        }

        // z
        {
            if (facingZ < -0.5) {
                visibleSides.add(EnumFacing.NORTH);
            }

            else if (facingZ > 0.5) {
                visibleSides.add(EnumFacing.SOUTH);
            }

            else if (!mc.world.getBlockState(position).isFullBlock()) {
                visibleSides.add(EnumFacing.NORTH);
                visibleSides.add(EnumFacing.SOUTH);
            }
        }

        return visibleSides;
    }
}