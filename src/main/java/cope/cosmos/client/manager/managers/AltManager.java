package cope.cosmos.client.manager.managers;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import cope.cosmos.asm.mixins.accessor.IMinecraft;
import cope.cosmos.client.alts.AltEntry;
import cope.cosmos.client.manager.Manager;
import cope.cosmos.util.Wrapper;
import net.minecraft.util.Session;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class AltManager extends Manager implements Wrapper {

	private static final List<AltEntry> alts = new ArrayList<>();

	public AltManager() {
		super("AltManager", "Manages alternate accounts for easy login");
	}

	public static YggdrasilUserAuthentication logIn(String email, String password, boolean setSession) {
		YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);
		auth.setUsername(email);
		auth.setPassword(password);

		new Thread(() -> {
			try {
				auth.logIn();
				if (setSession) {
					Session session = new Session(auth.getSelectedProfile().getName(), auth.getSelectedProfile().getId().toString(), auth.getAuthenticatedToken(), "mojang");
					if (session != null) {
						((IMinecraft) mc).setSession(session);
					}
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}).start();

		return auth;
	}
	
	public static List<AltEntry> getAlts() {
		return AltManager.alts;
	}
}
