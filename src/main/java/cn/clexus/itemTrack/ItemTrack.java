package cn.clexus.itemTrack;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class ItemTrack extends JavaPlugin {
    public static ItemTrack plugin;
    private DatabaseManager databaseManager;
    public void asyncDatabaseOperation(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                task.run();
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(this, () -> {
                    e.printStackTrace();
                });
            }
        });
    }
    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            databaseManager = new DatabaseManager(getDataFolder().getAbsolutePath() + "/database.db");
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
        plugin = this;

        NamespacedKey key = new NamespacedKey(this, "ItemTrack-UUID");

        this.getCommand("itemtrack").setExecutor(new TrackCommand(databaseManager, key));
        getServer().getPluginManager().registerEvents(new Events(databaseManager),this);

        Bukkit.getScheduler().runTaskTimer(this, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission("itemtrack.notrack")) {
                ItemStack[] items = player.getInventory().getContents();
                for (ItemStack item : items) {
                    if (item == null || item.getType() == Material.AIR || item.getItemMeta().hasMaxStackSize()) continue;
                    ItemMeta meta = item.getItemMeta();
                    PersistentDataContainer container = meta.getPersistentDataContainer();

                    if (!container.has(key)) {
                        String randomUUID = UUID.randomUUID().toString();
                        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().toString();

                        asyncDatabaseOperation(() -> {
                            try {
                                databaseManager.createItemWithPlayer(randomUUID, player, displayName);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        container.set(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING),
                                List.of(randomUUID, player.getUniqueId().toString()));
                        item.setItemMeta(meta);
                    } else {
                        List<String> storedData = container.get(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING));
                        String itemUUID = storedData.get(0);
                        String previousPlayerUUID = storedData.get(1);

                        if (!previousPlayerUUID.equals(player.getUniqueId().toString())) {
                            asyncDatabaseOperation(() -> {
                                try {
                                    databaseManager.addPlayerToItem(itemUUID, player);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            container.set(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING),
                                    List.of(itemUUID, player.getUniqueId().toString()));
                            item.setItemMeta(meta);
                        }
                    }
                }
            }
        }), 0, 5);

    }

    @Override
    public void onDisable() {
        try {
            if (databaseManager != null) {
                databaseManager.closeConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
