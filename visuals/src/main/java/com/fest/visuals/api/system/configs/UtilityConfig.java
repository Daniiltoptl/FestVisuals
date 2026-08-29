package com.fest.visuals.api.system.configs;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import com.fest.visuals.api.system.backend.ClientInfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Хранит текстовые значения для Utility-модулей (Auto Auth, Auto Join,
 * Auto Resell, Death Cords), для которых в ClickGUI пока нет текстового
 * поля ввода. Редактируется напрямую в файле {@code other/utility.json}.
 */
@Getter
@Setter
public class UtilityConfig {
    @Getter private static final UtilityConfig instance = new UtilityConfig();

    private final File file = new File(ClientInfo.CONFIG_PATH_OTHER + "/utility.json");
    private final GsonBuilder gson = new GsonBuilder().setPrettyPrinting();

    /** Пароль, используемый Auto Auth для /login и /register. */
    private String authPassword = "";

    /** Regex-паттерны серверных сообщений, при которых Auto Auth логинится. */
    private String authLoginPattern = "(?i)please (log|login) using /login";

    /** Regex-паттерны серверных сообщений, при которых Auto Auth регистрируется. */
    private String authRegisterPattern = "(?i)please register using /register";

    /** Команда без ведущего '/', отправляемая один раз после входа на сервер. */
    private String joinCommand = "";

    /** Regex-паттерн сообщения о том, что лот на аукционе не продался. */
    private String resellTrigger = "(?i)(your auction|item) (has )?(expired|returned)";

    /** Команда без ведущего '/', перевыставляющая лот. */
    private String resellCommand = "";

    /** Отправлять координаты смерти в реальный чат сервера, а не только себе. */
    private boolean deathCordsPublic = false;

    public static class Data {
        public String authPassword = "";
        public String authLoginPattern = "(?i)please (log|login) using /login";
        public String authRegisterPattern = "(?i)please register using /register";
        public String joinCommand = "";
        public String resellTrigger = "(?i)(your auction|item) (has )?(expired|returned)";
        public String resellCommand = "";
        public boolean deathCordsPublic = false;
    }

    public void load() {
        if (!file.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Data data = gson.create().fromJson(reader, Data.class);
            if (data == null) return;

            authPassword = data.authPassword;
            authLoginPattern = data.authLoginPattern;
            authRegisterPattern = data.authRegisterPattern;
            joinCommand = data.joinCommand;
            resellTrigger = data.resellTrigger;
            resellCommand = data.resellCommand;
            deathCordsPublic = data.deathCordsPublic;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void save() {
        File parentDir = file.getParentFile();
        if (parentDir != null) parentDir.mkdirs();

        Data data = new Data();
        data.authPassword = authPassword;
        data.authLoginPattern = authLoginPattern;
        data.authRegisterPattern = authRegisterPattern;
        data.joinCommand = joinCommand;
        data.resellTrigger = resellTrigger;
        data.resellCommand = resellCommand;
        data.deathCordsPublic = deathCordsPublic;

        try (FileWriter writer = new FileWriter(file)) {
            gson.create().toJson(data, writer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
