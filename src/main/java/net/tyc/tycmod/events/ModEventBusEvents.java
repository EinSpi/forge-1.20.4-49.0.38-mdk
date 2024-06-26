package net.tyc.tycmod.events;



import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tyc.tycmod.TycMod;
import net.tyc.tycmod.entity.ModEntities;
import net.tyc.tycmod.entity.custom.RhinoEntity;
import net.tyc.tycmod.entity.custom.Zero;

@Mod.EventBusSubscriber(modid = TycMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ModEntities.RHINO.get(),RhinoEntity.createAttributes().build());
        event.put(ModEntities.ZERO.get(), Zero.createAttributes().build());
    }

}

