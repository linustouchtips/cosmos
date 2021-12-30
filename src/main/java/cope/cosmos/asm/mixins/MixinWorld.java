package cope.cosmos.asm.mixins;

import cope.cosmos.client.Cosmos;
import cope.cosmos.client.events.EntityWorldEvent;
import cope.cosmos.client.events.RenderSkyEvent;
import cope.cosmos.client.events.RenderSkylightEvent;
import cope.cosmos.util.client.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
    public void checkLightFor(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        if (lightType.equals(EnumSkyBlock.SKY)) {
            RenderSkylightEvent renderSkylightEvent = new RenderSkylightEvent();
            Cosmos.EVENT_BUS.post(renderSkylightEvent);

            if (renderSkylightEvent.isCanceled()) {
                info.cancel();
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "spawnEntity", at = @At("RETURN"), cancellable = true)
    public void spawnEntity(Entity entity, CallbackInfoReturnable<Boolean> info) {
        EntityWorldEvent.EntitySpawnEvent entitySpawnEvent = new EntityWorldEvent.EntitySpawnEvent(entity);
        Cosmos.EVENT_BUS.post(entitySpawnEvent);

        if (entitySpawnEvent.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    public void removeEntity(Entity entity, CallbackInfo info) {
        EntityWorldEvent.EntityRemoveEvent entityRemoveEvent = new EntityWorldEvent.EntityRemoveEvent(entity);
        Cosmos.EVENT_BUS.post(entityRemoveEvent);

        if (entityRemoveEvent.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "updateEntity", at = @At("HEAD"), cancellable = true)
    public void updateEntity(Entity entity, CallbackInfo info) {
        EntityWorldEvent.EntityUpdateEvent entityUpdateEvent = new EntityWorldEvent.EntityUpdateEvent(entity);
        Cosmos.EVENT_BUS.post(entityUpdateEvent);

        if (entityUpdateEvent.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    public void getSkyColor(Entity entityIn, float partialTicks, CallbackInfoReturnable<Vec3d> info) {
        RenderSkyEvent renderSkyEvent = new RenderSkyEvent();
        Cosmos.EVENT_BUS.post(renderSkyEvent);

        if (renderSkyEvent.isCanceled()) {
            info.cancel();
            info.setReturnValue(new Vec3d(ColorUtil.getPrimaryColor().getRed(), ColorUtil.getPrimaryColor().getGreen(), ColorUtil.getPrimaryColor().getBlue()));
        }
    }
}
