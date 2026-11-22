package dev.movingcastle.thaumicconcilium.client;

/** Which option this branch builds. The one line that differs between the D8 branches. */
public final class DaggerPoses {
    public static final DaggerPose ACTIVE = new DaggerPoseC();

    private DaggerPoses() {
    }
}
