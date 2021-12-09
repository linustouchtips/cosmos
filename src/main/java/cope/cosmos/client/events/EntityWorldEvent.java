package cope.cosmos.client.events;

import net.minecraft.entity.Entity;
import cope.cosmos.event.listener.Event;

public class EntityWorldEvent extends Event {

    private final Entity entity;

    public EntityWorldEvent(Entity entity) {
        this.entity = entity;
    }

    public static class EntitySpawnEvent extends EntityWorldEvent {
        public EntitySpawnEvent(Entity entity) {
            super(entity);
        }
    }

    public static class EntityRemoveEvent extends EntityWorldEvent {
        public EntityRemoveEvent(Entity entity) {
            super(entity);
        }
    }

    public Entity getEntity() {
        return entity;
    }
}
