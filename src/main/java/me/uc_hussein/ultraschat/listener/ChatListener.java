package me.uc_hussein.ultraschat.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.uc_hussein.ultraschat.ULTRASChatPlugin;
import me.uc_hussein.ultraschat.chat.ChatFilter;
import me.uc_hussein.ultraschat.mention.MentionService;
import me.uc_hussein.ultraschat.notification.Placeholders;
import me.uc_hussein.ultraschat.settings.PlayerSetting;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.UUID;

/**
 * Chat handling. Only touches the chat event itself: filters its viewers (per-player "chat" setting),
 * optionally re-renders it, and dispatches mentions. Other plugins' messages are never touched.
 */
public final class ChatListener implements Listener {
    private final ULTRASChatPlugin plugin;
    private final ChatFilter filter;
    private volatile boolean formatActive = true;

    public ChatListener(ULTRASChatPlugin plugin) {
        this.plugin = plugin;
        this.filter = new ChatFilter(plugin);
    }

    /** Decides (at startup and after reload) whether the ULTRAS chat format is applied. */
    public void refreshFormatMode() {
        String mode = plugin.cfg().string("chat.format-mode").toUpperCase(Locale.ROOT);
        boolean active = switch (mode) {
            case "NEVER" -> false;
            case "ALWAYS" -> true;
            default -> {
                boolean found = false;
                for (String name : plugin.cfg().stringList("chat.known-chat-plugins")) {
                    if (Bukkit.getPluginManager().isPluginEnabled(name)) {
                        plugin.getLogger().info("Chat plugin '" + name + "' detected - ULTRAS chat format disabled (AUTO mode).");
                        found = true;
                    }
                }
                yield !found;
            }
        };
        formatActive = active;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.cfg().bool("messages.chat", true)) {
            return;
        }
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message());

        if (filter.blocks(plain)) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().error(p, "chat.blocked", null));
            return;
        }
        if (!p.hasPermission("ultraschat.bypass.antispam") && !plugin.antiSpam().chatAllowed(id)) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().error(p, "general.spam", null));
            return;
        }
        // players who disabled chat do not see other players' messages (their own stay visible to all)
        e.viewers().removeIf(a -> a instanceof Player v
                && !v.getUniqueId().equals(id)
                && !plugin.players().isEnabled(v, PlayerSetting.CHAT));

        if (formatActive) {
            e.renderer((source, displayName, message, viewer) -> render(source, displayName, message, viewer));
        }
    }

    private Component render(Player source, Component displayName, Component message, Audience viewer) {
        String lang = viewer instanceof Player v ? plugin.players().language(v.getUniqueId())
                : plugin.languages().defaultLanguage();
        Component name = displayName.colorIfAbsent(plugin.theme().color("player"));
        Component body = highlight(source, message).colorIfAbsent(plugin.theme().color("chatmsg"));
        return plugin.messages().render(lang, plugin.languages().text(lang, "chat.format"),
                Placeholders.create().put("player", name).put("message", body));
    }

    private Component highlight(Player source, Component message) {
        if (!plugin.mentions().canSend(source)) {
            return message;
        }
        var color = plugin.theme().color("mention");
        return message.replaceText(TextReplacementConfig.builder()
                .match(MentionService.PATTERN)
                .replacement((match, builder) ->
                        plugin.mentionIndex().contains(match.group(1).toLowerCase(Locale.ROOT)) ? builder.color(color) : builder)
                .build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMention(AsyncChatEvent e) {
        if (!plugin.cfg().bool("messages.mentions", true)) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message());
        if (plain.indexOf('@') < 0) {
            return;
        }
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline()) {
                plugin.mentions().process(p, plain);
            }
        });
    }
}
