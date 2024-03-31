package net.tyc.tycmod.events;





import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tyc.tycmod.TycMod;
import net.tyc.tycmod.entity.client.ModModelLayers;
import net.tyc.tycmod.entity.client.RhinoModel;
import net.tyc.tycmod.entity.client.ZeroModel;

@Mod.EventBusSubscriber(modid = TycMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.ZERO_LAYER, ZeroModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.RHINO_LAYER, RhinoModel::createBodyLayer);

    }
}