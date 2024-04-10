package net.tyc.tycmod.entity.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.tyc.tycmod.entity.custom.AircraftEntity;
import org.slf4j.Logger;

import java.util.List;

public abstract class AircraftRenderer  extends MobRenderer<AircraftEntity,ZeroModel<AircraftEntity>>
{
    private static final Logger LOGGER = LogUtils.getLogger();


    public AircraftRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new ZeroModel<>(pContext.bakeLayer(ModModelLayers.RHINO_LAYER)), 2f);

    }

    public void render(AircraftEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);



    }

    @Override
    protected void setupRotations(AircraftEntity pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        if (!pEntityLiving.hasPose(Pose.SLEEPING)) {
            float Yaw=pPartialTicks == 1.0F ? pEntityLiving.getYRot(): Mth.lerp(pPartialTicks, pEntityLiving.yRotO, pEntityLiving.getYRot());
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Yaw));
            float Pitch=pPartialTicks == 1.0F ? pEntityLiving.getXRot(): Mth.lerp(pPartialTicks, pEntityLiving.xRotO, pEntityLiving.getXRot());
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-Pitch));
            float Roll=pPartialTicks == 1.0F ? pEntityLiving.getZRot(): Mth.lerp(pPartialTicks, pEntityLiving.zRotO, pEntityLiving.getZRot());
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(Roll));
        }
    }
}
