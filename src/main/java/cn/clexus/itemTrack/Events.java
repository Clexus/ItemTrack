package cn.clexus.itemTrack;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;


public class Events implements Listener {
    DatabaseManager databaseManager;
    NamespacedKey key = new NamespacedKey(ItemTrack.plugin, "ItemTrack-UUID");

    public Events(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void removeItem(String uuid){
        Bukkit.getScheduler().runTaskAsynchronously(ItemTrack.plugin, () -> {
            try {
                databaseManager.removeItem(uuid);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent e) {
        ItemStack item = e.getEntity().getItemStack();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if(container.has(key)){
            String uuid = container.get(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)).getFirst();
            removeItem(uuid);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemDeath(EntityDeathEvent e) {
        ItemStack item = e.getEntity() instanceof Item ? ((Item) e.getEntity()).getItemStack() : null;
        if(item != null){
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if(container.has(key)){
                String uuid = container.get(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)).getFirst();
                removeItem(uuid);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemRemoveFromWorld(EntityRemoveFromWorldEvent e) {
        ItemStack item = e.getEntity() instanceof Item ? ((Item) e.getEntity()).getItemStack() : null;
        if(item != null){
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if(container.has(key)){
                String uuid = container.get(key, PersistentDataType.LIST.listTypeFrom(PersistentDataType.STRING)).getFirst();
                removeItem(uuid);
            }
        }
    }
}
