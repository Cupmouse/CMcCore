package net.cupmouse.minecraft.util;

import org.spongepowered.api.command.CommandException;

public interface CEThrowableConsumer<A> {

    void accept(A a) throws CommandException;
}
