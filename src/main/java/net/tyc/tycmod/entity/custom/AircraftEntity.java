package net.tyc.tycmod.entity.custom;

import com.mojang.logging.LogUtils;
import com.mojang.math.MatrixUtil;
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
import org.checkerframework.checker.units.qual.Mass;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.slf4j.Logger;


import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public abstract class AircraftEntity extends Entity {

    protected static final EntityDataAccessor<Integer> f_302249_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> f_302571_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> f_302371_ = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final Logger LOGGER = LogUtils.getLogger();
    protected Matrix3f RotationMatrix1to0=
            new Matrix3f();// rotate matrix aircraft_frame to world frame, aircraft axis represented in world axis
    protected float ZRot=0;
    public float zRotO=0;

    protected float maximum_power;
    protected float mass;
    protected Matrix3f inertia;
    protected class AirDynamicElement
    {
        public float area;
        public Vector3f norm;
        public Vector3f rotate_axis;
        public float theta;
        public Vector3f relative_pos;
        public float balance_C_l;
        public float balance_C_d;

        public AirDynamicElement(float area, Vector3f norm, Vector3f rotate_axis,
                                 float theta, Vector3f relative_pos,float balance_C_l, float balance_C_d)
        {
            this.area=area;
            this.norm=norm;
            this.rotate_axis=rotate_axis;
            this.theta=theta;
            this.relative_pos=relative_pos;
            this.balance_C_l=balance_C_l;
            this.balance_C_d=balance_C_d;

        }

        public List<Vector3f> ComputeWingAirDynamicForcesInObjFrame(Vector3f speed_vector_in_object_frame, float air_density)
        {
            //compute s eq
            //Vector3f area_norm=new Vector3f(0.0f,(float) Math.cos(theta*Math.PI/180.0f),-(float) Math.sin(theta*Math.PI/180.0f));
            Vector3f area_norm=new Vector3f();
            norm.rotateAxis( (float)(theta*Math.PI/180.0f),rotate_axis.x,rotate_axis.y,rotate_axis.z,area_norm);
            float cos=area_norm.angleCos(speed_vector_in_object_frame);
            float S_eq=Math.abs(cos)*area;

            //compute norm_lift
            Vector3f norm_lift=new Vector3f();
            speed_vector_in_object_frame.cross(area_norm,norm_lift);
            norm_lift.cross(speed_vector_in_object_frame);
            norm_lift.normalize();
            //compute norm_friction
            Vector3f norm_friction=new Vector3f();
            speed_vector_in_object_frame.normalize(norm_friction);
            //Compute Actual lift and friction coefficients
            float C_l; float C_d;
            if(cos>=0&&cos<=(float) Math.cos(70*Math.PI/180.0f))
            {
                if (balance_C_l==0.0f)
                {
                    C_l=cos*(0.4f/(float) Math.cos(70*Math.PI/180.0f));
                }
                else {
                    C_l = balance_C_l + cos * (3 * balance_C_l / (float) Math.cos(70 * Math.PI / 180.0f));
                }
            }

            else if (cos<0&&cos>=(float) Math.cos(110f*Math.PI/180.0f))
            {
                if (balance_C_l==0)
                {
                    C_l=cos*(-0.4f/(float) Math.cos(70*Math.PI/180.0f));
                }
                else {
                    C_l = balance_C_l + cos * (-3 * balance_C_l / (float) Math.cos(110 * Math.PI / 180.0f));
                }
            }
            else
            {
                C_l=0;
            }

            C_d=balance_C_d+cos*cos*3*balance_C_d;
            //Compute lift force and friction force
            Vector3f force_in_obj_frame=new Vector3f();
            float lift_coefficient=air_density*(speed_vector_in_object_frame.lengthSquared())*C_l*S_eq;
            float friction_coefficient=air_density*(speed_vector_in_object_frame.lengthSquared())*C_d*S_eq;
            Vector3f lift_force=new Vector3f();
            Vector3f friction_force=new Vector3f();
            norm_lift.mul(lift_coefficient,lift_force);
            norm_friction.mul(friction_coefficient,friction_force);
            lift_force.add(friction_force,force_in_obj_frame);
            //Compute the moment of this force
            Vector3f moment_in_obj_frame=new Vector3f();
            relative_pos.cross(force_in_obj_frame,moment_in_obj_frame);
            List<Vector3f> force_and_moment=new ArrayList<>();
            force_and_moment.add(force_in_obj_frame);
            force_and_moment.add(moment_in_obj_frame);
            return force_and_moment;
        }

    }
    protected AirDynamicElement left_wing;
    protected AirDynamicElement right_wing;
    protected AirDynamicElement tail;
    protected AirDynamicElement vertical_tail;

    protected Vector3f speed_vector_in_global_frame;
    protected Vector3f  speed_vector_in_object_frame=new Vector3f();
    protected float air_density=0.013f;
    protected float throttle_percentage=0.0f;




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

    protected Vector3f EulerAnglesFromRotationMatrix()
    {
        // from RotMat to Euler Angle.
        //y axis: gamma
        //x axis: beta
        //z axis: alpha
        float gamma;
        float beta;
        float alpha;
        double sy=Math.sqrt(Math.fma(RotationMatrix1to0.m11,RotationMatrix1to0.m11,RotationMatrix1to0.m01*RotationMatrix1to0.m01));


        beta=(float) Math.atan2(RotationMatrix1to0.m21,sy);//beta from -pi/2 to pi/2

        if (beta==Math.PI/2 || beta==-Math.PI/2)//singularity
            {
                LOGGER.info("singular");
                alpha=0.0f;
                gamma=(float) Math.atan2(RotationMatrix1to0.m02,RotationMatrix1to0.m00);
            }
        else
            {
                gamma = (float) Math.atan2(-RotationMatrix1to0.m20 / Math.cos(beta), RotationMatrix1to0.m22 / Math.cos(beta));
                alpha = (float) Math.atan2(-RotationMatrix1to0.m01 / Math.cos(beta), RotationMatrix1to0.m11 / Math.cos(beta));
            }

            //beta from -pi/2 to pi/2
            //gamma from -pi to pi
            //alpha from -pi to pi

            beta=beta*(180.0f/(float) Math.PI);
            gamma=gamma*(180.0f/(float) Math.PI);
            alpha=alpha*(180.0f/(float) Math.PI);
            //beta from -90 to 90
            //gamma from -180 to 180
            //alpha from -180 to 180


        return new Vector3f(gamma,beta,alpha);

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


        Matrix3f RYXZ=new Matrix3f();
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

    protected List<Vector3f> ComputeLinearAndAngularAccInObjectFrame(List<Vector3f> force_and_moment ,Matrix3f MassCenterInertia,float Mass)
    {
        //Assume MassCenterInertia some times of identity matrix
        //Rigid Body Dynamics Formula N=Iw+wx(Iw)
        //I=lambda Identity, meaning the cross product term is 0.
        //then N=Iw,w=I^-1N
        Vector3f linear_acc=new Vector3f();
        force_and_moment.get(0).div(Mass,linear_acc);



        Matrix3f Inverted_Inertia=new Matrix3f();
        MassCenterInertia.invert(Inverted_Inertia);
        Vector3f angular_acc=new Vector3f();
        force_and_moment.get(1).mulTranspose(Inverted_Inertia,angular_acc);

        List<Vector3f> linear_and_angular_acc=new ArrayList<>();
        linear_and_angular_acc.add(linear_acc);
        linear_and_angular_acc.add(angular_acc);

        return  linear_and_angular_acc;
    }



    protected List<Vector3f> RotateToGlobal(List<Vector3f> vec_list)
    {
        List<Vector3f> global_vec_list=new ArrayList<>();
        Vector3f linear_acc_in_glb_frame=new Vector3f();
        Vector3f angular_acc_in_glb_frame=new Vector3f();
        vec_list.get(0).mulTranspose(this.RotationMatrix1to0,linear_acc_in_glb_frame);
        vec_list.get(1).mulTranspose(this.RotationMatrix1to0,angular_acc_in_glb_frame);
        global_vec_list.add(linear_acc_in_glb_frame);
        global_vec_list.add(angular_acc_in_glb_frame);

        return global_vec_list;
    }

    protected List<Vector3f> ComputeSumOfForcesAndMoments()
    {
        //compute sum force
        Vector3f gravity=ComputeGravityInObjFrame();
        Vector3f engine_force=ComputeEngineForceInObjFrame();
        Vector3f left_wing_force=left_wing.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(0);
        Vector3f right_wing_force=right_wing.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(0);
        Vector3f tail_force=tail.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(0);
        Vector3f vertical_tail_force=vertical_tail.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(0);

        Vector3f sum_force=new Vector3f();
        sum_force.add(gravity).add(engine_force).add(left_wing_force).add(right_wing_force).add(tail_force).add(vertical_tail_force);
        //compute sum moment, assume no moment for gravity and engine
        Vector3f left_wing_moment=left_wing.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(1);
        Vector3f right_wing_moment=right_wing.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(1);
        Vector3f tail_moment=tail.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(1);
        Vector3f vertical_tail_moment=vertical_tail.ComputeWingAirDynamicForcesInObjFrame(speed_vector_in_object_frame,air_density).get(1);
        Vector3f sum_moment=new Vector3f();
        sum_moment.add(left_wing_moment).add(right_wing_moment).add(tail_moment).add(vertical_tail_moment);

        List<Vector3f> force_and_moment= new ArrayList<>();
        force_and_moment.add(sum_force);
        force_and_moment.add(sum_moment);
        return force_and_moment;


    }



    protected Vector3f ComputeEngineForceInObjFrame()
    {
        return new Vector3f(0,0,(maximum_power*throttle_percentage)/speed_vector_in_object_frame.z);
    }

    protected Vector3f ComputeGravityInObjFrame()
    {
        Matrix3f ZeroToOne=new Matrix3f();
        this.RotationMatrix1to0.invert(ZeroToOne);
        Vector3f gravity_global=new Vector3f(0.0f,-9.8f*this.mass,0.0f);
        Vector3f gravity_object=new Vector3f();
        gravity_global.mulTranspose(ZeroToOne,gravity_object);
        return  gravity_object;
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

    @Override
    public void tick() {
        super.tick();
        this.zRotO = this.getZRot();

        //this.RotateSmallValueAround(0.05f,new Vector3f(1,0,1));
        this.UseEulerAngleToSetRotMatrix(45.0f,90.0f,-30.0f);
        //LOGGER.info("00:"+String.valueOf(RotationMatrix1to0.m00)+"01:"+String.valueOf(RotationMatrix1to0.m01)+"02:"+String.valueOf(RotationMatrix1to0.m02)
                //+"10:"+String.valueOf(RotationMatrix1to0.m10)+"11:"+String.valueOf(RotationMatrix1to0.m11)+"12:"+String.valueOf(RotationMatrix1to0.m12)
                //+"20:"+String.valueOf(RotationMatrix1to0.m20)+"21:"+String.valueOf(RotationMatrix1to0.m21)+"22:"+String.valueOf(RotationMatrix1to0.m22));
        //LOGGER.info("20:"+String.valueOf(RotationMatrix1to0.m20)+"21:"+String.valueOf(RotationMatrix1to0.m21)+"22:"+String.valueOf(RotationMatrix1to0.m22));
        Vector3f vec=this.EulerAnglesFromRotationMatrix();
        LOGGER.info("yaw:"+String.valueOf(vec.x)+"   "+"pitch"+String.valueOf(vec.y)+"   "+"roll"+String.valueOf(vec.z));
        this.setYRot(-vec.x);//y axis left-hand grabbing law
        this.setXRot(vec.y);//x axis right-hand grabbing law
        this.setZRot(-vec.z);//z axis left-hand grabbing law


        List<Vector3f> linear_and_angular_acc_in_glb=RotateToGlobal(ComputeLinearAndAngularAccInObjectFrame(ComputeSumOfForcesAndMoments(),inertia,mass));
        Vector3f linear_acc= linear_and_angular_acc_in_glb.get(0);
        Vector3f angular_acc=linear_and_angular_acc_in_glb.get(1);


    }
}




