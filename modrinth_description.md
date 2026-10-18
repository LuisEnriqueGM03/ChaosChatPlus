**A modern chat for Fabric servers.** Emojis, GIFs, item sharing, @mentions, private messages and safe in-game screenshots, all in a clean, minimalist chat that fits Minecraft's style.

![Emojis & item sharing](https://cdn.modrinth.com/data/FdERZ7mg/images/d19cc17868a61c500e934f4ea1e63a7f4e7729a9.gif)

## ✨ Features

### 😀 Emojis
- **197 built-in pixel-art emojis** (32x32) in 10 categories: faces, hearts, gestures, symbols, objects, Minecraft, animals, food and nature.
- Emoji picker with **Recent** and **Favorites**, plus autocomplete: type `:fire` and pick it.
- Unicode emojis such as 😂 ❤️ 🔥 are converted automatically.
- **Custom server emojis:** drop PNG files into `config/chaoschatplus/emojis/<category>/` and they appear in seconds, with no restart. Uploads by players can require approval with `/emoji approve`.

### 🎞️ GIFs
- **360 built-in GIFs** (text stickers, effects, troll and pranks, emojis, anime-style chibis), all original art.
- **Add your own:** drop GIFs into `config/chaoschatplus/gifs/<category>/`. Each subfolder becomes a category, and big GIFs are shrunk automatically.
- **Favorites:** right-click any GIF to save it.
- **Optional KLIPY search** (the GIF API that replaced Tenor): add a free KLIPY API key to the server config to unlock the *Custom* tab and search millions of GIFs.

![Built-in GIF categories](https://cdn.modrinth.com/data/FdERZ7mg/images/7314b68ddd6e200c4b190fc6ef7c90d14b5bd9af.gif)

![KLIPY GIF search](https://cdn.modrinth.com/data/FdERZ7mg/images/5f043f6f4f710f5d4acad6ddad88cd3c8d40f5ba.gif)

### 🗡️ Item sharing
- Show any item from your inventory with a clean **[View item]** link that shows enchantments, name and lore.
- You can also type `[item]` in chat.

### 📷 Safe screenshots
- The interface offers **in-game captures** rather than arbitrary file uploads. Modified clients can bypass this interface; enable server-side moderation when content filtering is required.
- **Camera mode:** move freely and scroll to zoom before you shoot.
- Report button, automatic hiding after several reports, and a moderation review screen for OPs.
- Optional automatic NSFW check with Sightengine.

![Safe in-game screenshots](https://cdn.modrinth.com/data/FdERZ7mg/images/653e3c67176cca4dda01755fea043328e67a8dba.gif)

### 💬 Mentions and private messages
- **@name** highlights the message and plays a sound for that player. `@everyone`, `@here` and `@rank` can be restricted by permission.
- **Private mode** in the chat bar: pick a player (5 random players plus a search box) and your text, emojis, GIFs, items and screenshots go only to them.

![Private mode](https://cdn.modrinth.com/data/FdERZ7mg/images/aa10ebf3ea16fa0fd9e52cd04cbabf59cb993ffb.gif)

### ☠️ Styled messages
- Styled death messages and join/leave messages, with a special welcome for first-time players.

## 🏷️ Chaos Ranks support (optional)
If the server has **Chaos Ranks**, rank badges appear in the chat and in the chat bar, and permissions can be granted per rank. Without Chaos Ranks, everything works normally and no badges are shown.

## 📦 Requirements
- Minecraft **26.2**, **Java 25**, and the matching Fabric, Forge or NeoForge edition. Fabric also requires **Fabric API**.
- **Required on both the server and every client.** Players without the mod are told which version to install when they try to join.
- No resource pack needed: the emojis and rank badges added by the server are sent through the mod.

## ⚙️ Configuration
Everything is set in `config/chaoschatplus/config.json`:
- Who can use each feature, by OP level and/or Chaos Ranks rank.
- Cooldowns, screenshot size and expiry, mention color and sound.
- KLIPY API key (optional).

Commands: `/chatplus reload`, `/chatplus restore`, `/chatplus shot list|view|delete`, `/emoji list|approve|deny|remove|alias`.

## 🔎 About KLIPY
GIF search in the *Custom* tab is **Powered by KLIPY**. To follow KLIPY's integration requirements, searches and GIFs are loaded directly from KLIPY on each player's game using the server's key, results are shown as provided, and KLIPY GIFs are never stored on disk. Content filtering is configured in the KLIPY Partner Panel. Get a free key at [partner.klipy.com](https://partner.klipy.com/api-keys).

## 📜 Notes
- All emojis, GIFs and icons included are original art made for this mod.
- Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
