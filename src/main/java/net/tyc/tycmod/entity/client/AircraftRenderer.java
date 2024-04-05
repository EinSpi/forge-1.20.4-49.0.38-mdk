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

public abstract class AircraftRenderer <T extends AircraftEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M>
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float EYE_BED_OFFSET = 0.1F;
    protected M model;
    protected final List<RenderLayer<T, M>> layers = Lists.newArrayList();

    public AircraftRenderer(EntityRendererProvider.Context pContext, M pModel, float pShadowRadius) {
        super(pContext);
        this.model = pModel;
        this.shadowRadius = pShadowRadius;
    }

    public M getModel() {
        return this.model;
    }

    public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {

        pPoseStack.pushPose();
        this.model.attackTime = 0.5f;


        boolean shouldSit = pEntity.isPassenger() && (pEntity.getVehicle() != null && pEntity.getVehicle().shouldRiderSit());
        this.model.riding = shouldSit;
        this.model.young = false;


        float f5 = Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot());


        float f7 = this.getBob(pEntity, pPartialTicks);
        this.setupRotations(pEntity, pPoseStack, f7, pEntityYaw, pPartialTicks);
        pPoseStack.scale(-1.0F, -1.0F, 1.0F);
        this.scale(pEntity, pPoseStack, pPartialTicks);
        pPoseStack.translate(0.0F, -1.501F, 0.0F);
        float f8 = 0.0F;
        float f4 = 0.0F;

        this.model.prepareMobModel(pEntity, f4, f8, pPartialTicks);
        this.model.setupAnim(pEntity, f4, f8, f7, 0.0f, f5);
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = this.isBodyVisible(pEntity);
        boolean flag1 = !flag && !pEntity.isInvisibleTo(minecraft.player);
        boolean flag2 = minecraft.shouldEntityAppearGlowing(pEntity);
        RenderType rendertype = this.getRenderType(pEntity, flag, flag1, flag2);
        if (rendertype != null) {
            VertexConsumer vertexconsumer = pBuffer.getBuffer(rendertype);
            int i = 1;
            this.model.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
        }

        if (!pEntity.isSpectator()) {
            for(RenderLayer<T, M> renderlayer : this.layers) {
                renderlayer.render(pPoseStack, pBuffer, pPackedLight, pEntity, f4, f8, pPartialTicks, f7, 0.0f, f5);
            }
        }

        pPoseStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    protected float getBob(T pLivingBase, float pPartialTick) {
        return (float)pLivingBase.tickCount + pPartialTick;
    }

    protected void setupRotations(T pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        if (this.isShaking(pEntityLiving)) {
            pRotationYaw += (float)(Math.cos((double)pEntityLiving.tickCount * 3.25D) * Math.PI * (double)0.4F);
        }

        if (!pEntityLiving.hasPose(Pose.SLEEPING)) {
            LOGGER.info(String.valueOf(pRotationYaw));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - pRotationYaw));
        }



    }

    protected void scale(T pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
    }

    protected boolean isShaking(T pEntity) {
        return pEntity.isFullyFrozen();
    }

    protected boolean isBodyVisible(T pLivingEntity) {
        return !pLivingEntity.isInvisible();
    }

    protected RenderType getRenderType(T pLivingEntity, boolean pBodyVisible, boolean pTranslucent, boolean pGlowing) {
        ResourceLocation resourcelocation = this.getTextureLocation(pLivingEntity);
        if (pTranslucent) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (pBodyVisible) {
            return this.model.renderType(resourcelocation);
        } else {
            return pGlowing ? RenderType.outline(resourcelocation) : null;
        }
    }



    protected float getWhiteOverlayProgress(T pLivingEntity, float pPartialTicks) {
        return 0.0F;
    }




}
