package me.mapacheee.revenge.service;

import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.revenge.data.VaultData;
import me.mapacheee.revenge.data.VaultRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class VaultService {

    private final VaultRepository vaultRepository;

    @Inject
    public VaultService(VaultRepository vaultRepository) {
        this.vaultRepository = vaultRepository;
    }

    public int getMaxRows(Player player) {
        for (int i = 5; i >= 2; i--) {
            if (player.hasPermission("revenge.vault.rows." + i)) {
                return i;
            }
        }
        return 1;
    }

    public int getMaxPages(Player player) {
        for (int i = 50; i >= 2; i--) {
            if (player.hasPermission("revenge.vault.page." + i)) {
                return i;
            }
        }
        return 1;
    }

    public CompletableFuture<VaultData> loadVault(Player player, int page) {
        return CompletableFuture.supplyAsync(() -> {
            String uuid = player.getUniqueId().toString();
            return vaultRepository.findOne(
                Filters.and(Filters.eq("uuid", uuid), Filters.eq("page", page))
            );
        });
    }

    public CompletableFuture<Void> saveVault(Player player, int page, Inventory inventory) {
        return CompletableFuture.runAsync(() -> {
            String uuid = player.getUniqueId().toString();
            VaultData existing = vaultRepository.findOne(
                Filters.and(Filters.eq("uuid", uuid), Filters.eq("page", page))
            );

            String encoded = serializeContents(inventory);

            if (existing != null) {
                existing.setContentsBase64(encoded);
                vaultRepository.save(existing);
            } else {
                VaultData newVault = new VaultData(uuid, page, encoded);
                vaultRepository.save(newVault);
            }
        });
    }

    public String serializeContents(Inventory inventory) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(outputStream);

            dataOutput.writeInt(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null) {
                    byte[] bytes = item.serializeAsBytes();
                    dataOutput.writeInt(bytes.length);
                    dataOutput.write(bytes);
                } else {
                    dataOutput.writeInt(0);
                }
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public ItemStack[] deserializeContents(String base64, int size) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[size];
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(data));

            int savedSize = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < savedSize && i < size; i++) {
                int length = dataInput.readInt();
                if (length > 0) {
                    byte[] itemBytes = new byte[length];
                    dataInput.readFully(itemBytes);
                    items[i] = ItemStack.deserializeBytes(itemBytes);
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            return new ItemStack[size];
        }
    }
}
