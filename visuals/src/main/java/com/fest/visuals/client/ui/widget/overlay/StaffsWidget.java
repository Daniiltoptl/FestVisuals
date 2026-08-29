package com.fest.visuals.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import com.fest.visuals.api.system.configs.StaffManager;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.framelimiter.FrameLimiter;
import com.fest.visuals.api.utils.other.ReplaceUtil;
import com.fest.visuals.api.utils.player.PlayerUtil;
import com.fest.visuals.client.ui.widget.ContainerWidget;

import java.awt.Color;
import java.util.*;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();

    public record Staff(String name, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online"),
        NEAR("Near"),
        GM3("Gm3"),
        VANISH("Vanish");

        private final String label;
    }

    @Override
    public String getName() {
        return "Staffs";
    }

    public StaffsWidget() {
        super(100f, 100f);
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();

        for (Staff staff : getStaffList()) {
            Color color = switch (staff.status()) {
                case ONLINE -> UIColors.positiveColor();
                case NEAR -> UIColors.middleColor();
                case GM3, VANISH -> UIColors.negativeColor();
            };

            String label = staff.status().getLabel();

            map.put(staff.name(), new ContainerElement.ColoredString(label, color));
        }
        return map;
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (!mc.isLocalServer()) {
                list.addAll(getOnlineStaff());
                list.addAll(getVanishedPlayers());
            }
            cacheStaffs = list;
        });

        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.connection == null || mc.level == null) return staff;

        for (PlayerInfo player : mc.player.connection.getOnlinePlayers()) {
            PlayerTeam team = player.getTeam();
            if (team == null) continue;

            String name = player.getProfile().name();
            if (!PlayerUtil.isValidName(name)) continue;

            String prefix = ReplaceUtil.replaceSymbols(team.getPlayerPrefix().getString());

            if (StaffManager.getInstance().contains(name) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;

                if (player.getGameMode() == GameType.SPECTATOR) {
                    status = Status.GM3;
                } else if (mc.level.players().stream().anyMatch(p -> p.getGameProfile().name().equals(name))) {
                    status = Status.NEAR;
                }

                staff.add(new Staff(prefix + " " + name, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.level == null || mc.level.getScoreboard() == null || mc.getConnection() == null)
            return vanished;

        Set<String> onlineNames = new HashSet<>();
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            onlineNames.add(entry.getProfile().name());
        }

        for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams()) {
            for (String name : team.getPlayers()) {
                if (!PlayerUtil.isValidName(name)) continue;
                if (!onlineNames.contains(name)) {
                    vanished.add(new Staff(name, Status.VANISH));
                }
            }
        }

        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return (
                        prefix.contains("helper") ||
                        prefix.contains("moder") ||
                        prefix.contains("admin") ||
                        prefix.contains("owner") ||
                        prefix.contains("developer") ||
                        prefix.contains("staff") ||
                        prefix.contains("curator") ||

                        prefix.contains("куратор") ||
                        prefix.contains("разраб") ||
                        prefix.contains("модер") ||
                        prefix.contains("админ") ||
                        prefix.contains("стажер") ||
                        prefix.contains("стажёр") ||
                        prefix.contains("хелпер")
                );
    }
}
