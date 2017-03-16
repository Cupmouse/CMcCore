package net.cupmouse.minecraft.data.user;

import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;

public enum UserAchievements {
    FIRST_LOGIN(Text.of("ようこそ！"),
            Text.of("初めてログインした"),
            ItemStack.builder().itemType(ItemTypes.GRASS).build()),
    ;

    private final Text title;
    private final Text description;
    private final ItemStack icon;

    UserAchievements(Text title, Text description, ItemStack icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }
}
