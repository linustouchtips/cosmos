package cope.cosmos.client.ui.altmanager;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.net.Proxy;

/**
 * @author Wolfsurge
 */
public class Alt {

    // The type of alt
    private AltType altType;
    // The email of the alt
    private String email;
    // The password of the alt
    private String password;
    // The alt session, for quick login
    private Session altSession;

    // Creates a new alt
    public Alt(String altEmail, String altPassword, AltType altType) {
        setEmail(altEmail);
        setPassword(altPassword);
        setAltType(altType);
        // Create a new session when, and only when, the alt is created
        setAltSession(createSession());
    }

    /**
     * Creates a new Minecraft session.
     * @return A new Minecraft session, if we were able to create one
     */
    private Session createSession() {
        if(getAltType() == AltType.Microsoft) {
            // Create new authenticator
            MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
            try {
                // Create new result
                MicrosoftAuthResult result = authenticator.loginWithCredentials(email, password);

                // Return created session
                return new Session(result.getProfile().getName(), result.getProfile().getId(), result.getAccessToken(),"legacy");
            } catch (MicrosoftAuthenticationException e) { e.printStackTrace(); }

        } else if(getAltType() == AltType.Mojang) {
            // Create auth variables
            YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(Proxy.NO_PROXY, "");
            YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication)service.createUserAuthentication(Agent.MINECRAFT);

            // Set email and password
            auth.setUsername(getEmail());
            auth.setPassword(getPassword());

            // Attempt login
            try {
                auth.logIn();

                // Return created session
                return new Session(auth.getSelectedProfile().getName(), auth.getSelectedProfile().getId().toString(), auth.getAuthenticatedToken(), "mojang");
            } catch (AuthenticationException localAuthenticationException) {
                localAuthenticationException.printStackTrace();
            }
        } else if(getAltType() == AltType.Cracked) {
            // Returns a session without proper auth. Email serves as the username.
            return new Session(getEmail(), "", "", "legacy");
        }

        return null;
    }

    /**
     * Type of alt
     */
    public enum AltType {
        // Premium Microsoft account
        Microsoft,
        // Premium Mojang account
        Mojang,
        // Cracked account
        Cracked
    }

    /**
     * Gets the alt type
     * @return The alt type
     */
    public AltType getAltType() {
        return altType;
    }

    /**
     * Sets the alt type
     * @param altType The new alt type
     */
    public void setAltType(AltType altType) {
        this.altType = altType;
    }

    /**
     * Gets the email of the alt
     * @return The email of the alt
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email of the alt
     * @param email The new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the password of the alt
     * @return The password of the alt
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the alt
     * @param password The new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the alt's session
     * @return The alt's session
     */
    public Session getAltSession() {
        return altSession;
    }

    /**
     * Sets the alt's session
     * @param altSession The new session
     */
    public void setAltSession(Session altSession) {
        this.altSession = altSession;
    }
}