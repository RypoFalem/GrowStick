package io.github.rypofalem.growstick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


public class GrowStickPlugin extends JavaPlugin implements Listener{
	Random rand;
	float baseMultiplier = .2f;
	boolean allowAdventure = false;
	List<String> blacklistedWorlds = new ArrayList<>();
	ArrayList<Material> doUpdateList;

	@Override
	public void onEnable(){
		Bukkit.getPluginManager().registerEvents(this, this);
		rand = new Random();
		loadConfig();
	}

	void loadConfig(){
		saveDefaultConfig();
		reloadConfig();
		if(getConfig() == null) return;
		doUpdateList = new ArrayList<>();

		if(getConfig().contains("whitelist")){
			List<String> whitelist = getConfig().getStringList("whitelist");
			if(whitelist != null){
				for(String matName : whitelist){
					try{
						Material mat = Material.valueOf(matName.toUpperCase());
						doUpdateList.add(mat);
					}catch(Exception e){
						getLogger().warning("Invalid Material name in GrowStick/config.yml: " + matName);
					}
				}
			}
		}

		blacklistedWorlds = getConfig().getStringList("worldBlacklist");
		baseMultiplier = (float) getConfig().getDouble("baseMultiplier", .2);
		allowAdventure = getConfig().getBoolean("allowAdventure", false);
	}

	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onRightClick(PlayerInteractEvent event){
		if(!event.getPlayer().hasPermission("growstick.use")
				|| event.getAction() != Action.RIGHT_CLICK_BLOCK
		  		|| (!allowAdventure && event.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
				|| blacklistedWorlds.contains(event.getPlayer().getWorld().getName())) return;

		ItemStack growstick = event.getPlayer().getEquipment().getItemInMainHand();
		if(growstick == null) return; 
		if(growstick.getType() != Material.STICK) return;
		if(event.getHand() == EquipmentSlot.OFF_HAND) event.setCancelled(true);
		Block clickedBlock = event.getClickedBlock();
		World world = clickedBlock.getWorld();

		int range = 1; //radius of growstick AoE
		int length= 2 * range + 1; //length of the square growstick AoE
		// since there might be a fractional chance to update but updates always happen in integer amounts,
		// separate the fractional chance and use that to decide if an extra update should be added
		float updateBase = length * length * baseMultiplier;
		int updates = (int) updateBase;
		if((updateBase - (int)(updateBase)) >= rand.nextFloat()){
			updates++;
		}

		//attempt to do block updates
		for(int i = 0; i<updates; i++){
			int x = rand.nextInt(length) - range + clickedBlock.getX();
			int y = clickedBlock.getY();
			int z = rand.nextInt(length) - range + clickedBlock.getZ();
			for(int yOffset = 2; yOffset >= -1; yOffset--){
				Block crop = world.getBlockAt(x, y + yOffset, z);
				Material cropType = crop.getType();
				if(!doUpdateList.contains(cropType)) continue;
				Location blockLoc = new Location(world, x + .5, y + yOffset + .99, z + .5);
				BlockUpdater.update(blockLoc);
				world.spawnParticle(Particle.ENCHANTMENT_TABLE, blockLoc, 1);
			}
		}

		//spawn water particles and play sound
		for(int xOffset = range * -1; xOffset <= range; xOffset++){
			for(int zOffset = range * -1; zOffset <= range; zOffset++){
				Location loc = clickedBlock.getLocation().clone().add(xOffset + .5, 1, zOffset + .5);
				world.spawnParticle(Particle.WATER_SPLASH, loc, 1);
			}
		}
		world.playSound(clickedBlock.getLocation(), Sound.BLOCK_WATER_AMBIENT, .1f, 1);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(args == null || args.length < 1 ) return false;
		switch(args[0]){
			case "reload" : loadConfig();
				sender.sendMessage("Growstick configuration reloaded.");
				break;
			default: return false;
		}
		return true;
	}
}