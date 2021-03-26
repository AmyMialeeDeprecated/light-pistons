package amymialee.lightpistons;

import amymialee.lightpistons.blocks.LightPistonBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RegisterPistons {
    public static Block lightPiston;
    public static BlockItem lightPistonItem;
    public static Block lightStickyPiston;
    public static BlockItem lightStickyPistonItem;
    @SubscribeEvent
    public static void onBlocksRegistration(final RegistryEvent.Register<Block> blockRegisterEvent) {
        lightPiston = new LightPistonBlock(false).setRegistryName("lightpistons", "light_piston");
        lightStickyPiston = new LightPistonBlock(true).setRegistryName("lightpistons", "light_sticky_piston");
        blockRegisterEvent.getRegistry().register(lightPiston);
        blockRegisterEvent.getRegistry().register(lightStickyPiston);
    }
    @SubscribeEvent
    public static void onItemsRegistration(final RegistryEvent.Register<Item> itemRegisterEvent) {
        Item.Properties itemSimpleProperties = new Item.Properties().group(ItemGroup.REDSTONE);
        lightPistonItem = new BlockItem(lightPiston, itemSimpleProperties);
        lightStickyPistonItem = new BlockItem(lightStickyPiston, itemSimpleProperties);
        lightPistonItem.setRegistryName(lightPiston.getRegistryName());
        lightStickyPistonItem.setRegistryName(lightStickyPiston.getRegistryName());
        itemRegisterEvent.getRegistry().register(lightPistonItem);
        itemRegisterEvent.getRegistry().register(lightStickyPistonItem);
    }
}
