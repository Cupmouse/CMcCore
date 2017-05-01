package net.cupmouse.minecraft;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.TickBlockEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * レッドストーン等を制限するためのモジュール。
 * レッドストーンの制限は、感圧板などでドアを開くなどの必要な動作以外はすべてキャンセルされる。
 * TODO 本当は、MCのサーバーコードを直接削除するほうが、遥かにパフォーマンスに貢献すると思われるので、そうしたい。
 *
 */
public class GriefingPreventerModule implements PluginModule {

    public static final Text TEXT_REDSTONE_RESTRICTED =
            Text.of(TextColors.YELLOW,"⚠レッドストーン関連の使用は制限されています。");
    private final CMcPlugin plugin;

    public GriefingPreventerModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInitializationProxy() {
        // リスナを登録する
        this.plugin.getGame().getEventManager().registerListeners(plugin, this);
    }

    @Listener
    public void onScheduledBlockTick(TickBlockEvent.Scheduled event) {
        // このリスナはレッドストーン関連の稼働を検知して、キャンセルする。
        plugin.getLogger().info(event.getTargetBlock().getState().toString());

        BlockSnapshot targetBlock = event.getTargetBlock();
        BlockType blockType = targetBlock.getState().getType();

        // 無効にする
        // リピーター/レッドストーントーチ/
        // トーチを無効にする
        // ピストンを無効にする
        if (blockType == BlockTypes.UNPOWERED_REPEATER
                || blockType == BlockTypes.UNPOWERED_COMPARATOR
                || blockType == BlockTypes.REDSTONE_TORCH) {

            // 近くのプレイヤーに通知する
            targetBlock.getLocation().ifPresent(worldLocation ->
                    notifyNearbyPlayer(worldLocation, TEXT_REDSTONE_RESTRICTED));
            event.setCancelled(true);
        }
    }

    @Listener
    public void onBlockPreChange(ChangeBlockEvent.Pre event) {
        // このリスナは、ピストンが稼働したことを検知してキャンセルする。
        Location<World> location = event.getLocations().get(0);
        BlockType type = location.getBlock().getType();

        if (type == BlockTypes.PISTON || type == BlockTypes.STICKY_PISTON) {
            notifyNearbyPlayer(location, TEXT_REDSTONE_RESTRICTED);
            event.setCancelled(true);
        }
    }

    private void notifyNearbyPlayer(Location<World> location, Text message) {
        for (Player player : location.getExtent().getPlayers()) {
            if (player.getLocation().getPosition().distanceSquared(location.getPosition()) <= 100) {
                player.sendMessage(ChatTypes.SYSTEM, message);
            }
        }
    }

    @Listener
    private void onChestOpen(InteractInventoryEvent.Open event) {
        if (event.getTargetInventory().getArchetype() == InventoryArchetypes.CHEST) {
            plugin.getLogger().debug("poing");
        }
    }
}
