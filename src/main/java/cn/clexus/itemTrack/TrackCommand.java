package cn.clexus.itemTrack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static cn.clexus.itemTrack.ItemTrack.plugin;


public class TrackCommand implements CommandExecutor {
    private final DatabaseManager databaseManager;
    private final NamespacedKey key;

    public TrackCommand(DatabaseManager databaseManager, NamespacedKey key) {
        this.databaseManager = databaseManager;
        this.key = key;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length==0&&!(sender instanceof Player)) {
            sender.sendMessage("该命令仅供玩家使用.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            String itemUUID = args[0];
            trackItemHistory(player, itemUUID);
        } else {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!item.hasItemMeta()) {
                player.sendMessage("请手持一个物品再试");
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta.hasMaxStackSize()) {
                player.sendMessage("可堆叠物品无法追踪历史.");
                return true;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (!container.has(key)) {
                player.sendMessage("该物品无追踪数据");
                return true;
            }

            List<String> storedData = container.get(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING));
            String itemUUID = storedData.getFirst();
            trackItemHistory(player, itemUUID);
        }

        return true;
    }
    public void trackItemHistory(Player player, String ItemUUID) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<String> playerHistory = databaseManager.getItemPlayerHistory(ItemUUID);
                if (playerHistory.isEmpty()) {
                    player.sendMessage("该物品没有历史追踪数据.");
                    return;
                }

                TextComponent.Builder message = Component.text();

                for (int i = 0; i < playerHistory.size(); i++) {
                    String history = playerHistory.get(i);
                    String[] parts = history.split(" \\| ");
                    String playerUUID = parts[0].split(": ")[1];
                    String trackingTime = parts[1].split(": ")[1];
                    if (trackingTime.contains(".")) {
                        trackingTime = trackingTime.split("\\.")[0];
                    }
                    if (i == 0) {
                        String[] itemParts = playerUUID.split("\\$");
                        String itemUUID = itemParts[0];

                        TextComponent itemText = Component.text("物品: ")
                                .append(Component.text(trackingTime)
                                        .color(TextColor.fromHexString("#00FFFF"))
                                        .hoverEvent(HoverEvent.showText(Component.text("物品 UUID: " + itemUUID)))
                                        .clickEvent(ClickEvent.copyToClipboard(itemUUID))
                                );

                        message.append(itemText).append(Component.newline());
                    } else {
                        String playerName = Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName();
                        if (playerName == null) {
                            playerName = "未知";
                        }

                        TextComponent playerText = Component.text("玩家: ")
                                .append(Component.text(playerName)
                                        .color(TextColor.fromHexString("#00FF00"))
                                        .hoverEvent(HoverEvent.showText(Component.text("UUID: " + playerUUID)))
                                        .clickEvent(ClickEvent.copyToClipboard(playerUUID))
                                );

                        TextComponent timeText = Component.text(" | 时间: " + trackingTime)
                                .color(TextColor.fromHexString("#FFFFFF"));

                        playerText = playerText.append(timeText);

                        message.append(playerText).append(Component.newline());
                    }
                }

                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message.build()));

            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("获取物品历史时出现错误."));
                e.printStackTrace();
            }
        });
    }

}

