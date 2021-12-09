package cope.cosmos.client.features.modules.misc;

import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import cope.cosmos.event.annotation.Subscription;
import cope.cosmos.util.world.WorldUtil;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public class FakePlayer extends Module {
	public static FakePlayer INSTANCE;
	
	public FakePlayer() {
        super("FakePlayer", Category.MISC, "Spawns in a indestructible client-side player");
        INSTANCE = this;
        setExempt(true);
    }

    public static Setting<Boolean> inventory = new Setting<>("Inventory", true).setDescription("Sync the fake player inventory");
    public static Setting<Boolean> health = new Setting<>("Health", true).setDescription("Sync the fakeplayer health");
	
	public int id = -1;

    @Override
    public void onEnable() {
        super.onEnable();
        WorldUtil.createFakePlayer(mc.player.getGameProfile(), id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE), inventory.getValue(), health.getValue());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.world.removeEntityFromWorld(id);
        id = -1;
    }

    @Subscription
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity().equals(mc.player)) {
            mc.world.removeEntityFromWorld(id);
            id = -1;
        }
    }

    public int getID() {
        return id;
    }
}
