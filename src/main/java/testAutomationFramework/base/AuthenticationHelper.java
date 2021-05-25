package testAutomationFramework.base;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.identityconnectors.common.security.GuardedString;

import java.nio.file.Path;

public class AuthenticationHelper {
    private static final Logger LOG = LogManager.getLogger(AuthenticationHelper.class);
    private static final CookieStore cookieStore = new BasicCookieStore();
    private static GuardedString atcUserName;
    private static boolean atcUserNameSet = false;
    private static GuardedString atcPassword;
    private static boolean atcPasswordSet = false;
    private static Path cookieFilePath;
    private static boolean useCookies;
    
    public static void setAtcPassword(char[] atcPasswordToSet){
        if(atcPasswordSet){
            LOG.warn("ATC password has already been set");
        }else {
            atcPassword = new GuardedString(atcPasswordToSet);
            atcPassword.makeReadOnly();
            atcPasswordSet = true;
        }
    }

    public static GuardedString getAtcPassword(){
        return atcPassword;
    }
    
    public static void setAtcUserName(char[] atcUserNameToSet){
        if(atcUserNameSet){
            LOG.warn("ATC UserName has already been set");
        }else {
            atcUserName = new GuardedString(atcUserNameToSet);
            atcUserName.makeReadOnly();
            atcUserNameSet = true;
        }
    }

    public static  GuardedString getAtcUserName(){
        return atcUserName;
    }

    public static boolean atcCredentialsHaveNotBeenSet(){
        return !atcUserNameSet || !atcPasswordSet;
    }
    public static CookieStore getCookieStore() {
      return cookieStore;
    }
}
