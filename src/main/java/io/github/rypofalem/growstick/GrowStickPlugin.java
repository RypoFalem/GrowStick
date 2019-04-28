package io.github.rypofalem.growstick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


public final class GrowStickPlugin extends JavaPlugin implements Listener{
	private ItemStack growstickItem = new ItemStack(Material.STICK);
	private Random rand = new Random();
	private float baseMultiplier = .2f;
	private int range = 1;
	private boolean allowAdventure = false;
	private boolean debug = false;
	private List<String> blacklistedWorlds = new ArrayList<>();
	private List<Material> doUpdateList = new ArrayList<>();
	private CustomConfig itemConfig = null;

	public static GrowStickPlugin instance(){
		return (GrowStickPlugin)Bukkit.getPluginManager().getPlugin("GrowStick");
	}

	@Override
	public void onEnable(){
		Bukkit.getPluginManager().registerEvents(this, this);
		loadConfig();
	}

	private void loadConfig(){
		saveDefaultConfig();
		reloadConfig();

		if(getConfig().contains("whitelist")){
			List<String> whitelist = getConfig().getStringList("whitelist");
			if(whitelist != null){
				for(String matName : whitelist){
					try{
						Material mat = Material.valueOf(matName.toUpperCase());
						doUpdateList.add(mat);
					}catch(IllegalArgumentException exception){
						getLogger().log(
								Level.WARNING,
								String.format("%sInvalid Material name in GrowStick/config.yml: %s", ChatColor.RED, matName),
								exception);
					}
				}
			}
		}
		blacklistedWorlds = getConfig().getStringList("worldBlacklist");
		baseMultiplier = (float) getConfig().getDouble("baseMultiplier", .2);
		range = getConfig().getInt("range", 1);
		allowAdventure = getConfig().getBoolean("allowAdventure", false);
		debug = getConfig().getBoolean("debug", false);
		if(range > 10){
			getLogger().warning("Range is set too high! Setting it to 10");
			range = 10;
		} else if (range < 0){
			getLogger().warning("Range cannot be negative! Setting it to 0!");
			range = 0;
		}
		if(baseMultiplier < 0){
			getLogger().warning("baseMultiplier cannot be negative! Setting it to 0!");
			baseMultiplier = 0;
		} else if(baseMultiplier > 10){
			getLogger().warning("baseMultiplyer is set too high! Setting it to 10");
			baseMultiplier = 10;
		}
		growstickItem = getItemConfig().getConfig().getItemStack("growstick", growstickItem);
	}

	private CustomConfig getItemConfig(){
		if(itemConfig == null){
			itemConfig = new CustomConfig(getDataFolder() + "/" + "item.yml", this);
		}
		return itemConfig;
	}


	//Use the growstick to water crops
	@EventHandler (priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onRightClick(PlayerInteractEvent event){
		if(!event.getPlayer().hasPermission("growstick.use")
				|| event.getAction() != Action.RIGHT_CLICK_BLOCK
		  		|| (!allowAdventure && event.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
				|| blacklistedWorlds.contains(event.getPlayer().getWorld().getName())) return;

		ItemStack growstick = event.getPlayer().getEquipment().getItemInMainHand();
		if(growstick == null) return; 
		if(!isGrowstickItem(growstick)) return;
		event.setCancelled(true);
		Block clickedBlock = event.getClickedBlock();
		World world = clickedBlock.getWorld();

		int length= 2 * range + 1; //length of the square growstick AoE
		// since there might be a fractional chance to update but updates always happen in integer amounts,
		// separate the fractional chance and use that to decide if an extra update should be added
		float updateBase = length * length * baseMultiplier;
		int updates = (int) updateBase;
		if((updateBase - (int)(updateBase)) >= rand.nextFloat()){
			updates++;
		}

		//do block updates
		for(int i = 0; i<updates; i++){
			int x = rand.nextInt(length) - range + clickedBlock.getX();
			int y = clickedBlock.getY();
			int z = rand.nextInt(length) - range + clickedBlock.getZ();
			for(int yOffset = 2; yOffset >= -1; yOffset--){
				Block crop = world.getBlockAt(x, y + yOffset, z);
				Material cropType = crop.getType();
				if(!doUpdateList.contains(cropType)) continue;
				Location blockLoc = new Location(world, x + .5, y + yOffset + .99, z + .5);
				BlockUpdater.update(blockLoc, rand);
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

	//Don't allow crafting if growstick is an input item (unless growstick is also output item)
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCraft(PrepareItemCraftEvent event){
		if(!growstickItem.hasItemMeta() //growstick is a normal everyday item, allow crafting
				|| isGrowstickItem(event.getInventory().getResult()) //growstick is the result, allow crafting
				) return;
		for(ItemStack item : event.getInventory().getMatrix()){
			if(isGrowstickItem(item)){
				event.getInventory().setResult(new ItemStack(Material.AIR));
				break;
			}
		}
	}

	//Don't allow anvil use if growstick is an input item
	@EventHandler
	public void onAnvilUse(PrepareAnvilEvent event){
		if(!growstickItem.hasItemMeta()) return; //growstick is a normal everyday item, allow anvil use
		for(ItemStack item : event.getInventory().getStorageContents()){
			if(isGrowstickItem(item)){
				event.setResult(new ItemStack(Material.AIR));
				break;
			}
		}
	}

	//Don't allow encahnting growsticks
	@EventHandler
	public void onEnchant(PrepareItemEnchantEvent event){
		if(!growstickItem.hasItemMeta()) return; //growstick is a normal everyday item, allow enchanting)
		for(ItemStack item : event.getInventory().getStorageContents()){
			if(isGrowstickItem(item)){
				event.setCancelled(true);
				break;
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(args == null || args.length < 1 ) return false;
		switch(args[0].toLowerCase()){
			case "reload" :
				loadConfig();
				sender.sendMessage("Growstick configuration reloaded.");
				break;
			case "give" :
				giveCommand(sender, args);
				break;
			case "set" :
				setCommand(sender, args);
				break;
			default: return false;
		}
		return true;
	}

	//handle /growstick give
	private void giveCommand(CommandSender sender, String[] args){
		Player recipient = null;
		if(args.length > 1){
			recipient = Bukkit.getPlayer(args[1]);
			if(recipient == null){
				sender.sendMessage("Invalid recipient: " + args[1]);
				return;
			}
		}else if(sender instanceof  Player){
			recipient = (Player) sender;
		} else{
			sender.sendMessage("You must specify a player if you are not one!");
			return;
		}
		giveGrowStick(recipient);
		sender.sendMessage("Growstick given to " + recipient.getName() + "!");
	}

	//handle /growstick set
	private void setCommand(CommandSender sender, String[] args){
		if(!(sender instanceof Player)){
			sender.sendMessage("Only players can use this command!");
			return;
		}
		Player player = (Player)sender;
		ItemStack item = player.getEquipment().getItemInMainHand();
		if(item == null || item.getType() == Material.AIR){
			sender.sendMessage("You need to hold the new growstick in your main hand!");
			return;
		}
		setGrowStick(item);
		sender.sendMessage("The item in your hand is the new growstick!");
	}

	//Give a player a growstick
	public void giveGrowStick(Player player){
		if(player == null) throw new IllegalArgumentException("Player cannot be null!");
		if(!player.isOnline()) throw new IllegalArgumentException("Player must be online!");
		Item item = player.getWorld().dropItem(player.getLocation(), getGrowstickItem());
		item.setPickupDelay(0);
	}

	//Set growstick to a given ItemStack and save that information to item.yml
	public void setGrowStick(ItemStack item){
		if(item == null) throw new IllegalArgumentException("Item cannot be null!");
		item = item.clone();
		item.setAmount(1);
		growstickItem = item;
		itemConfig.getConfig().set("growstick", growstickItem);
		itemConfig.saveConfig();
	}

	public ItemStack getGrowstickItem(){
		return growstickItem.clone();
	}

	public boolean isGrowstickItem(ItemStack item){
		return item != null && item.isSimilar(growstickItem);
	}

	public void debug(String message){
		if(!debug) return;
		getLogger().log(Level.INFO, message);
		Bukkit.getOnlinePlayers().forEach((player) -> player.sendMessage("[Growstick]: " + message));
	}

}