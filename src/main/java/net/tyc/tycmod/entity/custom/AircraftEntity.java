package net.tyc.tycmod.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public abstract class AircraftEntity extends Entity {

    protected static final EntityDataAccessor<Integer> f_302249_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> f_302571_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> f_302371_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    protected Matrix3f RotationMatrix1to0=
            new Matrix3f();// rotate matrix aircraft_frame to world frame, aircraft axis represented in world axis
    private double atan2(double a, double b)
    {
        if (Math.abs(b)<=1e-6)
        {
            if(a>0)
            {
                return Math.PI/2;
            }
            else if(a==0)
            {
                return 0;//undifiniert
            }
            else
            {
                return  -Math.PI/2;
            }
        }
        else if (b>0)
        {
            return Math.atan(a/b);
        }
        else
        {
            return Math.atan(a/b)+Math.PI;
        }
    }
    protected Vector3f EulerAnglesFromRotationMatrix()
    {
        // from RotMat to Euler Angle.
        Matrix3f RotationMatrix0to1= new Matrix3f(RotationMatrix1to0.m00,RotationMatrix1to0.m01,RotationMatrix1to0.m02,
                RotationMatrix1to0.m10,RotationMatrix1to0.m11,RotationMatrix1to0.m12,
                RotationMatrix1to0.m20,RotationMatrix1to0.m21,RotationMatrix1to0.m22);
        RotationMatrix0to1.invert();
        //y axis: gamma
        //x axis: beta
        //z axis: alpha
        float gamma;
        float beta;
        float alpha;
        double sy=Math.sqrt(Math.fma(RotationMatrix0to1.m11,RotationMatrix0to1.m11,RotationMatrix0to1.m01*RotationMatrix0to1.m01));
        boolean singular=sy<1e-6;
        if(singular)
        {
            //radian to degree
            if(RotationMatrix0to1.m21>0)
            {
                beta=90f;
                gamma=(float) (atan2(RotationMatrix0to1.m10,RotationMatrix0to1.m00));
            }
            else
            {
                beta=-90f;
                gamma=(float) (-atan2(RotationMatrix0to1.m10,RotationMatrix0to1.m00));

            }
            gamma=gamma*(180.0f/(float)Math.PI);//radian to degree
            alpha=0.0f;
        }
        else
        {
            beta=-(float) atan2(-RotationMatrix0to1.m21,sy);//radian to degree
            gamma=-(float) atan2(RotationMatrix0to1.m20/Math.cos(beta),RotationMatrix0to1.m22/Math.cos(beta));
            alpha=-(float) atan2(RotationMatrix0to1.m01/Math.cos(beta),RotationMatrix0to1.m11/Math.cos(beta));
            beta=beta*(180.0f/(float) Math.PI);
            gamma=gamma*(180.0f/(float) Math.PI);
            alpha=alpha*(180.0f/(float) Math.PI);
        }

        return new Vector3f(gamma,beta,alpha);

    }

    private void UseEulerAngleToSetRotMatrix(float roll, float yaw, float pitch)
    {

    }

    protected void RotateSmallValueAround(float small_value, Vector3f axis)
    {
        //small_value here radian
        axis.normalize();
        RotationMatrix1to0.rotate(small_value,axis);
    }



    public AircraftEntity(EntityType<?> p_310168_, Level p_309578_) {
        super(p_310168_, p_309578_);
    }

    /**
     * Called when the entity is attacked.
     */

    public boolean hurt(DamageSource p_310829_, float p_310313_) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (this.isInvulnerableTo(p_310829_)) {
                return false;
            } else {
                this.m_306256_(-this.m_305195_());
                this.m_307446_(10);
                this.markHurt();
                this.m_305563_(this.m_304923_() + p_310313_ * 10.0F);
                this.gameEvent(GameEvent.ENTITY_DAMAGE, p_310829_.getEntity());
                boolean flag = p_310829_.getEntity() instanceof Player && ((Player)p_310829_.getEntity()).getAbilities().instabuild;
                if ((flag || !(this.m_304923_() > 40.0F)) && !this.m_304763_(p_310829_)) {
                    if (flag) {
                        this.discard();
                    }
                } else {
                    this.m_38227_(p_310829_);
                }

                return true;
            }
        } else {
            return true;
        }
    }

    boolean m_304763_(DamageSource p_309621_) {
        return false;
    }

    public void m_305179_(Item p_313028_) {
        this.kill();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemstack = new ItemStack(p_313028_);
            if (this.hasCustomName()) {
                itemstack.setHoverName(this.getCustomName());
            }

            this.spawnAtLocation(itemstack);
        }
    }

    protected void defineSynchedData() {
        this.entityData.define(f_302249_, 0);
        this.entityData.define(f_302571_, 1);
        this.entityData.define(f_302371_, 0.0F);
    }

    public void m_307446_(int p_312621_) {
        this.entityData.set(f_302249_, p_312621_);
    }

    public void m_306256_(int p_312074_) {
        this.entityData.set(f_302571_, p_312074_);
    }

    public void m_305563_(float p_313007_) {
        this.entityData.set(f_302371_, p_313007_);
    }

    public float m_304923_() {
        return this.entityData.get(f_302371_);
    }

    public int m_305464_() {
        return this.entityData.get(f_302249_);
    }

    public int m_305195_() {
        return this.entityData.get(f_302571_);
    }

    protected void m_38227_(DamageSource p_312900_) {
        this.m_305179_(this.getDropItem());
    }

    abstract Item getDropItem();
}




