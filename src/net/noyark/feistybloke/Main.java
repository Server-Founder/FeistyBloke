package net.noyark.feistybloke;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.level.particle.WaterDripParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.Utils;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * _____                                  _____
 * |  __ \                                / ____|                              _
 * | |  | | _ __  ___   __ _  _ __ ___   | (___    ___  _ __ __   __ ___  _ __(_)
 * | |  | || '__|/ _ \ / _` || '_ ` _ \   \___ \  / _ \| '__|\ \ / // _ \| '__|
 * | |__| || |  |  __/| (_| || | | | | |  ____) ||  __/| |    \ V /|  __/| |   _
 * |_____/ |_|   \___| \__,_||_| |_| |_| |_____/  \___||_|     \_/  \___||_|  (_)
 * ______     _       _           ____   _         _
 * |  ____|   (_)     | |         |  _ \ | |       | |
 * ______  | |__  ___  _  ___ | |_  _   _ | |_) || |  ___  | | __ ___
 * |______| |  __|/ _ \| |/ __|| __|| | | ||  _ < | | / _ \ | |/ // _ \
 * | |  |  __/| |\__ \| |_ | |_| || |_) || || (_) ||   <|  __/
 * |_|   \___||_||___/ \__| \__, ||____/ |_| \___/ |_|\_\\___|
 * __/ |
 * |___/
 * <p>
 * FeistyBloke @ DreamServer Server-Founder Group
 *
 * @author zzz
 */
public class Main extends PluginBase implements Listener {

    private static double jumping_power = 2.1F;
    private static int food_level_reduce = 0;
    private static Item feisty_item = Item.get(Item.DIRT);
    private static boolean keep_movement_check = true;
    private static int cooldown = 1000;

    private static final Map<Long, Long> players = new WeakHashMap<>();

    @Override
    public void onEnable() {

        this.getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        Config config = getConfig();
        jumping_power = config.getDouble("settings.charge-power", 2.1F);
        food_level_reduce = config.getInt("settings.food-level-reduce", 0);
        String fi = config.getString("settings.feisty-item", "3");
        String[] sa = fi.split(":");
        if (sa.length == 1) {
            feisty_item = Item.get(Integer.parseInt(sa[0]));
        } else if (sa.length == 2) {
            feisty_item = Item.get(Integer.parseInt(sa[0]), Integer.parseInt(sa[1]));
        } else if (sa.length == 3) {
            feisty_item = Item.get(Integer.parseInt(sa[0]), Integer.parseInt(sa[1]), Integer.parseInt(sa[2]));
        } else if (sa.length == 4) {
            feisty_item = Item.get(Integer.parseInt(sa[0]), Integer.parseInt(sa[1]), Integer.parseInt(sa[2]), Utils.parseHexBinary(sa[3]));
        } else {
            this.getLogger().error("无法解析feisty-item的格式,已默认为3:0");
        }
        keep_movement_check = config.getBoolean("settings.keep-movement-check",true);
        cooldown = config.getInt("settings.cooldown",1000);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void feistyLunch(PlayerInteractEvent event) {
        if (event.getPlayer().getInventory().getItemInHand().equals(feisty_item,true,false)) {
            if (event.getPlayer().isFoodEnabled() && event.getPlayer().getFoodData().getLevel() >= food_level_reduce) {
                if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
                    if (event.getPlayer().hasPermission("feistybloke.charge") && System.currentTimeMillis() > players.getOrDefault(event.getPlayer().getId(),0L)) {
                        double f = jumping_power;
                        Player p = event.getPlayer();
                        double yaw = p.getYaw();
                        double pitch = p.getPitch();

                        p.setMotion(new Vector3(-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f, -Math.sin(Math.toRadians(pitch)) * f * f, Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f));
                        p.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_SPIN_ATTACK, true);

                        p.resetFallDistance();
                        if (keep_movement_check) {
                            p.setCheckMovement(false);
                        }
                        //p.addEffect(Effect.getEffect(Effect.LEVITATION).setDuration(20));
                        if (food_level_reduce > 0) {
                            p.getFoodData().setLevel(p.getFoodData().getLevel() - food_level_reduce);
                        }
                        for (int i = 0; i < 5; i++) {
                            p.getLevel().addParticle(new SmokeParticle(p.add(Math.cos(i * 60 * 3.14F / 180), -0.2F, Math.sin(i * 60 * 3.14F / 180))));
                        }
                        if (cooldown > 0) {
                            players.put(p.getId(), System.currentTimeMillis() + cooldown);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void feistyDamageBlock(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && players.remove(event.getEntity().getId()) != null) {
            event.setCancelled();
            event.getEntity().resetFallDistance();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void feistyLand(PlayerMoveEvent event) {
        if (!event.getPlayer().isOnGround()) {
            if (players.containsKey(event.getPlayer().getId())) {
                event.getPlayer().getLevel().addParticle(new WaterDripParticle(event.getPlayer()));
            }
        } else {
            players.remove(event.getPlayer().getId());
            if(keep_movement_check){
                event.getPlayer().setCheckMovement(true);
            }
            event.getPlayer().setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_SPIN_ATTACK, false);
        }
    }

}
