# Ravengard QOL's

## Development

- Minecraft 26.2
- NeoForge 26.2.0.40-beta
- Java 25

Run the client with `./gradlew runClient` (`gradlew.bat runClient` on Windows).

Press `R` in-game to open the test main screen. The key can be changed in Minecraft's Controls menu.

## Features

- Highlights Common, Uncommon, and Rare items in open containers, the player inventory, and the hotbar.
- Press `F8` while hovering an item in a container to inspect every serialized item component. With no screen open, `F8` inspects the held item. The key can be changed in Controls.
- The item inspector shows the rarity detection source and can copy the complete component dump to the clipboard.
- Reads Ravengard rarity from `minecraft:tooltip_style` and sell prices from the number before `Crowns` in `minecraft:lore`.
- Displays a scrollable, rarity-sorted Loot Ledger beside inventories and containers, including item sell prices and total crown value.
