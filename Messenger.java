package dev.zorno.guard;

import dev.zorno.guard.core.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/** MiniMessage ile renkli mesajlar. Değişkenler <ad> biçiminde, kullanıcı girdisi güvenli (unparsed) eklenir. */
public final class Messenger {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Settings s;

    public Messenger(Settings s) {
        this.s = s;
    }

    public String raw(String key) {
        String v = s.messages.get(key);
        return v == null ? "<red>[" + key + "]</red>" : v;
    }

    public Component parse(String template, String... kv) {
        TagResolver[] r = new TagResolver[kv.length / 2];
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r[i / 2] = Placeholder.unparsed(kv[i], kv[i + 1]);
        }
        return MM.deserialize(template.replace("{prefix}", s.prefix), r);
    }

    public void send(CommandSender to, String template, String... kv) {
        to.sendMessage(parse(template, kv));
    }

    public void sendKey(CommandSender to, String key, String... kv) {
        send(to, raw(key), kv);
    }
}
