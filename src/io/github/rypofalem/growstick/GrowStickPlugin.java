package io.github.rypofalem.growstick;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.winthier.skills.*;

public class GrowStickPlugin extends JavaPlugin implements Listener{
	Random rand; 
	Skill harvest;
	boolean skillsEnabled = false;
	
	@Override
	public void onEnable(){
		Bukkit.getPluginManager().registerEvents(this, this);
		rand = new Random();
		skillsEnabled = Bukkit.getPluginManager().isPluginEnabled("Skills");
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onRightClick(PlayerInteractEvent event){
		if(!event.getPlayer().hasPermission("growstick.use")) return;
		if(event.getHand() == EquipmentSlot.OFF_HAND) return;
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		ItemStack growstick = event.getItem();
		if(growstick == null) return; 
		if(growstick.getType() != Material.STICK) return;
		Block clickedBlock = event.getClickedBlock();
		World world = clickedBlock.getWorld();
		world.playSound(clickedBlock.getLocation(), Sound.BLOCK_WATER_AMBIENT, .1f, 1);
		
		int range = getRange(event.getPlayer());
		int length= 2*range + 1;
		float updateBase = length * length* getMultiplier(event.getPlayer());
		int updates = (int) Math.max(1, updateBase/5);
		if(updateBase - (int)(updateBase) <= rand.nextFloat()){
			//TODO: optimize so this happens 1/x times without random number generation
			updates++; 
		}
		
		for(int i = 0; i<updates; i++){
			int x = rand.nextInt(length) - range + clickedBlock.getX();
			int y = clickedBlock.getY();
			int z = rand.nextInt(length) - range + clickedBlock.getZ();
			for(int yOffset = 2; yOffset >= -1; yOffset--){
				Block crop = world.getBlockAt(x, y + yOffset, z);
				if(crop.getType() == Material.PORTAL) continue; //don't make pigmen spawn
				if(crop.getType() != Material.AIR){ //any other block is safe to schedule an update for
					Location blockLoc = new Location(world, x + .5, y + yOffset + .99, z + .5);
					BlockUpdater.update(blockLoc);
					world.playEffect(blockLoc, Effect.FLYING_GLYPH, 1 );
				}
			}
		}
		
		for(int xOffset = range * -1; xOffset <= range; xOffset++){
			for(int zOffset = range * -1; zOffset <= range; zOffset++){
				world.playEffect(clickedBlock.getLocation().clone().add(xOffset + .5, 1, zOffset + .5), Effect.SPLASH, 1);
			}
		}
	}
	
	float getMultiplier(Player player){
		return 1 + getSkillLevel(player) * .005f;
	}
	
	int getRange(Player player){
		return Math.min(1 + (int)(getSkillLevel(player)/100), 4); 
	}
	
	int getSkillLevel(Player player){
		if(!skillsEnabled) return 0;
		Skills skills = Skills.getInstance();
		
		harvest = null;
		if(harvest == null){
			if(skills == null) return 0;
			for(Skill s : skills.getSkills()){
				if(s.getKey().equals("harvest")){
					harvest = s;
					break;
				}
			}
			if(harvest == null) return 0;
		}
		
		return skills.getScore().getSkillLevel(player.getUniqueId(), harvest);
	}
	
	static void print(String message){
		Bukkit.getServer().broadcastMessage(message);
	}
	
}
