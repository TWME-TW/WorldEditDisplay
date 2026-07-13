package dev.twme.worldeditdisplay.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerSettingsCommandTest {

    private PlayerSettingsCommand command;

    @BeforeEach
    void setUp() {
        command = new PlayerSettingsCommand(null);
    }

    @Test
    void parseValueAcceptsOnlyStrictBooleans() {
        assertEquals(Boolean.TRUE, command.parseValue("fill_enabled", "true"));
        assertEquals(Boolean.FALSE, command.parseValue("fill_enabled", "FALSE"));
        assertNull(command.parseValue("fill_enabled", "maybe"));
    }

    @Test
    void parseValueNormalizesHexColor() {
        assertEquals("#FF0000FF", command.parseValue("edge_color", "FF0000FF"));
        assertNull(command.parseValue("edge_color", "not-a-color"));
    }

    @Test
    void settingWhitelistRejectsUnknownKeys() {
        assertTrue(command.isValidSetting("cuboid", "edge_color"));
        assertFalse(command.isValidSetting("cuboid", "unknown_setting"));
        assertFalse(command.isValidSetting("missing", "edge_color"));
    }

    @Test
    void onlySettingsCommandsRequireSettingsPermission() {
        assertTrue(command.requiresSettingsPermission("set"));
        assertTrue(command.requiresSettingsPermission("reloadplayer"));
        assertTrue(command.requiresSettingsPermission("language"));

        assertFalse(command.requiresSettingsPermission("toggle"));
        assertFalse(command.requiresSettingsPermission("render"));
        assertFalse(command.requiresSettingsPermission("share"));
        assertFalse(command.requiresSettingsPermission("view"));
    }

    @Test
    void subCommandPermissionChecksMatchCommandCategories() {
        CommandSender sender = senderWithPermissions("worldeditdisplay.use.share", "worldeditdisplay.use.view.list");

        assertFalse(command.canUseSubCommand(sender, "set"));
        assertFalse(command.canUseSubCommand(sender, "toggle"));
        assertFalse(command.canUseSubCommand(sender, "render"));

        assertTrue(command.canUseSubCommand(sender, "share"));
        assertTrue(command.canUseSubCommand(sender, "view"));
    }

    @Test
    void viewRouteAcceptsHideOnlyPermission() {
        CommandSender sender = senderWithPermissions("worldeditdisplay.use.view.hide");

        assertTrue(command.canUseSubCommand(sender, "view"));
    }

    @Test
    void shareSubCommandPermissionChecksMatchSpecificPermissions() {
        ShareCommand shareCommand = new ShareCommand(null);
        org.bukkit.entity.Player player = playerWithPermissions("worldeditdisplay.use.share.invite", "worldeditdisplay.use.share.list");

        assertTrue(shareCommand.canUseSubCommand(player, "invite"));
        assertTrue(shareCommand.canUseSubCommand(player, "list"));
        assertFalse(shareCommand.canUseSubCommand(player, "accept"));
        assertFalse(shareCommand.canUseSubCommand(player, "revoke"));
        assertFalse(shareCommand.canUseSubCommand(player, "unwatch"));
    }

    @Test
    void viewSubCommandPermissionChecksMatchSpecificPermissions() {
        ViewCommand viewCommand = new ViewCommand(null);
        org.bukkit.entity.Player player = playerWithPermissions("worldeditdisplay.use.view.hide", "worldeditdisplay.use.view.label");

        assertTrue(viewCommand.canUseSubCommand(player, "hide"));
        assertTrue(viewCommand.canUseSubCommand(player, "hideall"));
        assertTrue(viewCommand.canUseSubCommand(player, "unhide"));
        assertTrue(viewCommand.canUseSubCommand(player, "label"));
        assertFalse(viewCommand.canUseSubCommand(player, "list"));
    }

    private CommandSender senderWithPermissions(String... permissions) {
        return proxyWithPermissions(CommandSender.class, permissions);
    }

    private org.bukkit.entity.Player playerWithPermissions(String... permissions) {
        return proxyWithPermissions(org.bukkit.entity.Player.class, permissions);
    }

    private <T> T proxyWithPermissions(Class<T> type, String... permissions) {
        Set<String> permissionSet = Set.of(permissions);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("hasPermission") && args != null && args.length == 1) {
                Object permission = args[0];
                if (permission instanceof String name) return permissionSet.contains(name);
                if (permission instanceof org.bukkit.permissions.Permission perm) return permissionSet.contains(perm.getName());
            }
            if (method.getName().equals("toString")) return "TestCommandSender";
            if (method.getReturnType().equals(boolean.class)) return false;
            return null;
        };
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
            handler));
    }
}
