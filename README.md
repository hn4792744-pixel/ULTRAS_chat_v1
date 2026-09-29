# ULTRAS_Chat_v1

Calm, premium chat / messages / notifications for Paper — by **UC_Hussein**.
Replaces vanilla death, join, quit and advancement messages with short, coloured `ᴜʟᴛʀᴀs │` messages, adds a
per-player settings GUI, private messages, @mentions, sounds and HUD notifications.

> **Status: NOT compiled or tested.** This project was written in an environment with no JDK 25, no Gradle and no
> network, so it has never been built or run. Expect to fix a few compile errors on the first `build`
> (most likely small Paper-API details). Please report or fix them before using it on a live server.

## Requirements
- Paper **26.2** (primary target), Java **25**
- Best-effort Paper 1.21.x: build with `-Ptarget121` (Java 21, `api-version: 1.21`). Untested. The code only uses
  Paper/Adventure APIs that exist in both, and no NMS, packets or reflection.

## Build
No Gradle wrapper jar is included (it is a binary). With Gradle 9.1+ and JDK 25 installed:
```
gradle wrapper --gradle-version 9.2.1     # once
./gradlew build
```
Output: `build/libs/ULTRAS_Chat_v1.jar` → put in `plugins/`. 1.21 build: `./gradlew build -Ptarget121 [-PlegacyApi=1.21.8-R0.1-SNAPSHOT]`.

## Features
- Death messages for 27 death types (3 random short variants each), vanilla message removed
- Join / quit (`+ Name` green, `- Name` red), achievements (task / goal / challenge)
- Chat format `ᴜʟᴛʀᴀs │ Name › message`, per-player "hide chat" (own messages stay visible to others)
- `/msg` `/tell` `/w` `/whisper`, `/reply` `/r`, HUD notification + sound, "not receiving messages" handling
- `@Name` mentions: highlight, ActionBar, sound, per-player toggles
- Two languages (`en`, `ar`), **chosen per player**
- Settings GUI (categories, ON/OFF colours, language button, reset confirmation), admin view of other players
- Every event has its own configurable sound; every setting is per player
- Anti-spam, optional word filter (off), daily log files, safe reload

## Commands
| Command | Description |
|---|---|
| `/chat` , `/chat help` | Short command list |
| `/chat setting` | Open your settings GUI |
| `/chat setting <player>` | Admin: open another player's settings |
| `/chat toggle <setting> [on\|off]` | Toggle a setting |
| `/chat language [en\|ar]` | Switch your language |
| `/chat reset` | Reset your settings (confirmation GUI; `/chat reset confirm` if GUI disabled) |
| `/chat reload` | Admin: reload config / messages / GUI / sounds (player data is kept) |
| `/msg <player> <message>` (`/tell /w /whisper`), `/reply <message>` (`/r`) | Private messages |

Toggle keys: `chat`, `plugin-messages`, `messages` (all ULTRAS messages), `sounds` (all sounds), `death`, `death-sounds`,
`join-quit`, `join-quit-sounds`, `achievements`, `achievement-sounds`, `msg`, `msg-sounds`, `mentions`, `mention-sounds`
(aliases: `join`, `quit`, `pm`, …). Join and quit are combined into `join-quit`.

Behaviour notes: with `plugin-messages` (or `messages`) off, command *confirmations* are hidden but the command still runs;
errors are always shown. With `messages` off, ULTRAS death/join/quit/achievement messages and mention/PM HUD notifications
are hidden; the private message text itself and normal chat are still delivered.

## Permissions
| Permission | Default | Purpose |
|---|---|---|
| `ultraschat.use` | true | `/chat`, toggles |
| `ultraschat.chat` | true | chat setting / ULTRAS chat format |
| `ultraschat.msg` | true | private messages |
| `ultraschat.mention` | true | trigger mentions |
| `ultraschat.settings` | true | settings GUI |
| `ultraschat.reload` | op | `/chat reload` |
| `ultraschat.admin.settings` | op | `/chat setting <player>` |
| `ultraschat.admin` | op | all of the admin permissions below |
| `ultraschat.bypass.antispam`, `ultraschat.msg.bypass`, `ultraschat.vanish.see`, `ultraschat.mention.staff` | op | bypasses / staff features |

Tab completion only offers what the sender may use (`reload` and player names for `setting` are hidden from normal players).

## Configuration
```
plugins/ULTRAS_Chat/
├── config.yml            language-neutral: switches, colors, sounds, death types, anti-spam, defaults, logging
├── messages/             messages_xx, death_xx, join_xx, chat_xx, private_xx, achievement_xx, settings_xx  (xx = en, ar)
├── gui/                  settings.yml, player-settings.yml, confirm-reset.yml, admin-settings.yml
├── player-data.yml       per-player language and toggles
└── logs/                 daily log files (no message contents)
```
- **Disable one message:** `death.types.fall.enabled: false` → neither the ULTRAS nor the vanilla message appears.
- **Disable a whole system:** `messages.death: false` etc. Then `fallback.show-vanilla-message-when-disabled` decides whether vanilla shows.
- Templates use MiniMessage; `%player% %killer% %victim% %sender% %receiver% %message% %achievement% %target% %language% …` are placeholders.
  Color tags come from `config.yml → colors` (`<success> <error> <info> <warning> <death> <pm> <mention> <muted> <sep> …`).
- English text is rendered in small caps automatically (`meta.small-caps` in the language file, `style.small-caps` in config).
- GUI files: size, slot, material, `type` (`TOGGLE OPEN BACK CLOSE LANGUAGE RESET INFO YES NO`), name/lore templates.
  Button texts come from `settings_xx.yml` so they follow each player's language.
- Bad YAML or wrong values never crash the plugin: defaults are used, the problem is logged and shown to admins.

## Storage
`player-data.yml` (YAML instead of SQLite: tiny data, human-readable, no driver). Only changed settings are stored.
Saves are queued ~2 s after a change, written atomically on a background thread, and flushed on shutdown.

## Compatibility notes
- Only vanilla messages that ULTRAS replaces are removed (death, join, quit, advancement). Other plugins' messages are never touched.
- If another plugin already removed a join/quit/death message (e.g. vanish plugins), ULTRAS respects that (`respect-suppressed-message`).
- Chat: `chat.format-mode: AUTO` skips the ULTRAS format when a known chat plugin is installed (viewer filtering and mentions still work).
- **Command conflicts:** ULTRAS registers `/msg /tell /w /whisper /r` through `plugin.yml` and never removes another plugin's commands.
  If another plugin owns one of them, a warning is printed at startup; you can still use `/ultras_chat:msg`, or remove the alias
  from the other plugin.
- Vanished players (metadata `vanished` or Bukkit `hidePlayer`) cannot be mentioned/messaged by players who cannot see them.

## Troubleshooting
- Check `plugins/ULTRAS_Chat/logs/` and the console for "config warning" lines; `/chat reload` prints the first ones.
- Missing language keys fall back to English, then to the key name.
- Vanilla messages still appear → check the master switch (`messages.*`) and `fallback.show-vanilla-message-when-disabled`.

---
## بالعربية (ملخص)
إضافة شات وإشعارات هادئة وفخمة لسيرفر Paper. تستبدل رسائل الموت والدخول والخروج والإنجازات برسائل `ᴜʟᴛʀᴀs │`،
وتضيف رسائل خاصة ومنشن وأصوات وواجهة إعدادات (`/chat setting`) ولغة مستقلة لكل لاعب (`/chat language`).
**تنبيه:** لم يتم بناء المشروع أو اختباره فعليًا (لا يوجد JDK 25 ولا Gradle في بيئة الكتابة). ابنِه بـ `./gradlew build`
بعد توليد الـ wrapper وأصلح أي أخطاء تجميع صغيرة قبل الاستخدام.
