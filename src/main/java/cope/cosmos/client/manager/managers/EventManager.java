package cope.cosmos.client.manager.managers;

import com.mojang.realmsclient.gui.ChatFormatting;
import cope.cosmos.client.Cosmos;
import cope.cosmos.client.Cosmos.ClientType;
import cope.cosmos.client.events.combat.CriticalModifierEvent;
import cope.cosmos.client.events.combat.DeathEvent;
import cope.cosmos.client.events.combat.TotemPopEvent;
import cope.cosmos.client.events.motion.movement.KnockBackEvent;
import cope.cosmos.client.events.motion.movement.PushOutOfBlocksEvent;
import cope.cosmos.client.events.network.PacketEvent;
import cope.cosmos.client.events.entity.player.interact.EntityUseItemEvent;
import cope.cosmos.client.events.entity.player.interact.ItemInputUpdateEvent;
import cope.cosmos.client.events.entity.player.interact.RightClickItemEvent;
import cope.cosmos.client.events.render.gui.RenderOverlayEvent;
import cope.cosmos.client.events.render.world.RenderFogColorEvent;
import cope.cosmos.client.events.render.world.RenderFogEvent;
import cope.cosmos.client.events.block.LeftClickBlockEvent;
import cope.cosmos.client.manager.Manager;
import cope.cosmos.util.Wrapper;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;

/**
 * @author bon55, linustouchtips
 * @since 05/05/2021
 */
public class EventManager extends Manager implements Wrapper {
	public EventManager() {
		super("EventManager", "Manages Forge events");

		// register to event bus
		Cosmos.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onUpdate(LivingEvent.LivingUpdateEvent event) {

		// runs on the entity update method
		mc.mcProfiler.startSection("cosmos-update");

		if (event.getEntity().getEntityWorld().isRemote && event.getEntityLiving().equals(mc.player)) {

			// module onUpdate
			getCosmos().getModuleManager().getAllModules().forEach(module -> {
				if (nullCheck() || getCosmos().getNullSafeFeatures().contains(module)) {

					// run if module is enabled
					if (module.isEnabled()) {
						try {
							module.onUpdate();
						} catch (Exception exception) {

							// print stacktrace if in dev environment
							if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
								exception.printStackTrace();
							}
						}
					}
				}
			});

			// manager onUpdate
			getCosmos().getManagers().forEach(manager -> {
				if (nullCheck() || getCosmos().getNullSafeFeatures().contains(manager)) {

					// run manager onUpdate
					try {
						manager.onUpdate();
					} catch (Exception exception) {

						// print stacktrace if in dev environment
						if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
							exception.printStackTrace();
						}
					}
				}
			});
		}

		mc.mcProfiler.endSection();

		// if (mc.currentScreen instanceof GuiMainMenu && !Cosmos.SETUP) {
		//		mc.displayGuiScreen(new SetUpGUI());
		// }
	}

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent event) {

		// runs on game (root) tick
		mc.mcProfiler.startSection("cosmos-root-tick");

		// module onTick
		getCosmos().getModuleManager().getAllModules().forEach(module -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(module)) {

				// run if module is enabled
				if (module.isEnabled()) {
					try {
						module.onTick();
					} catch (Exception exception) {

						// print stacktrace if in dev environment
						if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
							exception.printStackTrace();
						}
					}
				}
			}
		});

		// manager onTick
		getCosmos().getManagers().forEach(manager -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(manager)) {

				// run manager onTick
				try {
					manager.onTick();
				} catch (Exception exception) {

					// print stacktrace if in dev environment
					if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
						exception.printStackTrace();
					}
				}
			}
		});

		mc.mcProfiler.endSection();
	}
	
	@SubscribeEvent
	public void onRender2d(RenderGameOverlayEvent.Text event) {

		// runs on every frame
		mc.mcProfiler.startSection("cosmos-render-2D");

		// module onRender2D
		getCosmos().getModuleManager().getAllModules().forEach(module -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(module)) {

				// run if module is enabled
				if (module.isEnabled()) {
					try {
						module.onRender2D();
					} catch (Exception exception) {

						// print stacktrace if in dev environment
						if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
							exception.printStackTrace();
						}
					}
				}
			}
		});

		// manager onRender2D
		getCosmos().getManagers().forEach(manager -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(manager)) {

				// run manager onRender2D
				try {
					manager.onRender2D();
				} catch (Exception exception) {

					// print stacktrace if in dev environment
					if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
						exception.printStackTrace();
					}
				}
			}
		});

		mc.mcProfiler.endSection();
	}
	
	@SubscribeEvent
	public void onRender3D(RenderWorldLastEvent event) {

		// runs on every frame
		mc.mcProfiler.startSection("cosmos-render-3D");

		// module onRender3D
		getCosmos().getModuleManager().getAllModules().forEach(module -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(module)) {

				// run if module is enabled
				if (module.isEnabled()) {
					try {
						module.onRender3D();
					} catch (Exception exception) {

						// print stacktrace if in dev environment
						if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
							exception.printStackTrace();
						}
					}
				}
			}
		});

		// manager onRender3D
		getCosmos().getManagers().forEach(manager -> {
			if (nullCheck() || getCosmos().getNullSafeFeatures().contains(manager)) {

				// run manager onRender3D
				try {
					manager.onRender3D();
				} catch (Exception exception) {

					// print stacktrace if in dev environment
					if (Cosmos.CLIENT_TYPE.equals(ClientType.DEVELOPMENT)) {
						exception.printStackTrace();
					}
				}
			}
		});

		mc.mcProfiler.endSection();
	}
	
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		
		// pressed key
		int key = Keyboard.getEventKey();

		// toggle
		if (key != 0 && Keyboard.getEventKeyState()) {
			getCosmos().getModuleManager().getAllModules().forEach(module -> {
				if (module.getKey() == key) {
					module.toggle();
				}
			});
		}
	}

	@SubscribeEvent
	public void onChatInput(ClientChatEvent event) {
		
		// event the user sends a command
		if (event.getMessage().startsWith(Cosmos.PREFIX)) {
			event.setCanceled(true);

			try {
				
				// dispatch
				getCosmos().getCommandDispatcher().execute(getCosmos().getCommandDispatcher()
								.parse(event.getOriginalMessage().substring(1), 1));
				
			} catch (Exception exception) {
				// exception.printStackTrace();
				getCosmos().getChatManager().sendHoverableMessage(ChatFormatting.RED + "An error occurred!", "No such command was found");
			}
		}
	}

	// **************************** EVENTS ****************************

	@SubscribeEvent
	public void onTotemPop(PacketEvent.PacketReceiveEvent event) {
		if (event.getPacket() instanceof SPacketEntityStatus && ((SPacketEntityStatus) event.getPacket()).getOpCode() == 35) {
			TotemPopEvent totemPopEvent = new TotemPopEvent(((SPacketEntityStatus) event.getPacket()).getEntity(mc.world));
			Cosmos.EVENT_BUS.post(totemPopEvent);

			if (totemPopEvent.isCanceled()) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onCriticalHit(CriticalHitEvent event) {
		CriticalModifierEvent criticalModifierEvent = new CriticalModifierEvent();
		Cosmos.EVENT_BUS.post(criticalModifierEvent);

		// update damage modifier
		event.setDamageModifier(criticalModifierEvent.getDamageModifier());
	}

	@SubscribeEvent
	public void onInputUpdate(InputUpdateEvent event) {
		ItemInputUpdateEvent itemInputUpdateEvent = new ItemInputUpdateEvent(event.getMovementInput());
		Cosmos.EVENT_BUS.post(itemInputUpdateEvent);
	}

	@SubscribeEvent
	public void onLivingEntityUseItem(LivingEntityUseItemEvent event) {
		EntityUseItemEvent entityUseItemEvent = new EntityUseItemEvent();
		Cosmos.EVENT_BUS.post(entityUseItemEvent);
	}

	@SubscribeEvent
	public void onKnockback(LivingKnockBackEvent event) {
		KnockBackEvent knockBackEvent = new KnockBackEvent();
		Cosmos.EVENT_BUS.post(knockBackEvent);

		if (knockBackEvent.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (event.getEntityPlayer().equals(mc.player)) {
			RightClickItemEvent rightClickItemEvent = new RightClickItemEvent(event.getItemStack());
			Cosmos.EVENT_BUS.post(rightClickItemEvent);

			if (rightClickItemEvent.isCanceled()) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		LeftClickBlockEvent leftClickBlockEvent = new LeftClickBlockEvent(event.getPos(), event.getFace());
		Cosmos.EVENT_BUS.post(leftClickBlockEvent);
	}

	@SubscribeEvent
	public void onPushOutOfBlocks(PlayerSPPushOutOfBlocksEvent event) {
		if (event.getEntity().equals(mc.player)) {
			PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
			Cosmos.EVENT_BUS.post(pushOutOfBlocksEvent);

			if (pushOutOfBlocksEvent.isCanceled()) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		DeathEvent deathEvent = new DeathEvent(event.getEntity());
		Cosmos.EVENT_BUS.post(deathEvent);
	}

	@SubscribeEvent
	public void onRenderBlockOverlay(RenderBlockOverlayEvent event) {
		RenderOverlayEvent renderOverlayEvent = new RenderOverlayEvent(event.getOverlayType());
		Cosmos.EVENT_BUS.post(renderOverlayEvent);

		if (renderOverlayEvent.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
		RenderFogEvent renderFogEvent = new RenderFogEvent(event.getDensity());
		Cosmos.EVENT_BUS.post(renderFogEvent);

		event.setDensity(renderFogEvent.getDensity());

		if (renderFogEvent.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onFogColor(EntityViewRenderEvent.FogColors event) {
		RenderFogColorEvent fogColorEvent = new RenderFogColorEvent(Color.WHITE);
		Cosmos.EVENT_BUS.post(fogColorEvent);

		// update fog colors
		event.setRed(fogColorEvent.getColor().getRed() / 255F);
		event.setGreen(fogColorEvent.getColor().getGreen() / 255F);
		event.setBlue(fogColorEvent.getColor().getBlue() / 255F);
	}
}
