package io.github.rypofalem.growstick;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_11_R1.util.CraftMagicNumbers;

import net.minecraft.server.v1_11_R1.BlockPosition;


public abstract class BlockUpdater {
	
	//schedules a "random tick" block update
	//see https://minecraft.gamepedia.com/Tick#Block_tick
	static void update(Location loc){
		net.minecraft.server.v1_11_R1.World mcWorld = ((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) loc.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		mcWorld.a(blockPos, CraftMagicNumbers.getBlock(loc.getBlock()), 1);
	}

}
