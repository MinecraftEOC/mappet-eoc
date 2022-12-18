package mchorse.mappet.api.scripts.code;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.Unpooled;
import mchorse.blockbuster.common.tileentity.TileEntityModel;
import mchorse.blockbuster.network.common.PacketModifyModelBlock;
import mchorse.mappet.Mappet;
import mchorse.mappet.api.npcs.Npc;
import mchorse.mappet.api.npcs.NpcState;
import mchorse.mappet.api.scripts.code.blocks.ScriptBlockState;
import mchorse.mappet.api.scripts.code.blocks.ScriptTileEntity;
import mchorse.mappet.api.scripts.code.entities.ScriptEntity;
import mchorse.mappet.api.scripts.code.entities.ScriptNpc;
import mchorse.mappet.api.scripts.code.items.ScriptInventory;
import mchorse.mappet.api.scripts.code.nbt.ScriptNBTCompound;
import mchorse.mappet.api.scripts.user.IScriptRayTrace;
import mchorse.mappet.api.scripts.user.IScriptWorld;
import mchorse.mappet.api.scripts.user.blocks.IScriptBlockState;
import mchorse.mappet.api.scripts.user.blocks.IScriptTileEntity;
import mchorse.mappet.api.scripts.user.data.ScriptVector;
import mchorse.mappet.api.scripts.user.entities.IScriptEntity;
import mchorse.mappet.api.scripts.user.entities.IScriptNpc;
import mchorse.mappet.api.scripts.user.entities.IScriptPlayer;
import mchorse.mappet.api.scripts.user.items.IScriptInventory;
import mchorse.mappet.api.scripts.user.items.IScriptItemStack;
import mchorse.mappet.api.scripts.user.nbt.INBTCompound;
import mchorse.mappet.api.utils.RayTracing;
import mchorse.mappet.client.morphs.WorldMorph;
import mchorse.mappet.entities.EntityNpc;
import mchorse.mappet.network.Dispatcher;
import mchorse.mappet.network.common.scripts.PacketWorldMorph;
import mchorse.mappet.utils.WorldUtils;
import mchorse.mclib.utils.MathUtils;
import mchorse.metamorph.api.MorphManager;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScriptWorld implements IScriptWorld
{
    public static final int MAX_VOLUME = 100;

    private World world;
    private BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public ScriptWorld(World world)
    {
        this.world = world;
    }

    @Override
    public World getMinecraftWorld()
    {
        return this.world;
    }

    @Override
    public void setBlock(IScriptBlockState state, int x, int y, int z)
    {
        if (!this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            return;
        }

        this.world.setBlockState(this.pos, state.getMinecraftBlockState());
    }

    @Override
    public IScriptBlockState getBlock(int x, int y, int z)
    {
        if (!this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            return ScriptBlockState.AIR;
        }

        IBlockState blockState = this.world.getBlockState(this.pos);

        return ScriptBlockState.create(blockState.getActualState(this.world, this.pos));
    }

    @Override
    public boolean hasTileEntity(int x, int y, int z)
    {
        if (!this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            return false;
        }

        return this.world.getTileEntity(this.pos) != null;
    }

    @Override
    public IScriptTileEntity getTileEntity(int x, int y, int z)
    {
        if (!this.hasTileEntity(x, y, z))
        {
            return null;
        }

        return new ScriptTileEntity(this.world.getTileEntity(this.pos.setPos(x, y, z)));
    }

    @Override
    public boolean hasInventory(int x, int y, int z)
    {
        this.pos.setPos(x, y, z);

        return this.world.isBlockLoaded(this.pos) && this.world.getTileEntity(this.pos) instanceof IInventory;
    }

    @Override
    public IScriptInventory getInventory(int x, int y, int z)
    {
        if (this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            TileEntity tile = this.world.getTileEntity(this.pos);

            if (tile instanceof IInventory)
            {
                return new ScriptInventory((IInventory) tile);
            }
        }

        return null;
    }

    @Override
    public boolean isRaining()
    {
        return this.world.getWorldInfo().isRaining();
    }

    @Override
    public void setRaining(boolean raining)
    {
        this.world.getWorldInfo().setRaining(raining);
    }

    @Override
    public long getTime()
    {
        return this.world.getWorldTime();
    }

    @Override
    public void setTime(long time)
    {
        this.world.setWorldTime(time);
    }

    @Override
    public long getTotalTime()
    {
        return this.world.getTotalWorldTime();
    }

    @Override
    public int getDimensionId()
    {
        Integer[] ids = DimensionManager.getIDs();

        for (int id : ids)
        {
            World world = this.world.getMinecraftServer().getWorld(id);

            if (world == this.world)
            {
                return id;
            }
        }

        return 0;
    }

    @Override
    public void spawnParticles(EnumParticleTypes type, boolean longDistance, double x, double y, double z, int n, double dx, double dy, double dz, double speed, int... args)
    {
        ((WorldServer) this.world).spawnParticle(type, longDistance, x, y, z, n, dx, dy, dz, speed, args);
    }

    @Override
    public void spawnParticles(IScriptPlayer entity, EnumParticleTypes type, boolean longDistance, double x, double y, double z, int n, double dx, double dy, double dz, double speed, int... args)
    {
        if (entity == null)
        {
            return;
        }

        ((WorldServer) this.world).spawnParticle(entity.getMinecraftPlayer(), type, longDistance, x, y, z, n, dx, dy, dz, speed, args);
    }

    @Override
    public IScriptEntity spawnEntity(String id, double x, double y, double z, INBTCompound compound)
    {
        if (!this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            return null;
        }

        NBTTagCompound tag = new NBTTagCompound();

        if (compound != null)
        {
            tag.merge(compound.getNBTTagCompound());
        }

        tag.setString("id", id);

        Entity entity = AnvilChunkLoader.readWorldEntityPos(tag, this.world, x, y, z, true);

        return entity == null ? null : ScriptEntity.create(entity);
    }

    @Override
    public IScriptNpc spawnNpc(String id, String state, double x, double y, double z)
    {
        Npc npc = Mappet.npcs.load(id);

        if (npc == null)
        {
            return null;
        }

        NpcState npcState = npc.states.get(state);

        if (npcState == null)
        {
            return null;
        }

        EntityNpc entity = new EntityNpc(this.world);

        entity.setPosition(x, y, z);
        entity.setNpc(npc, npcState);

        entity.world.spawnEntity(entity);
        entity.initialize();

        if (!npc.serializeNBT().getString("StateName").equals("default"))
        {
            entity.setStringInData("StateName", state);
        }

        return new ScriptNpc(entity);
    }

    @Override
    public List<IScriptEntity> getEntities(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        List<IScriptEntity> entities = new ArrayList<IScriptEntity>();

        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double minZ = Math.min(z1, z2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        double maxZ = Math.max(z1, z2);

        if (maxX - minX > MAX_VOLUME || maxY - minY > MAX_VOLUME || maxZ - minZ > MAX_VOLUME)
        {
            return entities;
        }

        if (!this.world.isBlockLoaded(this.pos.setPos(minX, minY, minZ)) || !this.world.isBlockLoaded(this.pos.setPos(maxX, maxY, maxZ)))
        {
            return entities;
        }

        for (Entity entity : this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)))
        {
            entities.add(ScriptEntity.create(entity));
        }

        return entities;
    }

    @Override
    public List<IScriptEntity> getEntities(double x, double y, double z, double radius)
    {
        radius = Math.abs(radius);
        List<IScriptEntity> entities = new ArrayList<IScriptEntity>();

        if (radius > MAX_VOLUME / 2)
        {
            return entities;
        }

        double minX = x - radius;
        double minY = y - radius;
        double minZ = z - radius;
        double maxX = x + radius;
        double maxY = y + radius;
        double maxZ = z + radius;

        if (!this.world.isBlockLoaded(this.pos.setPos(minX, minY, minZ)) || !this.world.isBlockLoaded(this.pos.setPos(maxX, maxY, maxZ)))
        {
            return entities;
        }

        for (Entity entity : this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)))
        {
            AxisAlignedBB box = entity.getEntityBoundingBox();
            double eX = (box.minX + box.maxX) / 2D;
            double eY = (box.minY + box.maxY) / 2D;
            double eZ = (box.minZ + box.maxZ) / 2D;

            double dX = x - eX;
            double dY = y - eY;
            double dZ = z - eZ;

            if (dX * dX + dY * dY + dZ * dZ < radius * radius)
            {
                entities.add(ScriptEntity.create(entity));
            }
        }

        return entities;
    }

    @Override
    public void playSound(String event, double x, double y, double z, float volume, float pitch)
    {
        for (EntityPlayerMP player : this.world.getMinecraftServer().getPlayerList().getPlayers())
        {
            WorldUtils.playSound(player, event, x, y, z, volume, pitch);
        }
    }

    @Override
    public void stopSound(String event, String category)
    {
        PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());

        packetbuffer.writeString(category);
        packetbuffer.writeString(event);

        for (EntityPlayerMP player : this.world.getMinecraftServer().getPlayerList().getPlayers())
        {
            player.connection.sendPacket(new SPacketCustomPayload("MC|StopSound", packetbuffer));
        }
    }

    @Override
    public IScriptEntity dropItemStack(IScriptItemStack stack, double x, double y, double z, double mx, double my, double mz)
    {
        if (stack == null || stack.isEmpty())
        {
            return null;
        }

        EntityItem item = new EntityItem(this.world, x, y, z, stack.getMinecraftItemStack().copy());

        item.motionX = mx;
        item.motionY = my;
        item.motionZ = mz;

        this.world.spawnEntity(item);

        return ScriptEntity.create(item);
    }

    @Override
    public void explode(IScriptEntity exploder, double x, double y, double z, float distance, boolean blazeGround, boolean destroyTerrain)
    {
        this.world.newExplosion(exploder == null ? null : exploder.getMinecraftEntity(), x, y, z, distance, blazeGround, destroyTerrain);
    }

    @Override
    public IScriptRayTrace rayTrace(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return new ScriptRayTrace(RayTracing.rayTraceWithEntity(this.world, x1, y1, z1, x2, y2, z2));
    }

    @Override
    public IScriptRayTrace rayTraceBlock(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return new ScriptRayTrace(RayTracing.rayTrace(this.world, x1, y1, z1, x2, y2, z2));
    }

    /* Mappet stuff */

    @Override
    public void displayMorph(AbstractMorph morph, int expiration, double x, double y, double z, float yaw, float pitch, int range, IScriptPlayer player)
    {
        if (morph == null)
        {
            return;
        }

        WorldMorph worldMorph = new WorldMorph();

        worldMorph.morph = morph;
        worldMorph.expiration = expiration;
        worldMorph.x = x;
        worldMorph.y = y;
        worldMorph.z = z;
        worldMorph.yaw = yaw;
        worldMorph.pitch = pitch;

        int dimension = this.world.provider.getDimension();

        if (player == null)
        {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(dimension, x, y, z, MathUtils.clamp(range, 1, 256));

            Dispatcher.DISPATCHER.get().sendToAllAround(new PacketWorldMorph(worldMorph), point);
        }
        else
        {
            Dispatcher.sendTo(new PacketWorldMorph(worldMorph), player.getMinecraftPlayer());
        }
    }

    @Override
    public void setModelBlockMorph(String nbt, int x, int y, int z, boolean force)
    {
        if (!this.world.isBlockLoaded(this.pos.setPos(x, y, z)))
        {
            return;
        }

        if (Loader.isModLoaded("blockbuster"))
        {
            this.setModelBlockMorphBlockbuster(nbt, x, y, z, force);
        }
    }

    @Optional.Method(modid = "blockbuster")
    private void setModelBlockMorphBlockbuster(String nbt, int x, int y, int z, boolean force)
    {
        try
        {
            AbstractMorph morph = MorphManager.INSTANCE.morphFromNBT(JsonToNBT.getTagFromJson(nbt));
            TileEntity tile = this.world.getTileEntity(new BlockPos(x, y, z));

            if (tile instanceof TileEntityModel)
            {
                TileEntityModel model = (TileEntityModel) tile;

                if (Objects.equals(model.morph.get(), morph) && !force)
                {
                    return;
                }

                model.setMorph(morph);

                PacketModifyModelBlock message = new PacketModifyModelBlock(model.getPos(), model, true);
                NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.world.provider.getDimension(), x, y, z, 64);

                mchorse.blockbuster.network.Dispatcher.DISPATCHER.get().sendToAllAround(message, point);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isActive(int x, int y, int z)
    {
        IBlockState blockState = this.world.getBlockState(this.pos.setPos(x, y, z));

        return blockState.getProperties().containsKey(BlockButton.POWERED) ? blockState.getValue(BlockButton.POWERED) : false;
    }

    @Override
    public boolean testForBlock(int x, int y, int z, String blockId, int meta)
    {
        Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
        ImmutableList<IBlockState> validStates = value.getBlockState().getValidStates();
        IBlockState state = meta >= 0 && meta < validStates.size() ? validStates.get(meta) : value.getDefaultState();

        return this.getBlock(x, y, z).isSame(ScriptBlockState.create(state));
    }

    @Override
    public void fill(IScriptBlockState state, int x1, int y1, int z1, int x2, int y2, int z2)
    {
        int xMin = Math.min(x1, x2);
        int xMax = Math.max(x1, x2);
        int yMin = Math.min(y1, y2);
        int yMax = Math.max(y1, y2);
        int zMin = Math.min(z1, z2);
        int zMax = Math.max(z1, z2);

        for (int x = xMin; x <= xMax; x++)
        {
            for (int y = yMin; y <= yMax; y++)
            {
                for (int z = zMin; z <= zMax; z++)
                {
                    this.setBlock(state, x, y, z);
                }
            }
        }
    }

    @Override
    public IScriptEntity summonFallingBlock(double x, double y, double z, String blockId, int meta)
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("Block", blockId);
        nbt.setInteger("Data", meta);
        nbt.setInteger("Time", 1);

        return this.spawnEntity("minecraft:falling_block", x, y, z, new ScriptNBTCompound(nbt));
    }

    @Override
    public IScriptEntity setFallingBlock(int x, int y, int z)
    {
        IScriptBlockState state = getBlock(x, y, z);

        if (state.isAir())
        {
            return null;
        }

        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("Block", state.getBlockId());
        nbt.setInteger("Data", state.getMeta());
        nbt.setInteger("Time", 1);

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(state.getBlockId()));

        if (block != null)
        {
            IBlockState blockState = block.getStateFromMeta(state.getMeta());

            for (IProperty<?> property : blockState.getProperties().keySet())
            {
                nbt.setString(property.getName(), blockState.getValue(property).toString());
            }
        }

        if (hasTileEntity(x, y, z))
        {
            nbt.setTag("TileEntityData", this.getTileEntity(x, y, z).getData().getNBTTagCompound());
        }

        this.world.removeTileEntity(this.pos.setPos(x, y, z));
        this.world.setBlockState(this.pos.setPos(x, y, z), Blocks.AIR.getDefaultState(), 3);

        return this.spawnEntity("minecraft:falling_block", x + 0.5, y + 0.5, z + 0.5, new ScriptNBTCompound(nbt));
    }

    @Override
    public List<IScriptEntity> fancyExplode(int x1, int y1, int z1, int x2, int y2, int z2, float blocksPercentage)
    {
        List<IScriptEntity> entities = new ArrayList<IScriptEntity>();
        int xMin = Math.min(x1, x2);
        int xMax = Math.max(x1, x2);
        int yMin = Math.min(y1, y2);
        int yMax = Math.max(y1, y2);
        int zMin = Math.min(z1, z2);
        int zMax = Math.max(z1, z2);
        int xCentre = (xMin + xMax) / 2;
        int yCentre = (yMin + yMax) / 2;
        int zCentre = (zMin + zMax) / 2;

        this.playSound("minecraft:entity.generic.explode", xCentre, yCentre, zCentre, 1, 1);

        for (int x = xMin; x <= xMax; x++)
        {
            for (int y = yMin; y <= yMax; y++)
            {
                for (int z = zMin; z <= zMax; z++)
                {
                    /* If the blocks are closer the centre, they are more likely to be destroyed */
                    if (Math.random() < (1 - (Math.sqrt(Math.pow(xCentre - x, 2) + Math.pow(yCentre - y, 2) + Math.pow(zCentre - z, 2)) / Math.sqrt(Math.pow(xCentre - xMin, 2) + Math.pow(yCentre - yMin, 2) + Math.pow(zCentre - zMin, 2)))) * blocksPercentage)
                    {
                        IScriptEntity entity = this.setFallingBlock(x, y, z);

                        if (entity != null)
                        {
                            entity.addMotion((x - xCentre) / 2.0, (y - yCentre) / 2.0, (z - zCentre) / 2.0);
                            entities.add(entity);

                            ScriptVector pos = entity.getPosition();

                            this.spawnParticles(EnumParticleTypes.EXPLOSION_NORMAL, true, pos.x, pos.y, pos.z, 10, 1, 1, 1, 0.1);
                        }
                    }
                }
            }
        }

        return entities;
    }

    @Override
    public void tpExplode(int x1, int y1, int z1, int x2, int y2, int z2, float blocksPercentage)
    {
        int xMin = Math.min(x1, x2);
        int xMax = Math.max(x1, x2);
        int yMin = Math.min(y1, y2);
        int yMax = Math.max(y1, y2);
        int zMin = Math.min(z1, z2);
        int zMax = Math.max(z1, z2);
        int xCentre = (xMin + xMax) / 2;
        int yCentre = (yMin + yMax) / 2;
        int zCentre = (zMin + zMax) / 2;

        this.playSound("minecraft:entity.generic.explode", xCentre, yCentre, zCentre, 1, 1);

        for (int x = xMin; x <= xMax; x++)
        {
            for (int y = yMin; y <= yMax; y++)
            {
                for (int z = zMin; z <= zMax; z++)
                {
                    /* If the blocks are closer the centre, they are more likely to be destroyed */
                    if (Math.random() < (1 - (Math.sqrt(Math.pow(xCentre - x, 2) + Math.pow(yCentre - y, 2) + Math.pow(zCentre - z, 2)) / Math.sqrt(Math.pow(xCentre - xMin, 2) + Math.pow(yCentre - yMin, 2) + Math.pow(zCentre - zMin, 2)))) * blocksPercentage)
                    {
                        IScriptBlockState state = getBlock(x, y, z);
                        if (!state.getBlockId().equals("minecraft:air"))
                        {
                            int xNew = (int) (Math.random() * (xMax - xMin) + xMin);
                            int yNew = (int) (Math.random() * (yMax - yMin) + yMin);
                            int zNew = (int) (Math.random() * (zMax - zMin) + zMin);

                            this.setBlock(ScriptBlockState.create(Blocks.AIR.getDefaultState()), x, y, z);
                            this.setBlock(state, xCentre + (int) (Math.random() * 10 - 5), yCentre + (int) (Math.random() * 10 - 5), zCentre + (int) (Math.random() * 10 - 5));

                            spawnParticles(EnumParticleTypes.EXPLOSION_NORMAL, true, xNew + 0.5, yNew + 0.5, zNew + 0.5, 10, 1, 1, 1, 0.1);
                        }
                    }
                }
            }
        }
    }
}