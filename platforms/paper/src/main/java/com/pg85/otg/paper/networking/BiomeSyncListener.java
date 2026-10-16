package com.pg85.otg.paper.networking;

import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.biome.ColorSet;
import com.pg85.otg.util.biome.ColorThreshold;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.Arrays;
import java.util.Locale;

/**
 * Sends the biome settings that vanilla doesn't sync (fog density, grass/foliage/water color noise) to clients that
 * listen on the otg:spigot channel, the same format as OTG 1.18 used on Spigot/Paper.
 */
public class BiomeSyncListener implements Listener {
    public static final String CHANNEL = Constants.MOD_ID_SHORT + ":spigot";

    private final Plugin plugin;

    public BiomeSyncListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // Only clients that registered the channel receive plugin messages, so wait for that instead of the join
    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equals(CHANNEL)) {
            sendDataPacket(event.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getListeningPluginChannels().contains(CHANNEL)) {
            sendDataPacket(event.getPlayer());
        }
    }

    private void sendDataPacket(Player player) {
        if (!(((CraftWorld) player.getWorld()).getHandle().getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator generator)) {
            return;
        }

        byte[] data = encode(generator.getPreset());
        if (data.length > Messenger.MAX_MESSAGE_SIZE) {
            OTGLog.log(LogLevel.WARN, LogCategory.MAIN, "Biome sync data for " + player.getName() + " is too large to send (" + data.length + " bytes).");
            return;
        }
        OTGLog.log(LogLevel.INFO, LogCategory.MAIN, "Sending sync data of size " + data.length + " bytes to " + player.getName() + ".");
        player.sendPluginMessage(this.plugin, CHANNEL, data);
    }

    static byte[] encode(Preset preset) {
        String presetName = preset.getFolderName().toLowerCase(Locale.ROOT);
        FriendlyByteBuf biomes = new FriendlyByteBuf(Unpooled.buffer());
        int count = 0;
        for (BiomeConfig biome : preset.getBiomeConfigList()) {
            if (biome.getRegistryKey() == null) {
                continue;
            }
            String key = biome.getRegistryKey().toResourceLocationString();
            biomes.writeUtf(key.substring(key.indexOf(':') + 1));
            biomes.writeFloat(biome.getVisualSettings().getFogDensity());
            writeColorSet(biomes, biome.getVisualSettings().getGrassColorControl());
            writeColorSet(biomes, biome.getVisualSettings().getFoliageColorControl());
            writeColorSet(biomes, biome.getVisualSettings().getWaterColorControl());
            count++;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(1);
        buffer.writeInt(count);
        buffer.writeUtf(presetName);
        buffer.writeBytes(biomes);
        return Arrays.copyOf(buffer.array(), buffer.writerIndex());
    }

    private static void writeColorSet(FriendlyByteBuf buffer, ColorSet colorSet) {
        buffer.writeByte((byte) colorSet.getLayers().size());
        for (ColorThreshold color : colorSet.getLayers()) {
            buffer.writeInt(color.getColor().intValue());
            buffer.writeFloat(color.getMaxNoise());
        }
    }
}
