package com.andrei1058.bedwars.support.version.v1_14_R1.entities;

import com.andrei1058.bedwars.api.team.TeamColor;
import com.andrei1058.bedwars.arena.BedWarsTeam;
import com.andrei1058.bedwars.configuration.ConfigPath;
import com.andrei1058.bedwars.language.Messages;
import net.minecraft.server.v1_14_R1.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static com.andrei1058.bedwars.Main.lang;
import static com.andrei1058.bedwars.Main.shop;

@SuppressWarnings("unchecked")
public class IGolem extends EntityIronGolem {

    private BedWarsTeam bedWarsTeam;

    private IGolem(EntityTypes<? extends EntityIronGolem> entitytypes, World world, BedWarsTeam bedWarsTeam) {
        super(entitytypes, world);
        this.bedWarsTeam = bedWarsTeam;
    }

    public IGolem(EntityTypes entityTypes, World world) {
        super(entityTypes, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this));
        if (bedWarsTeam != null) this.targetSelector.a(2, new AttackEnemies(this, true, bedWarsTeam));
        this.goalSelector.a(6, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
    }

    public static LivingEntity spawn(VersionSupport vs, Location loc, ITeam bedWarsTeam, int speed, int health, int despawn) {
        WorldServer mcWorld = ((CraftWorld)loc.getWorld()).getHandle();
        IGolem customEnt = new IGolem(mcWorld, bedWarsTeam, vs);
        customEnt.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        ((CraftLivingEntity)customEnt.getBukkitEntity()).setRemoveWhenFarAway(false);
        customEnt.setCustomNameVisible(true);
        customEnt.getAttributeInstance(GenericAttributes.maxHealth).setValue(health);
        customEnt.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(speed);
        customEnt.setCustomName(Language.getDefaultLanguage().m(Messages.SHOP_UTILITY_NPC_IRON_GOLEM_NAME)
                .replace("{despawn}", String.valueOf(speed)
                        .replace("{health}", StringUtils.repeat(Language.getDefaultLanguage().m(Messages.FORMATTING_DESPAWNABLE_UTILITY_NPC_HEALTH)+" ", 10))
                        .replace("{TeamColor}", TeamColor.getChatColor(bedWarsTeam.getColor()).toString())));
        mcWorld.addEntity(customEnt, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return (LivingEntity) customEnt.getBukkitEntity();
    }
}
