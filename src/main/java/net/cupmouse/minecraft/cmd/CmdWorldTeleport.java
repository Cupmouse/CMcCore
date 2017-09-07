package net.cupmouse.minecraft.cmd;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

public class CmdWorldTeleport implements CommandExecutor {
    public static final CommandCallable CALLABLE = CommandSpec.builder()
            .arguments(GenericArguments.world(Text.of("world")),
                    GenericArguments.optional(GenericArguments.player(Text.of("player"))))
            .executor(new CmdWorldTeleport())
            .build();

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<Player> playerOptional = args.<Player>getOne("player");

        Player player;

        if (playerOptional.isPresent()) {
            player = playerOptional.get();
        } else {
            if (!(src instanceof Player)) {
                throw new CommandException(Text.of(TextColors.RED, "✗プレイヤーを指定してください"), true);
            }

            player = ((Player) src);
        }

        WorldProperties worldProperties = args.<WorldProperties>getOne("world").get();
        Optional<World> worldOptional = Sponge.getServer().getWorld(worldProperties.getUniqueId());
        if (worldOptional.isPresent()) {
            player.transferToWorld(worldOptional.get());
        } else {
            throw new CommandException(Text.of(TextColors.RED, "✗ワールドが存在しません"));
        }

        return CommandResult.success();
    }
}
