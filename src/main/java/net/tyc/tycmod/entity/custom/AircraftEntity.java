package net.tyc.tycmod.entity.custom;

import com.mojang.logging.LogUtils;
import com.mojang.math.MatrixUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
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
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.units.qual.Mass;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.slf4j.Logger;


import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class AircraftEntity extends Entity {

    protected static final EntityDataAccessor<Integer> f_302249_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> f_302571_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> f_302371_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final Logger LOGGER = LogUtils.getLogger();
    protected Matrix3d RotationMatrix1to0=
            new Matrix3d();// rotate matrix aircraft_frame to world frame, aircraft axis represented in world axis
    protected float ZRot=0;
    public float zRotO=0;

    protected double maximum_power;
    protected double mass;
    protected Matrix3d inertia;
    protected class AirDynamicElement
    {
        public double area;
        public Vec3 norm;
        public String rotate_axis;
        public float theta;
        public Vec3 relative_pos;
        public double balance_C_l;
        public double balance_C_d;

        public AirDynamicElement(double area, Vec3 norm, String rotate_axis,
                                 float theta, Vec3 relative_pos,double balance_C_l, double balance_C_d)
        {
            this.area=area;
            this.norm=norm;
            this.rotate_axis=rotate_axis;
            this.theta=theta;
            this.relative_pos=relative_pos;
            this.balance_C_l=balance_C_l;
            this.balance_C_d=balance_C_d;

        }

        public List<Vec3> ComputeWingAirDynamicForcesInObjFrame(Vec3 speed_vector_in_object_frame, double air_density)
        {
            //compute s eq
            //Vector3f area_norm=new Vector3f(0.0f,(float) Math.cos(theta*Math.PI/180.0f),-(float) Math.sin(theta*Math.PI/180.0f));
            Vec3 area_norm=Vec3.ZERO;
            if (rotate_axis=="-x")
            {
                area_norm=this.norm.xRot(theta);
            }
            else if(rotate_axis=="y")
            {
                area_norm=this.norm.yRot(theta);
            }


            if(speed_vector_in_object_frame.length()<=1.0E-4D)
            {
                List<Vec3> vec=new ArrayList<>();
                vec.add(Vec3.ZERO);
                vec.add(Vec3.ZERO);
                return vec;

            }
            else {
                double cos = area_norm.dot(speed_vector_in_object_frame)/(area_norm.length()*speed_vector_in_object_frame.length());

                double S_eq=rotate_axis=="-x"?Math.abs(cos) * area+0.2*area:Math.abs(cos) * area;


                //compute norm_lift
                Vec3 norm_lift;
                //norm_lift=speed_vector_in_object_frame.cross(area_norm).cross(speed_vector_in_object_frame).normalize();
                if(rotate_axis=="-x"){
                norm_lift=new Vec3(0,1,0);}
                else
                {
                    norm_lift=new Vec3(1,0,0);

                }

                //compute norm_friction
                Vec3 norm_friction;
                norm_friction=speed_vector_in_object_frame.normalize();
                //LOGGER.info(norm_friction.toString());

                //Compute Actual lift and friction coefficients
                double C_l;
                double C_d;
                if (cos >= 0 && cos <= Math.cos(70 * Math.PI / 180.0)) {
                    //LOGGER.info("case1");
                    if (balance_C_l == 0) {
                        C_l = cos * (2000 / Math.cos(70 * Math.PI / 180.0));
                    } else {
                        C_l = balance_C_l + cos * (3 * balance_C_l / Math.cos(70 * Math.PI / 180.0));
                        //LOGGER.info(String.valueOf(C_l));
                    }
                } else if (cos < 0 && cos >=  Math.cos(110 * Math.PI / 180.0)) {
                    //LOGGER.info("case2");
                    if (balance_C_l == 0) {
                        C_l = cos * (-2000 / Math.cos(110 * Math.PI / 180.0));
                    } else {
                        C_l = balance_C_l + cos * (-3 * balance_C_l /  Math.cos(110 * Math.PI / 180.0));
                    }
                } else {
                    //LOGGER.info("case3");
                    C_l = 0.0;
                }


                C_d = balance_C_d + cos * cos * 3 * balance_C_d;
                //Compute lift force and friction force
                double lift_coefficient=air_density*(speed_vector_in_object_frame.lengthSqr())*C_l*S_eq;
                double friction_coefficient=speed_vector_in_object_frame.lengthSqr()==0?0.0:Math.max(air_density * (speed_vector_in_object_frame.lengthSqr()) * C_d * S_eq,air_density * (0.001) * C_d * S_eq);
                Vec3 lift_force ;
                lift_force=norm_lift.scale(lift_coefficient);
                //LOGGER.info(String.valueOf(lift_coefficient));
                //LOGGER.info(norm_lift.toString());

                //if(rotate_axis=="y"){LOGGER.info(lift_force.toString());}

                Vec3 friction_force;
                friction_force=norm_friction.scale(friction_coefficient);
                //LOGGER.info(String.valueOf(friction_coefficient));
                //LOGGER.info(norm_friction.toString());
                //LOGGER.info(friction_force.toString());

                Vec3 sum_force;
                sum_force=lift_force.add(friction_force);
                Vec3 sum_moment;

                sum_moment=relative_pos.cross(sum_force);


                List<Vec3> sum_force_and_moment=new ArrayList<>();
                sum_force_and_moment.add(sum_force);
                sum_force_and_moment.add(sum_moment);

                return sum_force_and_moment;


            }
        }

    }
    protected AirDynamicElement left_wing;
    protected AirDynamicElement right_wing;
    protected AirDynamicElement tail;
    protected AirDynamicElement vertical_tail;

    protected Vec3 speed_vector_in_global_frame=Vec3.ZERO;
    protected Vec3 speed_vector_in_object_frame=Vec3.ZERO;
    protected Vec3 air_speed_in_object_frame=Vec3.ZERO;
    protected Vec3 angular_velocity=Vec3.ZERO;
    protected double air_density=0.13;
    protected double throttle_percentage=0.0;
    public boolean setdeltamovement=true;




    protected abstract void SetUpAirDynamics();
    protected abstract void SetUpMass_Engine();

    public void setZRot(float z)
    {
        this.ZRot=z;
    }

    public float getZRot()
    {
        return this.ZRot;
    }
    protected Vec3 premultiply(Matrix3d matrix, Vec3 vec)
    {
        return new Vec3(Math.fma(matrix.m00,vec.x,Math.fma(matrix.m01,vec.y,matrix.m02*vec.z)),
                        Math.fma(matrix.m10,vec.x,Math.fma(matrix.m11,vec.y,matrix.m12*vec.z)),
                        Math.fma(matrix.m20,vec.x,Math.fma(matrix.m21,vec.y,matrix.m22*vec.z)));
    }

    protected Vec3 EulerAnglesFromRotationMatrix()
    {
        // from RotMat to Euler Angle.
        //y axis: gamma
        //x axis: beta
        //z axis: alpha
        double gamma;
        double beta;
        double alpha;
        double sy=Math.sqrt(Math.fma(RotationMatrix1to0.m11,RotationMatrix1to0.m11,RotationMatrix1to0.m01*RotationMatrix1to0.m01));


        beta= Math.atan2(RotationMatrix1to0.m21,sy);//beta from -pi/2 to pi/2

        if (beta==Math.PI/2 || beta==-Math.PI/2)//singularity
            {
                alpha=0.0;
                gamma= Math.atan2(RotationMatrix1to0.m02,RotationMatrix1to0.m00);
            }
        else
            {
                gamma =  Math.atan2(-RotationMatrix1to0.m20 / Math.cos(beta), RotationMatrix1to0.m22 / Math.cos(beta));
                alpha = Math.atan2(-RotationMatrix1to0.m01 / Math.cos(beta), RotationMatrix1to0.m11 / Math.cos(beta));
            }

            //beta from -pi/2 to pi/2
            //gamma from -pi to pi
            //alpha from -pi to pi

            beta=beta*(180.0/ Math.PI);
            gamma=gamma*(180.0/ Math.PI);
            alpha=alpha*(180.0/ Math.PI);
            //beta from -90 to 90
            //gamma from -180 to 180
            //alpha from -180 to 180


        return new Vec3(gamma,beta,alpha);

    }

    private void UseEulerAngleToSetRotMatrix(float yaw, float pitch, float roll)
    {
        float yaw_rad=(float) Math.PI*yaw/180.f;
        float pitch_rad=(float) Math.PI*pitch/180.f;
        float roll_rad=(float) Math.PI*roll/180.f;

        Matrix3f RY=new Matrix3f((float) Math.cos(yaw_rad),0.0f,(float) Math.sin(yaw_rad),
                                            0.0f,1.0f,0.0f,
                                -(float) Math.sin(yaw_rad),0.0f,(float) Math.cos(yaw_rad));

        Matrix3f RX=new Matrix3f(1.0f,0.0f,0.0f,
                0.0f,(float) Math.cos(pitch_rad), -(float) Math.sin(pitch_rad),
                0.0f,(float) Math.sin(pitch_rad),   (float) Math.cos(pitch_rad));

        Matrix3f RZ=new Matrix3f((float) Math.cos(roll_rad),-(float) Math.sin(roll_rad),0.0f,
                                (float) Math.sin(roll_rad),(float) Math.cos(roll_rad),0.0f,
                                    0.0f,0.0f,1.0f);


        Matrix3d RYXZ=new Matrix3d();
        RYXZ.mul(RY);
        RYXZ.mul(RX);
        RYXZ.mul(RZ);
        this.RotationMatrix1to0=RYXZ;

    }

    protected void RotateSmallValueAround(float small_value, Vector3f axis)
    {
        //small_value here radian
        axis.normalize();
        RotationMatrix1to0.rotate(small_value,axis);
    }

    protected List<Vec3> ComputeLinearAndAngularAccInObjectFrame(List<Vec3> force_and_moment ,Matrix3d MassCenterInertia,double Mass)
    {
        //Assume MassCenterInertia some times of identity matrix
        //Rigid Body Dynamics Formula N=Iw+wx(Iw)
        //I=lambda Identity, meaning the cross product term is 0.
        //then N=Iw,w=I^-1N
        Vec3 linear_acc;
        linear_acc=force_and_moment.get(0).scale(1/Mass);



        Matrix3d Inverted_Inertia=new Matrix3d();
        MassCenterInertia.invert(Inverted_Inertia);
        Vec3 angular_acc;
        angular_acc=premultiply(Inverted_Inertia,force_and_moment.get(1));

        List<Vec3> linear_and_angular_acc=new ArrayList<>();
        linear_and_angular_acc.add(linear_acc);
        linear_and_angular_acc.add(angular_acc);

        return  linear_and_angular_acc;
    }



    protected List<Vec3> RotateToGlobal(List<Vec3> vec_list)
    {
        Vec3 linear_acc_global=premultiply(RotationMatrix1to0,vec_list.get(0));
        Vec3 angular_acc_global=premultiply(RotationMatrix1to0,vec_list.get(1));
        List<Vec3> global_vec_list=new ArrayList<>();
        global_vec_list.add(linear_acc_global);
        global_vec_list.add(angular_acc_global);

        return global_vec_list;
    }

    protected List<Vec3> ComputeSumOfForcesAndMoments()
    {
        //compute sum force
        Vec3 gravity=ComputeGravityInObjFrame();
        Vec3 engine_force=ComputeEngineForceInObjFrame();
        Vec3 left_wing_force=left_wing.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(0);
        Vec3 right_wing_force=right_wing.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(0);
        Vec3 tail_force=tail.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(0);
        Vec3 vertical_tail_force=vertical_tail.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(0);


        Vec3 sum_force;
        sum_force=gravity.add(engine_force).add(left_wing_force).add(right_wing_force).add(tail_force).add(vertical_tail_force);
        //LOGGER.info(left_wing_force.toString());
        //LOGGER.info(right_wing_force.toString());
        //LOGGER.info(left_wing_force.toString());

        //compute sum moment, assume no moment for gravity and engine
        Vec3 left_wing_moment=left_wing.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(1);
        Vec3 right_wing_moment=right_wing.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(1);
        Vec3 tail_moment=tail.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(1);
        Vec3 vertical_tail_moment=vertical_tail.ComputeWingAirDynamicForcesInObjFrame(air_speed_in_object_frame,air_density).get(1);
        Vec3 sum_moment;
        sum_moment=left_wing_moment.add(right_wing_moment).add(tail_moment).add(vertical_tail_moment);
        //LOGGER.info(sum_moment.toString());
        List<Vec3> force_and_moment= new ArrayList<>();
        force_and_moment.add(sum_force);
        force_and_moment.add(sum_moment);
        return force_and_moment;


    }



    protected Vec3 ComputeEngineForceInObjFrame()
    {
        return new Vec3(0,0,(maximum_power*throttle_percentage)/(speed_vector_in_object_frame.z+0.001));
    }

    protected Vec3 ComputeGravityInObjFrame()
    {
        Matrix3d ZeroToOne=new Matrix3d();
        this.RotationMatrix1to0.invert(ZeroToOne);
        Vec3 gravity_global=new Vec3(0.0,-9.8*this.mass,0.0);
        Vec3 gravity_object=premultiply(ZeroToOne,gravity_global);
        return  gravity_object;
    }



    public AircraftEntity(EntityType<?> p_310168_, Level p_309578_) {
        super(p_310168_, p_309578_);
        this.setdeltamovement=true;
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

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if(pPlayer.getItemInHand(pHand).is(Items.AIR)) {
            pPlayer.startRiding(this);
            LOGGER.info("started riding");
            return InteractionResult.SUCCESS;
        }
        else{return InteractionResult.PASS;}



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

    @Override
    public void tick() {
        if (setdeltamovement)
        {
            this.setDeltaMovement(0,0,0);
            setdeltamovement=false;
        }
        clampSpeedVector();

        Matrix3d RotationMatrix0to1=new Matrix3d();
        RotationMatrix1to0.transpose(RotationMatrix0to1);
        this.speed_vector_in_object_frame=premultiply(RotationMatrix0to1,getDeltaMovement());
        this.air_speed_in_object_frame=this.speed_vector_in_object_frame.reverse();
        List<Vec3> linear_and_angular_acc_in_glb=RotateToGlobal(ComputeLinearAndAngularAccInObjectFrame(ComputeSumOfForcesAndMoments(),inertia,mass));
        //LOGGER.info(ComputeSumOfForcesAndMoments().get(0).toString());
        Vec3 linear_acc= linear_and_angular_acc_in_glb.get(0);
        this.setDeltaMovement(getDeltaMovement().add(linear_acc.scale(0.0015)));
        //LOGGER.info(getDeltaMovement().toString());
        this.move(MoverType.SELF,getDeltaMovement());
        //setup rotations
        Vec3 angular_acc=linear_and_angular_acc_in_glb.get(1);
        //LOGGER.info(getDeltaMovement().toString());
        this.angular_velocity=angular_velocity.add(angular_acc.scale(0.001));

        //LOGGER.info(angular_velocity.toString());
        this.RotationMatrix1to0.rotate(-angular_velocity.length()*0.001,angular_velocity.normalize().x,angular_velocity.normalize().y,angular_velocity.normalize().z);
        Vec3 vec=this.EulerAnglesFromRotationMatrix();
        this.setYRot(-(float) vec.x);//y axis left-hand grabbing law
        this.setXRot((float) vec.y);//x axis right-hand grabbing law
        this.setZRot(-(float) vec.z);
        super.tick();
        //LOGGER.info(getDeltaMovement().toString());
        //LOGGER.info(angular_acc.toString());
        this.move(MoverType.SELF,getDeltaMovement());
        this.zRotO = this.getZRot();

        //this.RotateSmallValueAround(0.05f,new Vector3f(1,0,1));
        //this.UseEulerAngleToSetRotMatrix(45.0f,90.0f,-30.0f);
        //LOGGER.info("00:"+String.valueOf(RotationMatrix1to0.m00)+"01:"+String.valueOf(RotationMatrix1to0.m01)+"02:"+String.valueOf(RotationMatrix1to0.m02)
                //+"10:"+String.valueOf(RotationMatrix1to0.m10)+"11:"+String.valueOf(RotationMatrix1to0.m11)+"12:"+String.valueOf(RotationMatrix1to0.m12)
                //+"20:"+String.valueOf(RotationMatrix1to0.m20)+"21:"+String.valueOf(RotationMatrix1to0.m21)+"22:"+String.valueOf(RotationMatrix1to0.m22));
        //LOGGER.info("20:"+String.valueOf(RotationMatrix1to0.m20)+"21:"+String.valueOf(RotationMatrix1to0.m21)+"22:"+String.valueOf(RotationMatrix1to0.m22));







    }
    protected void clampSpeedVector()
    {
        Vec3 v=getDeltaMovement();
        setDeltaMovement(Math.abs(v.x)<=0.008?0:v.x,Math.abs(v.y)<=0.008?0:v.y,Math.abs(v.z)<0.008?0:v.z);
    }
}




