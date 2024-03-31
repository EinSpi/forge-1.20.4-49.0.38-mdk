package net.tyc.tycmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tyc.tycmod.TycMod;
import net.tyc.tycmod.entity.custom.RhinoEntity;
import net.tyc.tycmod.entity.custom.Zero;

public class ModEntities {
    public static  final DeferredRegister<EntityType<?>> ENTITY_TYPES=
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TycMod.MOD_ID);
    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }
    public static final RegistryObject<EntityType<Zero>> ZERO=
            ENTITY_TYPES.register("zero",()->EntityType.Builder.of(Zero::new , MobCategory.CREATURE).
                    sized(2.5f,2.5f).build("zero"));
    public static final RegistryObject<EntityType<RhinoEntity>> RHINO =
            ENTITY_TYPES.register("rhino", () -> EntityType.Builder.of(RhinoEntity::new, MobCategory.CREATURE)
                    .sized(2.5f, 2.5f).build("rhino"));

}
