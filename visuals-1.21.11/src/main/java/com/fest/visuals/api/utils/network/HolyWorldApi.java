package com.fest.visuals.api.utils.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HolyWorldApi {
    private static final Gson GSON = new Gson();
    private static final String CLIENT_ID = "festvisuals";

    public record FeatureControlPayload(String json) implements CustomPacketPayload {
        public static final Type<FeatureControlPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("liteapi", "feature-control"));
        public static final StreamCodec<FriendlyByteBuf, FeatureControlPayload> CODEC = CustomPacketPayload.codec(
            FeatureControlPayload::write, FeatureControlPayload::new
        );

        public FeatureControlPayload(FriendlyByteBuf buf) {
            this(readRaw(buf));
        }

        private static String readRaw(FriendlyByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBytes(json.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(FeatureControlPayload.ID, FeatureControlPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FeatureControlPayload.ID, FeatureControlPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(FeatureControlPayload.ID, (payload, context) -> {
            try {
                JsonObject response = GSON.fromJson(payload.json(), JsonObject.class);
                if (response.has("ok") && response.get("ok").getAsBoolean()) {
                    JsonObject resPayload = response.getAsJsonObject("payload");
                    if (resPayload != null && resPayload.has("blocklist")) {
                        JsonArray blocklist = resPayload.getAsJsonArray("blocklist");
                        List<String> blocked = new ArrayList<>();
                        blocklist.forEach(e -> blocked.add(e.getAsString().toLowerCase()));

                        context.client().execute(() -> {
                            for (Module m : ModuleManager.getInstance().getAllModules()) {
                                if (blocked.contains(m.getName().toLowerCase()) || blocked.contains(m.getClass().getSimpleName().toLowerCase().replace("module", ""))) {
                                    m.setHidden(true);
                                    if (m.isEnabled()) {
                                        m.setEnabled(false);
                                    }
                                } else {
                                    m.setHidden(false);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            for (Module m : ModuleManager.getInstance().getAllModules()) {
                m.setHidden(false);
            }

            JsonObject request = new JsonObject();
            request.addProperty("id", UUID.randomUUID().toString());
            request.addProperty("method", "checkFeatures");

            JsonObject p = new JsonObject();
            p.addProperty("client", CLIENT_ID);

            JsonArray features = new JsonArray();
            for (Module m : ModuleManager.getInstance().getAllModules()) {
                features.add(m.getName().toLowerCase());
            }
            p.add("features", features);
            request.add("payload", p);

            ClientPlayNetworking.send(new FeatureControlPayload(GSON.toJson(request)));
        });
    }
}
