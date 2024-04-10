package net.tyc.tycmod.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.phys.Vec3;
import net.tyc.tycmod.entity.ModEntities;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class Zero extends  AircraftEntity{
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(RhinoEntity.class, EntityDataSerializers.BOOLEAN);


    public Zero(EntityType<? extends AircraftEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        SetUpAirDynamics();
        SetUpMass_Engine();
        this.setNoGravity(true);
        this.setSharedFlag(7,false);

    }


    @Override
    protected void SetUpAirDynamics() {
        //theta degree
        this.left_wing=new AirDynamicElement(1000.0,new Vec3(0,1,0),
                "-x",0f*(float) Math.PI/180f,new Vec3(9,0,3),2662,0);
        this.right_wing=new AirDynamicElement(1000.0,new Vec3(0,1,0),
                "-x",0f*(float) Math.PI/180f,new Vec3(-9,0,3),1600,0);
        this.tail=new AirDynamicElement(500.0,new Vec3(0,1,0),
                "-x",0f,new Vec3(0,0,-12),2131,0);
        this.vertical_tail=new AirDynamicElement(500.0,new Vec3(1,0,0),
                "y",0f*(float) Math.PI/180f, new Vec3(0,0,-12),0,0);

    }

    @Override
    protected void SetUpMass_Engine() {
        this.mass=1300.0;
        this.maximum_power=950.0;
        this.inertia=new Matrix3d(1300.0,0.0,0.0,0.0,1300.0,0.0,0.0,0.0,1300.0);
    }

    @Override
    Item getDropItem() {
        return null;

    }




    @Override
    public void tick() {
        super.tick();
        //double theta=Math.PI/4;
        //this.RotationMatrix1to0=new Matrix3f(   1.0f,   0.0f,       0.0f,
               // 0.0f,   (float) Math.cos(theta),    (float) Math.sin(theta),
              //  0.0f,   -(float) Math.sin(theta),   (float) Math.cos(theta));


    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.1f)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5f)
                .add(Attributes.ATTACK_DAMAGE, 2f)
                .add(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get(),0.0);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACKING, false);
    }
}
