package com.github.hapily04.skriptminestom.command;

import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

public class StopCommand extends Command {

    public StopCommand() {
        super("stop");
        setCondition((sender, _) -> LuckPermsLookup.hasPermission(sender, "skript.stop"));
        setDefaultExecutor((_, _) -> MinecraftServer.stopCleanly());
    }

}
