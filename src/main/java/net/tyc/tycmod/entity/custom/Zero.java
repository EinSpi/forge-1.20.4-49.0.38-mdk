package net.tyc.tycmod.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
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
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);



    public Zero(EntityType<? extends Zero> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        SetUpAirDynamics();
        SetUpMass_Engine();
        this.blocksBuilding = true;


    }


    @Override
    protected void SetUpAirDynamics() {
        this.left_wing=new AirDynamicElement(10.0,new Vec3(0,1,0),
                "-x",0.0f,new Vec3(9,0,3),800000,0.05);
        this.right_wing=new AirDynamicElement(10.0,new Vec3(0,1,0),
                "-x",0.0f,new Vec3(-9,0,3),800000,0.05);
        this.tail=new AirDynamicElement(5.0,new Vec3(0,1,0),
                "-x",0.0f,new Vec3(0,0,-12),80,0.05);
        this.vertical_tail=new AirDynamicElement(5.0,new Vec3(1,0,0),
                "y",0.0f, new Vec3(0,0,-12),0.0,0.05);

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
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.contains("Type", 8)) {
            this.setVariant(Boat.Type.byName(pCompound.getString("Type")));
        }

    }
    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putString("Type", this.getVariant().getSerializedName());
    }
    public void setVariant(Boat.Type pVariant) {
        this.entityData.set(DATA_ID_TYPE, pVariant.ordinal());
    }

    public Boat.Type getVariant() {
        return Boat.Type.byId(this.entityData.get(DATA_ID_TYPE));
    }

    @Override
    public void tick() {
        super.tick();
        //double theta=Math.PI/4;
        //this.RotationMatrix1to0=new Matrix3f(   1.0f,   0.0f,       0.0f,
               // 0.0f,   (float) Math.cos(theta),    (float) Math.sin(theta),
              //  0.0f,   -(float) Math.sin(theta),   (float) Math.cos(theta));


    }
}
