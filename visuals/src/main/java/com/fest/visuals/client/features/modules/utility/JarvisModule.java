package com.fest.visuals.client.features.modules.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.sounds.SoundEvents;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BindSetting;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.StringSetting;
import com.fest.visuals.api.utils.jarvis.JarvisAudioCapture;
import com.fest.visuals.api.utils.jarvis.JarvisAudioPlayer;
import com.fest.visuals.api.utils.jarvis.JarvisClient;
import com.fest.visuals.api.utils.other.SoundUtil;
import com.fest.visuals.api.utils.other.TextUtil;

/**
 * Hold a key, say what you want, let go. The audio goes to the Jarvis voice service (a small
 * websocket server meant to run on a VPS — see {@code jarvis-server/} in the repository) which
 * transcribes it, works out what was meant, and replies with both a spoken answer and a list of
 * actions to run: server commands, a chat message, or toggling another module.
 *
 * <p>The module never interprets the recording itself; it only records, ships the clip, and
 * carries out whatever the server sends back. That keeps the client dumb and the "understanding
 * Minecraft slang" part upgradeable without a mod update.
 */
@ModuleRegister(name = "Jarvis", desc = "Голосовой помощник: зажми клавишу и скажи, что сделать", category = Category.OTHER)
public class JarvisModule extends Module {
    @Getter private static final JarvisModule instance = new JarvisModule();

    /** Below this, a held-and-immediately-released key is almost certainly a misclick, not speech. */
    private static final int MIN_UTTERANCE_BYTES = 16_000 /* Hz */ * 2 /* bytes/sample */ / 5; // 200ms

    public final BindSetting key = new BindSetting("Клавиша");
    public final StringSetting server = new StringSetting("Адрес сервера")
            .value("ws://127.0.0.1:8765/jarvis").placeholder("ws://ip:port/jarvis").maxLength(128);
    public final BooleanSetting voice = new BooleanSetting("Голосовой ответ").value(true);
    public final SliderSetting volume = new SliderSetting("Громкость").value(0.8f).range(0f, 1f).step(0.05f);
    public final BooleanSetting cues = new BooleanSetting("Звуковые сигналы").value(true);
    public final BooleanSetting printReply = new BooleanSetting("Показывать ответ в чат").value(true);
    public final StringSetting token = new StringSetting("Токен доступа")
            .placeholder("необязательно, как на сервере").maxLength(64).secret();

    private final JarvisAudioCapture capture = new JarvisAudioCapture();
    private final JarvisAudioPlayer player = new JarvisAudioPlayer();
    private final JarvisClient client = new JarvisClient();

    private boolean holding;

    public JarvisModule() {
        addSettings(key, server, token, voice, volume, cues, printReply);

        client.onResult((json, wav) -> mc.execute(() -> handleResult(json, wav)));
        client.onError(message -> mc.execute(() -> TextUtil.sendMessage("Jarvis: " + message)));
    }

    @Override
    public void onEvent() {
        addEvents(TickEvent.getInstance().subscribe(new Listener<>(event -> tick())));
    }

    @Override
    public void onDisable() {
        if (holding) stopRecording(false);
        client.close();
    }

    private void tick() {
        if (mc.player == null || key.getValue() == -999) return;

        boolean wanted = mc.gui.screen() == null && isBindDown();

        if (wanted && !holding) {
            startRecording();
        } else if (!wanted && holding) {
            stopRecording(true);
        }
    }

    private boolean isBindDown() {
        int bind = key.getValue();
        return bind < 0
                ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().handle(), bind + 100) == 1
                : com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), bind);
    }

    private void startRecording() {
        client.connect(server.getValue());

        // Marked as holding either way: if the microphone failed to open, the alternative is
        // retrying every tick for as long as the key stays down, which spams the chat 20x/sec.
        holding = true;

        if (!capture.start()) {
            TextUtil.sendMessage("Jarvis: микрофон недоступен");
            return;
        }

        if (cues.getValue()) SoundUtil.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.4f);
    }

    private void stopRecording(boolean send) {
        holding = false;
        byte[] pcm = capture.stop();

        if (cues.getValue()) SoundUtil.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 0.9f);
        if (!send || pcm.length < MIN_UTTERANCE_BYTES) return;

        if (!client.isConnected()) {
            TextUtil.sendMessage("Jarvis: нет связи с сервером (" + server.getValue() + ")");
            return;
        }

        client.sendUtterance(pcm, JarvisAudioCapture.sampleRate(), token.getValue());
    }

    private void handleResult(JsonObject json, byte[] wav) {
        if (printReply.getValue() && json.has("reply") && !json.get("reply").isJsonNull()) {
            String reply = json.get("reply").getAsString();
            if (!reply.isBlank()) TextUtil.sendMessage(reply);
        }

        if (json.has("actions") && json.get("actions").isJsonArray()) {
            for (JsonElement element : json.get("actions").getAsJsonArray()) {
                if (element.isJsonObject()) runAction(element.getAsJsonObject());
            }
        }

        if (voice.getValue() && wav.length > 0) {
            player.play(wav, volume.getValue());
        }
    }

    private void runAction(JsonObject action) {
        if (mc.player == null || !action.has("type")) return;

        switch (action.get("type").getAsString()) {
            case "command" -> {
                if (action.has("value")) {
                    mc.player.connection.sendCommand(action.get("value").getAsString().replaceFirst("^/", ""));
                }
            }
            case "chat" -> {
                if (action.has("text")) {
                    mc.player.connection.sendChat(action.get("text").getAsString());
                }
            }
            case "module" -> {
                if (action.has("name") && action.has("enabled")) {
                    setModuleEnabled(action.get("name").getAsString(), action.get("enabled").getAsBoolean());
                }
            }
            default -> { /* Unknown action from a newer server; nothing safe to do with it. */ }
        }
    }

    private void setModuleEnabled(String name, boolean enabled) {
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getName().equalsIgnoreCase(name)) {
                module.setEnabled(enabled);
                return;
            }
        }
        TextUtil.sendMessage("Jarvis: не нашёл модуль \"" + name + "\"");
    }
}
