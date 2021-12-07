package cope.cosmos.client.features.modules.movement;

import cope.cosmos.asm.mixins.accessor.ICPacketPlayer;
import cope.cosmos.client.events.KeyDownEvent;
import cope.cosmos.client.events.PacketEvent;
import cope.cosmos.client.events.SlimeEvent;
import cope.cosmos.client.events.SoulSandEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.util.system.Timer;
import cope.cosmos.util.system.Timer.Format;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * @author linustouchtips, Sxmurai
 * @since 07/21/2021
 */
@SuppressWarnings("unused")
public class NoSlow extends Module {
    public static NoSlow INSTANCE;

    public NoSlow() {
        super("NoSlow", Category.MOVEMENT, "Removes various slowdown effects");
        INSTANCE = this;
    }

    // anti-cheat
    public static Setting<Boolean> strict = new Setting<>("Strict", "Allows you to bypass normal NCP server", false);
    public static Setting<Boolean> airStrict = new Setting<>("AirStrict", "Allows you to bypass strict NCP servers while in the air", false);
    public static Setting<Boolean> switchStrict = new Setting<>("SwitchStrict", "Allows you to bypass strict NCP servers", false);
    public static Setting<Boolean> placeStrict = new Setting<>("PlaceStrict", "Allows you to bypass strict servers", false);
    public static Setting<Boolean> groundStrict = new Setting<>("GroundStrict", "Allows you to bypass strict NCP servers while on the ground", false);

    // inventory move
    public static Setting<Boolean> inventoryMove = new Setting<>("InventoryMove", "Allows you to move around while in GUIs", true);
    public static Setting<Float> arrowLook = new Setting<>("ArrowLook", "The speed that the arrow keys should rotate you with", 0.0F, 5.0F, 10.0F, 1).setParent(inventoryMove);

    // no slow instances
    public static Setting<Boolean> items = new Setting<>("Items", "Removes the slowdown effect while using items", true);
    public static Setting<Boolean> soulsand = new Setting<>("SoulSand", "Removes the slowdown effect when walking on soulsand", false);
    public static Setting<Boolean> slime = new Setting<>("Slime", "Removes the slowdown effect when walking on slime", false);
    public static Setting<Boolean> ice = new Setting<>("Ice", "Removes the slipperiness effect when walking on ice", true);

    // serverside sneaking
    private boolean isSneaking = false;

    // timer for ticks to stay on the ground
    private final Timer groundTimer = new Timer();

    // list of keybinds
    private final KeyBinding[] KEYS = new KeyBinding[] {
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindSprint,
            mc.gameSettings.keyBindSneak
    };

    @Override
    public void onEnable() {
        super.onEnable();

        // set the slipperiness of ice to the normal block value
        if (ice.getValue()) {
            Blocks.ICE.setDefaultSlipperiness(0.6F);
            Blocks.PACKED_ICE.setDefaultSlipperiness(0.6F);
            Blocks.FROSTED_ICE.setDefaultSlipperiness(0.6F);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // update our sneak state
        if (isSneaking && airStrict.getValue()) {
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }

        isSneaking = false;

        // reset our keybind conflicts
        for (KeyBinding binding : KEYS) {
            binding.setKeyConflictContext(KeyConflictContext.IN_GAME);
        }

        // reset ice slipperiness to default value
        if (ice.getValue()) {
            Blocks.ICE.setDefaultSlipperiness(0.98F);
            Blocks.FROSTED_ICE.setDefaultSlipperiness(0.98F);
            Blocks.PACKED_ICE.setDefaultSlipperiness(0.98F);
        }
    }

    @Override
    public void onUpdate() {
        // update our sneak state
        if (isSneaking && airStrict.getValue() && !mc.player.isHandActive()) {
            isSneaking = false;
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }

        // if we are slowed, then send corresponding packets
        if (isSlowed()) {
            // Updated NCP bypass
            if (switchStrict.getValue()) {
                mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem)); // lolololololo thanks FencingF
            }

            // Old NCP bypass
            if (placeStrict.getValue()) {
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(mc.objectMouseOver.getBlockPos(), EnumFacing.UP, EnumHand.MAIN_HAND, 0, 0, 0));
            }
        }

        // allows you to move normally while in GUI screens
        if (inventoryMove.getValue() && isInScreen()) {

            // update keybind state and conflict context
            for (KeyBinding binding : KEYS) {
                KeyBinding.setKeyBindState(binding.getKeyCode(), Keyboard.isKeyDown(binding.getKeyCode()));
                binding.setKeyConflictContext(ConflictContext.FAKE_CONTEXT);
            }

            // update rotation based on arrow key movement
            if (arrowLook.getValue() != 0) {
                if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                    mc.player.rotationPitch -= arrowLook.getValue();
                }

                else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                    mc.player.rotationPitch += arrowLook.getValue();
                }

                else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                    mc.player.rotationYaw += arrowLook.getValue();
                }

                else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                    mc.player.rotationYaw -= arrowLook.getValue();
                }

                // clamp pitch to be within vanilla values
                mc.player.rotationPitch = MathHelper.clamp(mc.player.rotationPitch, -90, 90);
            }
        }

        else {
            // reset key conflict
            for (KeyBinding binding : KEYS) {
                binding.setKeyConflictContext(KeyConflictContext.IN_GAME);
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.PacketSendEvent event) {
        if (event.getPacket() instanceof CPacketPlayer) {
            if (isSlowed()) {
                // NCP bypass
                if (strict.getValue()) {
                    mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, mc.player.getPosition(), EnumFacing.DOWN));
                }

                // Updated NCP bypass, specifically strict configurations
                if (groundStrict.getValue() && ((CPacketPlayer) event.getPacket()).isOnGround()) {
                    if (groundTimer.passedTime(2, Format.TICKS)) {
                        ((ICPacketPlayer) event.getPacket()).setY(((CPacketPlayer) event.getPacket()).getY(mc.player.posY) + 0.05);
                        groundTimer.resetTime();
                    }

                    ((ICPacketPlayer) event.getPacket()).setOnGround(false);
                }
            }
        }

        if (event.getPacket() instanceof CPacketClickWindow) {
            // Updated NCP bypass for inventory move
            if (strict.getValue()) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SPRINTING)); // rofl nice patch ncp devs
            }
        }
    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event) {
        // remove vanilla slowdown effect
        if (isSlowed()) {
            event.getMovementInput().moveForward *= 5;
            event.getMovementInput().moveStrafe *= 5;
        }
    }

    @SubscribeEvent
    public void onUseItem(LivingEntityUseItemEvent event) {
        // send sneaking packet when we use an item
        if (isSlowed() && airStrict.getValue() && !isSneaking) {
            isSneaking = true;
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
        }
    }

    @SubscribeEvent
    public void onSoulSand(SoulSandEvent event) {
        // remove soul sand slowdown effect
        if (soulsand.getValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSlime(SlimeEvent event) {
        // remove soul slime effect
        if (slime.getValue()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyDown(KeyDownEvent event) {
        // remove conflict context when pressing keys
        if (inventoryMove.getValue()) {
            event.setCanceled(true);
        }
    }

    /**
     * Checks if the player is in a screen
     * @return Whether the player is in a screen
     */
    public boolean isInScreen() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiEditSign || mc.currentScreen instanceof GuiRepair);
    }

    /**
     * Checks if the player is slowed
     * @return Whether the player is slowed
     */
    private boolean isSlowed() {
        return (mc.player.isHandActive() && items.getValue()) && !mc.player.isRiding() && !mc.player.isElytraFlying();
    }

    public enum ConflictContext implements IKeyConflictContext {

        /**
         * Fake key conflict context that allows keys to be pressed in GUI screens
         */
        FAKE_CONTEXT {

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return false;
            }
        }
    }
}

