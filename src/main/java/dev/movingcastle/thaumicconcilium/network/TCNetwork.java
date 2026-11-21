package dev.movingcastle.thaumicconcilium.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** All custom packets. */
public final class TCNetwork {
    private static final String VERSION = "1";

    private TCNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(FireworkBurstPayload.TYPE, FireworkBurstPayload.STREAM_CODEC, FireworkBurstPayload::handle);
        registrar.playToServer(EtherealRangePayload.TYPE, EtherealRangePayload.STREAM_CODEC, EtherealRangePayload::handle);
        registrar.playToServer(MillSelectPayload.TYPE, MillSelectPayload.STREAM_CODEC, MillSelectPayload::handle);
    }
}
