// Created by ridhoae303

package com.ridhoae303.expert;

import android.content.Context;

public final class Takane {
    /*
     * librena is loaded centrally by NativeConfig. Do not load a second
     * native library here; Takane.b(...) is exported by librena itself.
     * The native verifier receives the Context directly, so no additional
     * Java helper method is required.
     */
    public static native boolean b(Context ctx);

    private Takane() {
    }
}
