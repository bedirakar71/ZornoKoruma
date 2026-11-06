package dev.zorno.guard;

import java.lang.reflect.Method;
import java.util.UUID;

/** ViaVersion varsa 1.8 (ve altı) istemcileri tanır; bunlar blok yaparken vurabildiği için ilgili kontrolden muaf tutulur. */
public final class ViaCompat {
    private boolean tried;
    private Object api;
    private Method getVersion;

    private void init() {
        tried = true;
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            api = via.getMethod("getAPI").invoke(null);
            getVersion = Class.forName("com.viaversion.viaversion.api.ViaAPI").getMethod("getPlayerVersion", UUID.class);
        } catch (Throwable t) {
            api = null;
            getVersion = null;
        }
    }

    public boolean isLegacy(UUID id) {
        if (!tried) init();
        if (api == null || getVersion == null) return false;
        try {
            int v = (Integer) getVersion.invoke(api, id);
            return v > 0 && v < 107; // 107 = 1.9
        } catch (Throwable t) {
            return false;
        }
    }
}
