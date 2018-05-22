package com.bobmowzie.mowziesmobs.server.item;

import java.util.EnumMap;
import java.util.List;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.server.entity.barakoa.MaskType;
import net.minecraft.item.Item;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(MowziesMobs.MODID)
@Mod.EventBusSubscriber(modid = MowziesMobs.MODID)
public final class ItemHandler {
    private ItemHandler() {}

    public static final ItemFoliaathSeed FOLIAATH_SEED = null;
    public static final ItemTestStructure TEST_STRUCTURE = null;
    public static final ItemMobRemover MOB_REMOVER = null;
    public static final ItemWroughtAxe WROUGHT_AXE = null;
    public static final ItemWroughtHelm WROUGHT_HELMET = null;
    public static final ItemBarakoaMask BARAKOA_MASK_FURY = null;
    public static final ItemBarakoaMask BARAKOA_MASK_FEAR = null;
    public static final ItemBarakoaMask BARAKOA_MASK_RAGE = null;
    public static final ItemBarakoaMask BARAKOA_MASK_BLISS = null;
    public static final ItemBarakoaMask BARAKOA_MASK_MISERY = null;
    public static EnumMap<MaskType, ItemBarakoaMask> BARAKOA_MASKS;
    public static final ItemBarakoMask BARAKO_MASK = null;
    public static final ItemDart DART = null;
    public static final ItemSpear SPEAR = null;
    public static final ItemBlowgun BLOWGUN = null;
    public static final ItemSpawnEgg SPAWN_EGG = null;
    public static final ItemGrantSunsBlessing GRANT_SUNS_BLESSING = null;
    public static final ItemIceCrystal ICE_CRYSTAL = null;
    public static final ItemEarthTalisman EARTH_TALISMAN = null;

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemFoliaathSeed(),
                new ItemTestStructure(),
                new ItemMobRemover(),
                new ItemWroughtAxe(),
                new ItemWroughtHelm(),
                new ItemBarakoMask(),
                new ItemDart(),
                new ItemSpear(),
                new ItemBlowgun(),
                new ItemSpawnEgg(),
                new ItemGrantSunsBlessing(),
                new ItemIceCrystal(),
                new ItemEarthTalisman()
        );
        for (MaskType mask : MaskType.values()) {
            event.getRegistry().register(new ItemBarakoaMask(mask));
        }
        BARAKOA_MASKS = MaskType.newEnumMap(ItemBarakoaMask.class);
        BARAKOA_MASKS.put(MaskType.FURY, BARAKOA_MASK_FURY);
        BARAKOA_MASKS.put(MaskType.BLISS, BARAKOA_MASK_BLISS);
        BARAKOA_MASKS.put(MaskType.FEAR, BARAKOA_MASK_FEAR);
        BARAKOA_MASKS.put(MaskType.MISERY, BARAKOA_MASK_MISERY);
        BARAKOA_MASKS.put(MaskType.RAGE, BARAKOA_MASK_RAGE);
    }

    public static void addItemText(Item item, List<String> lines) {
        String keyStart = item.getUnlocalizedName() + ".text.";
        for (int line = 0;; line++) {
            String key = keyStart + line;
            if (I18n.canTranslate(key)) {
                lines.add(I18n.translateToLocal(key));
            } else {
                break;
            }
        }
    }
}