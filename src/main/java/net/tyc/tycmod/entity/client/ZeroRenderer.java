package net.tyc.tycmod.entity.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.tyc.tycmod.TycMod;
import net.tyc.tycmod.entity.custom.AircraftEntity;
import net.tyc.tycmod.entity.custom.Zero;
import org.slf4j.Logger;

import java.util.Map;
import java.util.stream.Stream;

public class ZeroRenderer extends AircraftRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ZeroRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);

    }

    @Override
    public ResourceLocation getTextureLocation(AircraftEntity pEntity) {
        return new ResourceLocation(TycMod.MOD_ID,"textures/entity/rhino.png");
    }







}
