package net.cupmouse.minecraft.util;

import org.spongepowered.api.command.CommandException;

public interface CEThrowableDualConsumer<A, B> {

    void accept(A a, B b) throws CommandException;

}
