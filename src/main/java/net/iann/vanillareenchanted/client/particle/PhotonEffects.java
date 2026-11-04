package net.iann.vanillareenchanted.client.particle;

import com.lowdragmc.photon.client.fx.FXRuntime;

/** Shared client-only cleanup for Photon effects. Use instead of FXRuntime.destroy. */
public final class PhotonEffects {
    private PhotonEffects() {}

    /** Stop audio and emission, allowing existing particles to finish. */
    public static void finish(FXRuntime runtime) {
        destroy(runtime, false);
    }

    /** Stop audio and remove particles immediately, including during world cleanup. */
    public static void stop(FXRuntime runtime) {
        destroy(runtime, true);
    }

    private static void destroy(FXRuntime runtime, boolean immediate) {
        if (runtime == null) return;
        // Photon 2.2.8 can evaluate the timeline again during particle cleanup.
        // Disable dispatch BEFORE destroy so a stopped audio clip cannot restart.
        runtime.getTimelinePlayer().setAudioDispatch(false);
        runtime.destroy(immediate);
    }
}
