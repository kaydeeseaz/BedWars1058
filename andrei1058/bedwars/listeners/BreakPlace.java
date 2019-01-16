package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.Main;
import com.andrei1058.bedwars.api.*;
import com.andrei1058.bedwars.api.events.BedBreakEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.BedWarsTeam;
import com.andrei1058.bedwars.arena.OreGenerator;
import com.andrei1058.bedwars.commands.bedwars.subcmds.sensitive.setup.AutoCreateTeams;
import com.andrei1058.bedwars.configuration.ConfigPath;
import com.andrei1058.bedwars.language.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.andrei1058.bedwars.Main.*;
import static com.andrei1058.bedwars.language.Language.getMsg;

public class BreakPlace implements Listener {

    private static List<Player> buildSession = new ArrayList<>();

    @EventHandler
    public void onIceMelt(BlockFadeEvent e) {
        if (Main.getServerType() != ServerType.BUNGEE) {
            if (e.getBlock().getLocation().getWorld().getName().equalsIgnoreCase(Main.getLobbyWorld())) {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getBlock().getType() == Material.ICE) {
            if (Arena.getArenaByName(e.getBlock().getWorld().getName()) != null) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCactus(BlockPhysicsEvent e) {
        if (e.getBlock().getType() == Material.CACTUS) {
            if (Arena.getArenaByName(e.getBlock().getWorld().getName()) != null) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {

        //Prevent player from placing during the removal from the arena
        Arena arena = Arena.getArenaByName(e.getBlock().getWorld().getName());
        if (arena != null) {
            if (arena.getStatus() != GameState.playing) {
                e.setCancelled(true);
                return;
            }
        }
        Player p = e.getPlayer();
        Arena a = Arena.getArenaByPlayer(p);
        if (a != null) {
            if (a.isSpectator(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getRespawn().containsKey(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getStatus() != GameState.playing) {
                e.setCancelled(true);
                return;
            }
            if (e.getBlockPlaced().getLocation().getBlockY() >= a.getCm().getInt(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y)) {
                e.setCancelled(true);
                return;
            }
            try {
                for (BedWarsTeam t : a.getTeams()) {
                    if (t.getSpawn().distance(e.getBlockPlaced().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_SPAWN_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                        return;
                    }
                    if (t.getShop().distance(e.getBlockPlaced().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_SHOP_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                        return;
                    }
                    if (t.getTeamUpgrades().distance(e.getBlockPlaced().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_UPGRADES_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                        return;
                    }
                }
                for (OreGenerator o : OreGenerator.getGenerators()) {
                    if (o.getLocation().distance(e.getBlock().getLocation()) <= 1) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                        return;
                    }
                }
            } catch (Exception ex) {
            }
            if (e.getBlock().getType() == Material.TNT) {
                e.setCancelled(true);
                TNTPrimed tnt = e.getBlock().getLocation().getWorld().spawn(e.getBlock().getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(45);
                nms.setSource(tnt, p);
                for (ItemStack i : p.getInventory().getContents()) {
                    if (i == null) continue;
                    if (i.getType() == null) continue;
                    if (i.getType() == Material.AIR) continue;
                    if (i.getType() == Material.TNT) {
                        if (i.getAmount() <= 1) {
                            p.getInventory().remove(i);
                            p.updateInventory();
                            return;
                        } else {
                            i.setAmount(i.getAmount() - 1);
                            p.updateInventory();
                            return;
                        }
                    }
                }
                return;
            }

            a.getPlaced().add(e.getBlock());
            return;
        }
        if (Main.getServerType() != ServerType.BUNGEE) {
            if (e.getBlock().getLocation().getWorld().getName().equalsIgnoreCase(Main.getLobbyWorld())) {
                if (!isBuildSession(p)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (Main.getServerType() != ServerType.BUNGEE) {
            if (e.getBlock().getLocation().getWorld().getName().equalsIgnoreCase(Main.getLobbyWorld())) {
                if (!isBuildSession(p)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        Arena a = Arena.getArenaByPlayer(p);
        if (a != null) {
            if (!a.isPlayer(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getRespawn().containsKey(p)) {
                e.setCancelled(true);
                return;
            }
            if (a.getStatus() != GameState.playing) {
                e.setCancelled(true);
                return;
            }

            if (nms.isBed(e.getBlock().getType())) {
                for (BedWarsTeam t : a.getTeams()) {
                    for (int x = e.getBlock().getX() - 2; x < e.getBlock().getX() + 2; x++) {
                        for (int y = e.getBlock().getY() - 2; y < e.getBlock().getY() + 2; y++) {
                            for (int z = e.getBlock().getZ() - 2; z < e.getBlock().getZ() + 2; z++) {
                                if (t.getBed().getBlockX() == x && t.getBed().getBlockY() == y && t.getBed().getBlockZ() == z) {
                                    if (!t.isBedDestroyed()) {
                                        if (t.isMember(p)) {
                                            p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_OWN_BED));
                                            e.setCancelled(true);
                                            return;
                                        } else {
                                            e.setCancelled(false);
                                            t.setBedDestroyed(true);
                                            a.addPlayerBedDestroyed(p);
                                            Bukkit.getPluginManager().callEvent(new BedBreakEvent(e.getPlayer(), a.getTeam(p), t));
                                            for (Player on : a.getWorld().getPlayers()) {
                                                if (t.isMember(on)) {
                                                    on.sendMessage(getMsg(on, Messages.INTERACT_BED_DESTROY_CHAT_ANNOUNCEMENT_TO_VICTIM).replace("{TeamColor}", TeamColor.getChatColor(t.getColor()).toString()).replace("{TeamName}", t.getName())
                                                            .replace("{PlayerColor}", TeamColor.getChatColor(a.getTeam(p).getColor()).toString()).replace("{PlayerName}", p.getName()));
                                                    nms.sendTitle(on, getMsg(on, Messages.INTERACT_BED_DESTROY_TITLE_ANNOUNCEMENT), getMsg(on, Messages.INTERACT_BED_DESTROY_SUBTITLE_ANNOUNCEMENT), 0, 25, 0);
                                                    on.playSound(on.getLocation(), nms.bedDestroy(), 2f, 2f);
                                                } else {
                                                    on.sendMessage(getMsg(on, Messages.INTERACT_BED_DESTROY_CHAT_ANNOUNCEMENT).replace("{TeamColor}", TeamColor.getChatColor(t.getColor()).toString()).replace("{TeamName}", t.getName())
                                                            .replace("{PlayerColor}", TeamColor.getChatColor(a.getTeam(p).getColor()).toString()).replace("{PlayerName}", p.getName()));
                                                    on.playSound(on.getLocation(), nms.bedDestroy(), 1f, 1f);
                                                }
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            try {
                for (BedWarsTeam t : a.getTeams()) {
                    if (t.getSpawn().distance(e.getBlock().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_SPAWN_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                        return;
                    }
                    if (t.getShop().distance(e.getBlock().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_SHOP_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                        return;
                    }
                    if (t.getTeamUpgrades().distance(e.getBlock().getLocation()) <= a.getCm().getInt(ConfigPath.ARENA_UPGRADES_PROTECTION)) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                        return;
                    }
                }
                for (OreGenerator o : OreGenerator.getGenerators()) {
                    if (o.getLocation().distance(e.getBlock().getLocation()) <= 1) {
                        e.setCancelled(true);
                        p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                        return;
                    }
                }
            } catch (Exception ex) {
            }

            if (!a.getCm().getBoolean(ConfigPath.ARENA_ALLOW_MAP_BREAK)) {
                if (!a.getPlaced().contains(e.getBlock())) {
                    p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_BREAK_BLOCK));
                    e.setCancelled(true);
                    return;
                }
                a.getPlaced().remove(e.getBlock());
            }
        }
    }

    /**
     * update game signs
     */
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (e.getLine(0).equalsIgnoreCase("[" + mainCmd + "]")) {
            File dir = new File("plugins/" + plugin.getName() + "/Arenas");
            boolean exists = false;
            if (dir.exists()) {
                for (File f : dir.listFiles()) {
                    if (f.isFile()) {
                        if (f.getName().contains(".yml")) {
                            if (e.getLine(1).equalsIgnoreCase(f.getName().replace(".yml", ""))) {
                                exists = true;
                            }
                        }
                    }
                }
                List<String> s;
                if (signs.getYml().get("locations") == null) {
                    s = new ArrayList<>();
                } else {
                    s = new ArrayList<>(signs.getYml().getStringList("locations"));
                }
                if (exists) {
                    s.add(e.getLine(1) + "," + signs.getConfigLoc(e.getBlock().getLocation()));
                    signs.set("locations", s);
                }
                Arena a = Arena.getArenaByName(e.getLine(1));
                if (a != null) {
                    p.sendMessage("§a▪ §7Sign saved for arena: " + e.getLine(1));
                    a.addSign(e.getBlock().getLocation());
                    Sign b = (Sign) e.getBlock().getState();
                    int line = 0;
                    for (String string : Main.signs.l("format")) {
                        e.setLine(line, string.replace("[on]", String.valueOf(a.getPlayers().size())).replace("[max]",
                                String.valueOf(a.getMaxPlayers())).replace("[arena]", a.getDisplayName()).replace("[status]", a.getDisplayStatus(Main.lang)));
                        line++;
                    }
                    b.update(true);
                }
            } else {
                p.sendMessage("§c▪ §7You didn't set any arena yet!");
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (Main.getServerType() != ServerType.BUNGEE) {
            if (e.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase(Main.getLobbyWorld())) {
                if (!isBuildSession(e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }
        Arena a = Arena.getArenaByPlayer(e.getPlayer());
        if (a != null) {
            if (a.isSpectator(e.getPlayer()) || a.getStatus() != GameState.playing || a.getRespawn().containsKey(e.getPlayer()))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (Main.getServerType() != ServerType.BUNGEE) {
            if (e.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase(Main.getLobbyWorld())) {
                if (!isBuildSession(e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }
        Arena a = Arena.getArenaByPlayer(e.getPlayer());
        if (a != null) {
            if (a.isSpectator(e.getPlayer()) || a.getStatus() != GameState.playing || a.getRespawn().containsKey(e.getPlayer())) {
                e.setCancelled(true);
            } else {
                try {
                    Player p = e.getPlayer();
                    for (BedWarsTeam t : a.getTeams()) {
                        if (t.getSpawn().distance(e.getBlockClicked().getLocation()) <= a.getCm().getInt("spawnProtection")) {
                            e.setCancelled(true);
                            p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                            return;
                        }
                        if (t.getShop().distance(e.getBlockClicked().getLocation()) <= a.getCm().getInt("shopProtection")) {
                            e.setCancelled(true);
                            p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                            return;
                        }
                        if (t.getTeamUpgrades().distance(e.getBlockClicked().getLocation()) <= a.getCm().getInt("upgradesProtection")) {
                            e.setCancelled(true);
                            p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                            return;
                        }
                    }
                    for (OreGenerator o : OreGenerator.getGenerators()) {
                        if (o.getLocation().distance(e.getBlockClicked().getLocation()) <= 1) {
                            e.setCancelled(true);
                            p.sendMessage(getMsg(p, Messages.INTERACT_CANNOT_PLACE_BLOCK));
                            return;
                        }
                    }
                    /** Remove empty bucket */
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        nms.minusAmount(e.getPlayer(), nms.getItemInHand(e.getPlayer()), 1);
                    }, 3L);
                } catch (Exception ex) {
                }
            }
        }
    }

    @EventHandler
    public void onBlow(EntityExplodeEvent e) {
        if (e.blockList().isEmpty()) return;
        Arena a = Arena.getArenaByName(e.blockList().get(0).getWorld().getName());
        if (a != null) {
            if (e.getEntity().hasMetadata("bw1058") || a.getNextEvent() != NextEvent.GAME_END) {
                List<Block> destroyed = e.blockList();
                Iterator<Block> it = new ArrayList<>(destroyed).iterator();
                while (it.hasNext()) {
                    Block block = it.next();
                    if (!a.getPlaced().contains(block)) {
                        e.blockList().remove(block);
                    } else if (AutoCreateTeams.is13Higher()) {
                        if (block.getType().toString().contains("_GLASS")) e.blockList().remove(block);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockCanBuildEvent(BlockCanBuildEvent e) {
        if (e.isBuildable()) return;
        Arena a = Arena.getArenaByName(e.getBlock().getWorld().getName());
        if (a != null) {
            boolean bed = false;
            for (BedWarsTeam t : a.getTeams()) {
                for (int x = e.getBlock().getX() - 1; x < e.getBlock().getX() + 1; x++) {
                    for (int z = e.getBlock().getZ() - 1; z < e.getBlock().getZ() + 1; z++) {

                        //Check bed block
                        if (t.getBed().getBlockX() == x && t.getBed().getBlockY() == e.getBlock().getY() && t.getBed().getBlockZ() == z) {
                            e.setBuildable(false);
                            bed = true;
                            break;
                        }
                    }
                }
                //Check bed hologram
                if (t.getBed().getBlockX() == e.getBlock().getX() && t.getBed().getBlockY() + 1 == e.getBlock().getY() && t.getBed().getBlockZ() == e.getBlock().getZ()) {
                    if (!bed) {
                        e.setBuildable(true);
                        break;
                    }
                }
            }
            if (bed) return;
            List<Object> players = Arrays.asList(e.getBlock().getWorld().getNearbyEntities(e.getBlock().getLocation(), 1, 1, 1).stream().filter(ee -> ee.getType() == EntityType.PLAYER).toArray());
            for (Object o : players) {
                Player p = (Player) o;
                if (a.isSpectator(p)) {
                    if (e.getBlock().getType() == Material.AIR) e.setBuildable(true);
                    return;
                }
            }
        }
    }

    public static boolean isBuildSession(Player p) {
        return buildSession.contains(p);
    }

    public static void addBuildSession(Player p) {
        buildSession.add(p);
    }

    public static void removeBuildSession(Player p) {
        buildSession.remove(p);
    }

    public static List<Player> getBuildSession() {
        return buildSession;
    }
}
