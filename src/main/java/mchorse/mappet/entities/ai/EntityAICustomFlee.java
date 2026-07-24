package mchorse.mappet.entities.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

public class EntityAICustomFlee extends EntityAIBase {
    private final EntityCreature creature;
    private final double speed;
    private final float minDistance; // Минимальная дистанция для активации бегства
    private EntityPlayer fleeFrom; // Цель, от которой бежим

    public EntityAICustomFlee(EntityCreature creature, double speed, float minDistance) {
        this.creature = creature;
        this.speed = speed;
        this.minDistance = minDistance;
        this.setMutexBits(1); // Предотвращает конфликты с другими задачами ИИ
    }

    @Override
    public boolean shouldExecute() {
        EntityPlayer closestPlayer = this.creature.world.getClosestPlayerToEntity(this.creature, this.minDistance);

        if (closestPlayer != null &&
            !closestPlayer.capabilities.isCreativeMode &&
            !closestPlayer.isSpectator() &&
            this.creature.getEntitySenses().canSee(closestPlayer)) {
            
            this.fleeFrom = closestPlayer;
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.fleeFrom != null &&
               !this.fleeFrom.isDead &&
               this.creature.getDistance(this.fleeFrom) < this.minDistance * 2;
    }

    @Override
    public void startExecuting() {
        Vec3d fleePos = findFleePosition();
        if (fleePos != null) {
            this.creature.getNavigator().tryMoveToXYZ(fleePos.x, fleePos.y, fleePos.z, this.speed);
        }
    }

    @Override
    public void updateTask() {
        if (this.creature.getNavigator().noPath()) {
            Vec3d fleePos = findFleePosition();
            if (fleePos != null) {
                this.creature.getNavigator().tryMoveToXYZ(fleePos.x, fleePos.y, fleePos.z, this.speed);
            }
        }
    }

    @Override
    public void resetTask() {
        this.fleeFrom = null;
        this.creature.getNavigator().clearPath();
    }

    private Vec3d findFleePosition() {
        if (this.fleeFrom == null) return null;

        return RandomPositionGenerator.findRandomTargetBlockAwayFrom(
            this.creature,
            8, // Радиус поиска
            3, // Вертикальный диапазон
            new Vec3d(this.fleeFrom.posX, this.fleeFrom.posY, this.fleeFrom.posZ)
        );
    }
}