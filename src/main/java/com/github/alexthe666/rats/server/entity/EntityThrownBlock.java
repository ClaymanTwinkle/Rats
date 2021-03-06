package com.github.alexthe666.rats.server.entity;


import com.github.alexthe666.rats.RatsMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityThrownBlock extends Entity {
    public EntityLivingBase shootingEntity;
    public IBlockState fallTile;
    public boolean dropBlock = true;
    public NBTTagCompound tileEntityData;
    private int ticksAlive;
    private int ticksInAir;

    public EntityThrownBlock(World worldIn) {
        super(worldIn);
        this.setSize(0.98F, 0.98F);
    }

    public EntityThrownBlock(World worldIn, IBlockState blockState, EntityLivingBase entityNeoRatlantean) {
        super(worldIn);
        this.setSize(0.98F, 0.98F);
        this.fallTile = blockState;
        this.shootingEntity = entityNeoRatlantean;
    }

    public static void registerFixesFireball(DataFixer fixer, String name) {
    }

    protected void entityInit() {
    }

    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        double d0 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0D;

        if (Double.isNaN(d0)) {
            d0 = 4.0D;
        }

        d0 = d0 * 64.0D;
        return distance < d0 * d0;
    }

    public void onUpdate() {
        if (this.world.isRemote || (this.shootingEntity == null || !this.shootingEntity.isDead) && this.world.isBlockLoaded(new BlockPos(this))) {
            super.onUpdate();

            ++this.ticksInAir;
            if (ticksInAir > 25) {
                this.noClip = true;
                RayTraceResult raytraceresult = ProjectileHelper.forwardsRaycast(this, true, this.ticksInAir >= 25, this.shootingEntity);
                if (raytraceresult != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
                    this.onImpact(raytraceresult);
                }
            } else {
                this.noClip = false;
            }
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            ProjectileHelper.rotateTowardsMovement(this, 0.2F);
            float f = this.getMotionFactor();

            if (this.isInWater()) {
                for (int i = 0; i < 4; ++i) {
                    float f1 = 0.25F;
                    this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ);
                }

                f = 0.8F;
            }
            this.motionX *= (double) f;
            this.motionY *= (double) f;
            this.motionZ *= (double) f;
            if (this.shootingEntity != null && shootingEntity instanceof EntityLiving) {
                if (((EntityLiving) shootingEntity).getAttackTarget() != null) {
                    EntityLivingBase target = ((EntityLiving) shootingEntity).getAttackTarget();
                    double d0 = target.posX - this.posX;
                    double d1 = target.posY - this.posY;
                    double d2 = target.posZ - this.posZ;
                    double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                    d3 = (double) MathHelper.sqrt(d3);
                    this.motionX += d0 / d3 * 0.2D;
                    this.motionY += d1 / d3 * 0.2D;
                    this.motionZ += d2 / d3 * 0.2D;
                }
            }
            this.setPosition(this.posX, this.posY, this.posZ);
        } else {
            this.setDead();
        }
    }

    public boolean canEntityBeSeen(Entity entityIn) {
        return this.world.rayTraceBlocks(new Vec3d(this.posX, this.posY + (double) this.getEyeHeight(), this.posZ), new Vec3d(entityIn.posX, entityIn.posY + (double) entityIn.getEyeHeight(), entityIn.posZ), false, true, false) == null;
    }

    protected float getMotionFactor() {
        return 0.95F;
    }

    protected void onImpact(RayTraceResult result) {
        if (fallTile != null) {
            Block block = this.fallTile.getBlock();
            if (result != null && result.getBlockPos() != null) {
                for (Entity hitMobs : world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(1.0F, 1.0F, 1.0F))) {
                    hitMobs.attackEntityFrom(DamageSource.IN_WALL, RatsMod.CONFIG_OPTIONS.neoRatlanteanAttack);
                }
                BlockPos blockpos1 = result.getBlockPos().up();
                if (this.world.mayPlace(block, blockpos1, true, EnumFacing.UP, null)) {
                    if (dropBlock) {
                        this.world.setBlockState(blockpos1, this.fallTile);
                    }
                    if (block instanceof BlockFalling) {
                        ((BlockFalling) block).onEndFalling(this.world, blockpos1, this.fallTile, fallTile);
                    }
                    if (this.tileEntityData != null && block.hasTileEntity(this.fallTile)) {
                        TileEntity tileentity = this.world.getTileEntity(blockpos1);

                        if (tileentity != null) {
                            NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());

                            for (String s : this.tileEntityData.getKeySet()) {
                                NBTBase nbtbase = this.tileEntityData.getTag(s);

                                if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
                                    nbttagcompound.setTag(s, nbtbase.copy());
                                }
                            }

                            tileentity.readFromNBT(nbttagcompound);
                            tileentity.markDirty();
                        }
                    }
                    this.setDead();
                } else if (this.world.getGameRules().getBoolean("doEntityDrops")) {
                    if (!world.isRemote && dropBlock) {
                        this.entityDropItem(new ItemStack(block, 1, block.damageDropped(this.fallTile)), 0.0F);
                    }
                    this.setDead();
                }
            }
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.setTag("direction", this.newDoubleNBTList(this.motionX, this.motionY, this.motionZ));
        compound.setInteger("life", this.ticksAlive);
        Block block = this.fallTile != null ? this.fallTile.getBlock() : Blocks.AIR;
        ResourceLocation resourcelocation = Block.REGISTRY.getNameForObject(block);
        compound.setString("Block", resourcelocation == null ? "" : resourcelocation.toString());
        compound.setByte("Data", (byte) block.getMetaFromState(this.fallTile));
        if (this.tileEntityData != null) {
            compound.setTag("TileEntityData", this.tileEntityData);
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("power", 9)) {
            NBTTagList nbttaglist = compound.getTagList("power", 6);
        }

        this.ticksAlive = compound.getInteger("life");

        if (compound.hasKey("direction", 9) && compound.getTagList("direction", 6).tagCount() == 3) {
            NBTTagList nbttaglist1 = compound.getTagList("direction", 6);
            this.motionX = nbttaglist1.getDoubleAt(0);
            this.motionY = nbttaglist1.getDoubleAt(1);
            this.motionZ = nbttaglist1.getDoubleAt(2);
        } else {
            this.setDead();
        }
        int i = compound.getByte("Data") & 255;

        if (compound.hasKey("Block", 8)) {
            this.fallTile = Block.getBlockFromName(compound.getString("Block")).getStateFromMeta(i);
        } else if (compound.hasKey("TileID", 99)) {
            this.fallTile = Block.getBlockById(compound.getInteger("TileID")).getStateFromMeta(i);
        } else {
            this.fallTile = Block.getBlockById(compound.getByte("Tile") & 255).getStateFromMeta(i);
        }

        Block block = this.fallTile.getBlock();
        if (block == null || block.getDefaultState().getMaterial() == Material.AIR) {
            this.fallTile = Blocks.GRASS.getDefaultState();
        }
        if (compound.hasKey("TileEntityData", 10)) {
            this.tileEntityData = compound.getCompoundTag("TileEntityData");
        }
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith() {
        return true;
    }

    public float getCollisionBorderSize() {
        return 1.0F;
    }

    /**
     * Called when the entity is attacked.
     */
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        } else {
            this.markVelocityChanged();

            if (source.getTrueSource() != null) {
                Vec3d vec3d = source.getTrueSource().getLookVec();

                if (vec3d != null) {
                    this.motionX = vec3d.x;
                    this.motionY = vec3d.y;
                    this.motionZ = vec3d.z;
                }

                if (source.getTrueSource() instanceof EntityLivingBase) {
                    this.shootingEntity = (EntityLivingBase) source.getTrueSource();
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public float getBrightness() {
        return 1.0F;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 15728880;
    }
}