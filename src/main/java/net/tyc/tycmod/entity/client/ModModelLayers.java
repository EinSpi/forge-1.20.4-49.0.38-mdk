package net.tyc.tycmod.entity.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.tyc.tycmod.TycMod;

public class ModModelLayers {
    public static final ModelLayerLocation ZERO_LAYER= new ModelLayerLocation(
            new ResourceLocation(TycMod.MOD_ID,"zero_layer"),"main"
    );
    public static final ModelLayerLocation RHINO_LAYER = new ModelLayerLocation(
            new ResourceLocation(TycMod.MOD_ID, "rhino_layer"), "main");


}
