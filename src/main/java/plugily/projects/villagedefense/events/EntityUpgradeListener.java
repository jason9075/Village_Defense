package plugily.projects.villagedefense.events;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.arena.ArenaRegistry;
import plugily.projects.villagedefense.handlers.upgrade.EntityUpgradeMenu;
import plugily.projects.villagedefense.utils.Utils;

import java.util.ArrayList;

/**
 * @author Plajer
 * <p>
 * Created at 18.06.2019
 */
public class EntityUpgradeListener implements Listener {

  private final EntityUpgradeMenu upgradeMenu;

  public EntityUpgradeListener(EntityUpgradeMenu upgradeMenu) {
    this.upgradeMenu = upgradeMenu;
    upgradeMenu.getPlugin().getServer().getPluginManager().registerEvents(this, upgradeMenu.getPlugin());
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent e) {
    if (!(e.getDamager() instanceof LivingEntity) || !(e.getDamager() instanceof IronGolem)) {
      return;
    }
    switch (e.getDamager().getType()) {
      case IRON_GOLEM:
        for (Arena arena : ArenaRegistry.getArenas()) {
          if (!arena.getIronGolems().contains(e.getDamager())) {
            continue;
          }
          e.setDamage(e.getDamage() + upgradeMenu.getTier(e.getDamager(), upgradeMenu.getUpgrade("Damage")) * 2);
        }
        break;
      case WOLF:
        for (Arena arena : ArenaRegistry.getArenas()) {
          if (!arena.getWolves().contains(e.getDamager())) {
            continue;
          }
          int tier = upgradeMenu.getTier(e.getDamager(), upgradeMenu.getUpgrade("Swarm-Awareness"));
          if (tier == 0) {
            return;
          }
          double multiplier = 1;
          for (Entity en : Utils.getNearbyEntities(e.getDamager().getLocation(), 3)) {
            if (en instanceof Wolf) {
              multiplier += tier * 0.2;
            }
          }
          e.setDamage(e.getDamage() * multiplier);
        }
        break;
      default:
        break;
    }
  }

  @EventHandler
  public void onFinalDefense(EntityDeathEvent e) {
    if (!(e.getEntity() instanceof IronGolem)) {
      return;
    }
    for (Arena arena : ArenaRegistry.getArenas()) {
      if (!arena.getIronGolems().contains(e.getEntity())) {
        continue;
      }
      int tier = upgradeMenu.getTier(e.getEntity(), upgradeMenu.getUpgrade("Final-Defense"));
      if (tier == 0) {
        return;
      }
      e.getEntity().getLocation().getWorld().spawnParticle(Particle.EXPLOSION_HUGE, e.getEntity().getLocation(), 5);
      for (Entity en : Utils.getNearbyEntities(e.getEntity().getLocation(), tier * 5)) {
        if (en instanceof Zombie) {
          ((Zombie) en).damage(10000.0, e.getEntity());
        }
      }
      for (Zombie zombie : new ArrayList<>(arena.getZombies())) {
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 5, 0));
        zombie.damage(0.5, e.getEntity());
      }
    }
  }

  @EventHandler
  public void onEntityClick(PlayerInteractEntityEvent e) {
    if (ArenaRegistry.getArena(e.getPlayer()) == null || upgradeMenu.getPlugin().getUserManager().getUser(e.getPlayer()).isSpectator()
        || (e.getRightClicked().getType() != EntityType.IRON_GOLEM && e.getRightClicked().getType() != EntityType.WOLF) || e.getRightClicked().getCustomName() == null) {
      return;
    }
    if (e.getHand() == EquipmentSlot.OFF_HAND || !e.getPlayer().isSneaking()) {
      return;
    }
    upgradeMenu.openUpgradeMenu((LivingEntity) e.getRightClicked(), e.getPlayer());
  }

}
