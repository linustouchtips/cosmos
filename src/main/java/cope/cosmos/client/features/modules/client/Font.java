package cope.cosmos.client.features.modules.client;

import cope.cosmos.client.events.render.gui.RenderFontEvent;
import cope.cosmos.client.features.modules.Category;
import cope.cosmos.client.features.modules.Module;
import cope.cosmos.client.features.setting.Setting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author linustouchtips
 * @since 07/12/2021
 */
public class Font extends Module {
	public static Font INSTANCE;
	
	public Font() {
		super("Font", Category.CLIENT, "Allows you to customize the client font.");
		setDrawn(false);
		setExempt(true);
		INSTANCE = this;
	}

	public static Setting<Boolean> vanilla = new Setting<>("Vanilla", false).setDescription("Overrides the minecraft vanilla font");

	@SubscribeEvent
	public void onFontRender(RenderFontEvent event) {
		if (vanilla.getValue()) {
			// override vanilla font rendering
			event.setCanceled(true);
		}
	}
}
