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
import com.fest.visuals.api.module.setting.ModeSetting;
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

    public final BindSetting key = new BindSetting("\u0411\u0438\u043d\u0434");
    public final ModeSetting processing = new ModeSetting("\u041e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0430").values("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f", "\u0421\u0435\u0440\u0432\u0435\u0440\u043d\u0430\u044f").value("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f");
    public final ModeSetting sttModel = new ModeSetting("\u041c\u043e\u0434\u0435\u043b\u044c STT").values("Light", "Medium", "Heavy").value("Light").setVisible(() -> processing.getValue().equals("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f"));
    public final BooleanSetting useGpu = new BooleanSetting("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c GPU").value(false).setVisible(() -> processing.getValue().equals("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f"));
    public final BooleanSetting cues = new BooleanSetting("\u0417\u0432\u0443\u043a\u043e\u0432\u044b\u0435 \u0441\u0438\u0433\u043d\u0430\u043b\u044b").value(true);
    public final BooleanSetting printReply = new BooleanSetting("\u041f\u0438\u0441\u0430\u0442\u044c \u043e\u0442\u0432\u0435\u0442 \u0432 \u0447\u0430\u0442").value(true);
    public final BooleanSetting voice = new BooleanSetting("Голосовой ответ").value(true);
    public final SliderSetting volume = new SliderSetting("Громкость").value(0.8f).range(0f, 1f).step(0.05f)
            .setVisible(voice::getValue);
    private final String token = "WzABLYQgNXrGH3SD";
    
    private String getServerUrl() {
        return "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f".equals(processing.getValue()) ? "ws://127.0.0.1:8765/jarvis" : "ws://195.179.231.14:8765/jarvis";
    }

    private final JarvisAudioCapture capture = new JarvisAudioCapture();
    private final JarvisAudioPlayer player = new JarvisAudioPlayer();
    private final JarvisClient client = new JarvisClient();

    private boolean holding;

    public JarvisModule() {
        addSettings(key, processing, sttModel, useGpu, voice, volume, cues, printReply);

        client.onResult((json, wav) -> mc.execute(() -> handleResult(json, wav)));
                client.onError(message -> mc.execute(() -> {
            if (message != null && message.contains("ConnectException")) {
                if ("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f".equals(processing.getValue())) {
                    TextUtil.sendMessage("Jarvis: \u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d! \u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u0435 \u043b\u0430\u0443\u043d\u0447\u0435\u0440.");
                } else {
                    TextUtil.sendMessage("Jarvis: \u041d\u0435\u0442 \u0441\u0432\u044f\u0437\u0438 \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c (" + message + ")");
                }
            } else {
                TextUtil.sendMessage("Jarvis: " + message);
            }
        }));
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
        client.connect(getServerUrl());

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
            if ("\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f".equals(processing.getValue())) {
                TextUtil.sendMessage("Jarvis: \u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d! \u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u0435 \u043b\u0430\u0443\u043d\u0447\u0435\u0440.");
            } else {
                TextUtil.sendMessage("Jarvis: \u043d\u0435\u0442 \u0441\u0432\u044f\u0437\u0438 \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c");
            }
            return;
        }

        // The server needs to know what exists on this client — modules with their descriptions,
        // configs, themes — to turn "выключи цветное небо" into the right module.
        JsonObject extra = new JsonObject();
        extra.addProperty("sttModel", sttModel.getValue());
        extra.addProperty("useGpu", useGpu.getValue());
        extra.addProperty("voice", voice.getValue());
        extra.add("context", com.fest.visuals.api.utils.jarvis.JarvisActions.context());
        client.sendUtterance(pcm, JarvisAudioCapture.sampleRate(), token, extra);
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

    /** "%nearest%" and "%attacker%" in a command become real names, chosen on the client. */
    private String resolvePlaceholders(String command) {
        if (mc.player == null || mc.level == null) return command;

        if (command.contains("%attacker%")) {
            var attacker = mc.player.getLastHurtByMob();
            if (!(attacker instanceof net.minecraft.world.entity.player.Player player)) return null;
            command = command.replace("%attacker%", player.getGameProfile().name());
        }
        if (command.contains("%nearest%")) {
            net.minecraft.world.entity.player.Player nearest = null;
            double best = Double.MAX_VALUE;
            for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
                if (other == mc.player) continue;
                double distance = other.distanceToSqr(mc.player);
                if (distance < best) {
                    best = distance;
                    nearest = other;
                }
            }
            if (nearest == null) return null;
            command = command.replace("%nearest%", nearest.getGameProfile().name());
        }
        return command;
    }

    private void runAction(JsonObject action) {
        if (mc.player == null || !action.has("type")) return;

        switch (action.get("type").getAsString()) {
            case "command" -> {
                if (action.has("value")) {
                    String command = resolvePlaceholders(action.get("value").getAsString().replaceFirst("^/", ""));
                    if (command == null) TextUtil.sendMessage("Jarvis: рядом нет игрока для команды");
                    else mc.player.connection.sendCommand(command);
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
            case "setting", "config", "theme" -> {
                String report = com.fest.visuals.api.utils.jarvis.JarvisActions.run(action);
                if (report != null && printReply.getValue()) TextUtil.sendMessage("Jarvis: " + report);
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
