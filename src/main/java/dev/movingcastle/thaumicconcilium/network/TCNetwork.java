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
        registrar.playToServer(BraceletCastPayload.TYPE, BraceletCastPayload.STREAM_CODEC, BraceletCastPayload::handle);
        registrar.playToClient(BraceletFxPayload.TYPE, BraceletFxPayload.STREAM_CODEC, BraceletFxPayload::handle);
        registrar.playToClient(LithographSyncPayload.TYPE, LithographSyncPayload.STREAM_CODEC, LithographSyncPayload::handle);
        registrar.playToClient(QuicksilverFlowPayload.TYPE, QuicksilverFlowPayload.STREAM_CODEC, QuicksilverFlowPayload::handle);
        registrar.playToClient(PartyStatusPayload.TYPE, PartyStatusPayload.STREAM_CODEC, PartyStatusPayload::handle);
        registrar.playToClient(LithographerZapPayload.TYPE, LithographerZapPayload.STREAM_CODEC, LithographerZapPayload::handle);
        registrar.playToClient(EnslavePayload.TYPE, EnslavePayload.STREAM_CODEC, EnslavePayload::handle);
        registrar.playToClient(CrimsonStatePayload.TYPE, CrimsonStatePayload.STREAM_CODEC, CrimsonStatePayload::handle);
        registrar.playToServer(PontifexTogglePayload.TYPE, PontifexTogglePayload.STREAM_CODEC, PontifexTogglePayload::handle);
        registrar.playToClient(BindingPayload.TYPE, BindingPayload.STREAM_CODEC, BindingPayload::handle);
        registrar.playToServer(BraceletFocusPayload.TYPE, BraceletFocusPayload.STREAM_CODEC, BraceletFocusPayload::handle);
        registrar.playToClient(DaggerFxPayload.TYPE, DaggerFxPayload.STREAM_CODEC, DaggerFxPayload::handle);
        registrar.playToServer(DaggerStabPayload.TYPE, DaggerStabPayload.STREAM_CODEC, DaggerStabPayload::handle);
        registrar.playToClient(VesselFlashPayload.TYPE, VesselFlashPayload.STREAM_CODEC, VesselFlashPayload::handle);
        registrar.playToClient(GuardFxPayload.TYPE, GuardFxPayload.STREAM_CODEC, GuardFxPayload::handle);
    }
}
