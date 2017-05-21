package net.cupmouse.minecraft.util;

import org.spongepowered.api.command.CommandException;

public interface CEThrowableFunction<A, R> {

    R apply(A a) throws CommandException;
}
