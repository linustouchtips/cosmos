package cope.cosmos.client.features.modules.combat;

import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.util.player.InventoryUtil;
import cope.cosmos.util.player.MotionUtil;
import cope.cosmos.util.player.PlayerUtil;
import cope.cosmos.util.system.Timer;
import cope.cosmos.util.system.Timer.Format;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unused")
public class Offhand extends Module {
    public static Offhand INSTANCE;

    public Offhand() {
        super("Offhand", Category.COMBAT, "Switches items in the offhand to a totem when low on health", () -> Setting.formatEnum(item.getValue()) + ", " + InventoryUtil.getItemCount(item.getValue().getItem()));
        INSTANCE = this;
    }

    public static Setting<OffhandItem> item = new Setting<>("Item", "Item to use when not at critical health", OffhandItem.CRYSTAL);
    public static Setting<OffhandItem> fallBack = new Setting<>("FallBack", "Item to use if you don't have the chosen item", OffhandItem.GAPPLE);
    public static Setting<Interact> interact = new Setting<>("Interact", "How to interact when switching", Interact.NORMAL);
    public static Setting<Gapple> gapple = new Setting<>("Gapple", "When to dynamically switch to a golden apple", Gapple.SWORD);

    public static Setting<Double> health = new Setting<>("Health", "Health considered as critical health", 0.0D, 16.0D, 36.0D, 1);
    public static Setting<Double> delay = new Setting<>("Delay", "Delay when switching items", 0.0D, 0.0D, 20.0D, 0);

    public static Setting<Boolean> armorSafe = new Setting<>("ArmorSafe", "Swaps to a totem when you have armor slots empty, prevents totem fails", false);
    public static Setting<Boolean> motionStrict = new Setting<>("MotionStrict", "Stops motion before switching", false);
    public static Setting<Boolean> recursive = new Setting<>("Recursive", "Allow the use of hotbar items", false);

    public static Setting<Boolean> pause = new Setting<>("Pause", "When to pause and use a totem", true);
    public static Setting<Boolean> pauseLiquid = new Setting<>("Liquid", "When in liquid", false).setParent(pause);
    public static Setting<Boolean> pauseAir = new Setting<>("Air", "When falling or flying", true).setParent(pause);
    public static Setting<Boolean> pauseElytra = new Setting<>("Elytra", "When elytra flying", true).setParent(pause);

    private Stage stage = Stage.IDLE;

    private final Timer offhandTimer = new Timer();

    @Override
    public void onDisable() {
        super.onDisable();
        stage = Stage.IDLE;
    }

    @Override
    public void onUpdate() {
        // we are not switching right now;
        stage = Stage.IDLE;

        if (mc.currentScreen == null && !mc.player.getHeldItemOffhand().getItem().equals(item.getValue().getItem())) {
            // the item to switch to
            Item switchItem = item.getValue().getItem();

            // if don't have out main item, try our fallback item
            if (InventoryUtil.getItemCount(item.getValue().getItem()) <= 0 && !stage.equals(Stage.MOVE_TO_OFFHAND)) {
                switchItem = fallBack.getValue().getItem();
            }

            // if we don't have anything to switch to, then we break the process
            if (InventoryUtil.getItemCount(switchItem) <= 0 && !stage.equals(Stage.MOVE_TO_OFFHAND)) {
                return;
            }

            // dynamically switch to a gapple if needed
            switch (gapple.getValue()) {
                case SWORD:
                    if (InventoryUtil.isHolding(Items.DIAMOND_SWORD)) {
                        switchItem = Items.GOLDEN_APPLE;
                    }

                    break;
                case INTERACT:
                    if (InventoryUtil.isHolding(Items.DIAMOND_SWORD) && Mouse.isButtonDown(1)) {
                        switchItem = Items.GOLDEN_APPLE;
                    }

                    break;
            }

            // make sure we are not in a situation where we need to pause the offhand
            if (pause.getValue()) {
                if (pauseLiquid.getValue() && PlayerUtil.isInLiquid()) {
                    switchItem = Items.TOTEM_OF_UNDYING;
                }

                else if (pauseAir.getValue() && mc.player.fallDistance > 5) {
                    switchItem = Items.TOTEM_OF_UNDYING;
                }

                else if (pauseElytra.getValue() && mc.player.isElytraFlying()) {
                    switchItem = Items.TOTEM_OF_UNDYING;
                }
            }

            // make sure none of our armor pieces are missing
            if (armorSafe.getValue()) {
                for (ItemStack stack : mc.player.getArmorInventoryList()) {
                    if (stack == null || stack.getItem() == Items.AIR) {
                        switchItem = Items.TOTEM_OF_UNDYING;
                        break;
                    }
                }
            }

            // some anticheats only have offhand patched if the player is holding a gapple, so this is a partial offhand bypass for those servers
            if (interact.getValue().equals(Interact.BYPASS) && InventoryUtil.isHolding(Items.GOLDEN_APPLE)) {
                switchItem = Items.TOTEM_OF_UNDYING;
            }

            // make sure we are not below our critical health
            if (PlayerUtil.getHealth() < health.getValue() && !mc.player.capabilities.isCreativeMode) {
                switchItem = Items.TOTEM_OF_UNDYING;
            }

            // make sure we've passed our delay and we're not already holding the item
            if (offhandTimer.passed(delay.getValue().longValue() * 100, Format.SYSTEM) && !mc.player.getHeldItemOffhand().getItem().equals(switchItem) && stage.equals(Stage.IDLE)) {
                // we are now picking up the item
                stage = Stage.PICKUP_ITEM;

                // find our item in our inventory
                int itemSlot = -1;
                for (int i = recursive.getValue() ? 0 : 9; i <= 44; i++) {
                    if (mc.player.inventoryContainer.getSlot(i).getStack().getItem().equals(switchItem)) {
                        itemSlot = i;
                        break;
                    }
                }

                // stop player motion before moving items
                if (motionStrict.getValue() && MotionUtil.hasMoved()) {
                    mc.player.motionX = 0;
                    mc.player.motionY = 0;
                    mc.player.motionZ = 0;
                    mc.player.setVelocity(0, 0, 0);
                    return;
                }

                // pick up the item
                if (itemSlot != -1) {
                    mc.playerController.windowClick(0, itemSlot, 0, ClickType.PICKUP, mc.player);

                    if (!interact.getValue().equals(Interact.STRICT)) {
                        // we are now moving the item to the offhand
                        stage = Stage.MOVE_TO_OFFHAND;

                        // move the item to the offhand
                        mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, mc.player);

                        // if we didn't get any item to swap
                        if (mc.player.inventory.getItemStack().isEmpty()) {
                            return;
                        }

                        // we are now swapping the old item
                        stage = Stage.SWAP_OLD;

                        // find a slot to return to
                        int returnSlot = -1;
                        for (int i = recursive.getValue() ? 0 : 9; i <= 44; i++) {
                            if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                                returnSlot = i;
                                break;
                            }
                        }

                        // move the item in the offhand to the return slot
                        if (returnSlot != -1) {
                            mc.playerController.windowClick(0, returnSlot, 0, ClickType.PICKUP, mc.player);
                        }
                    }
                }
            }

            if (interact.getValue().equals(Interact.STRICT)) {
                if (offhandTimer.passed(delay.getValue().longValue() * 200, Format.SYSTEM) && mc.player.inventory.getItemStack().getItem().equals(switchItem) && stage.equals(Stage.PICKUP_ITEM)) {
                    // we are now moving the item to the offhand
                    stage = Stage.MOVE_TO_OFFHAND;

                    // stop player motion before moving items
                    if (motionStrict.getValue() && MotionUtil.hasMoved()) {
                        mc.player.motionX = 0;
                        mc.player.motionY = 0;
                        mc.player.motionZ = 0;
                        mc.player.setVelocity(0, 0, 0);
                        return;
                    }

                    // move the item to the offhand
                    mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, mc.player);

                    // if we didn't get any item to swap
                    if (mc.player.inventory.getItemStack().isEmpty()) {
                        return;
                    }
                }

                if (offhandTimer.passed(delay.getValue().longValue() * 300, Format.SYSTEM) && mc.player.getHeldItemOffhand().getItem().equals(switchItem) && stage.equals(Stage.MOVE_TO_OFFHAND)) {
                    // we are now swapping the old item
                    stage = Stage.SWAP_OLD;

                    // find a slot to return to
                    int returnSlot = -1;
                    for (int i = recursive.getValue() ? 0 : 9; i <= 44; i++) {
                        if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                            returnSlot = i;
                            break;
                        }
                    }

                    if (returnSlot != -1) {
                        // stop player motion before moving items
                        if (motionStrict.getValue() && MotionUtil.hasMoved()) {
                            mc.player.motionX = 0;
                            mc.player.motionY = 0;
                            mc.player.motionZ = 0;
                            mc.player.setVelocity(0, 0, 0);
                            return;
                        }

                        // move the item in the offhand to the return slot
                        mc.playerController.windowClick(0, returnSlot, 0, ClickType.PICKUP, mc.player);
                    }
                }
            }
        }
    }

    public enum Stage {
        /**
         * Stage where no switching occurs
         */
        IDLE,

        /**
         * Stage where we pick up our item
         */
        PICKUP_ITEM,

        /**
         * Stage where we place our item in our offhand
         */
        MOVE_TO_OFFHAND,

        /**
         * Stage where we move the old item in our offhand to the return slot
         */
        SWAP_OLD
    }

    public enum OffhandItem {
        /**
         * Switch to an End Crystal
         */
        CRYSTAL(Items.END_CRYSTAL),

        /**
         * Switch to a Golden Apple
         */
        GAPPLE(Items.GOLDEN_APPLE),

        /**
         * Switch to a Totem
         */
        TOTEM(Items.TOTEM_OF_UNDYING);

        private final Item item;

        OffhandItem(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }
    }

    public enum Interact {
        /**
         * Interacts at all times
         */
        NORMAL,

        /**
         * Interacts if each process has passed the delay
         */
        STRICT,

        /**
         * Interacts when holding certain items
         */
        BYPASS
    }

    public enum Gapple {
        /**
         * Switches the offhand to a gapple when right clicking
         */
        INTERACT,

        /**
         * Switches the offhand to a gapple when holding a sword
         */
        SWORD,

        /**
         * Does not dynamically switch gapples to the offhand
         */
        NONE,
    }
}