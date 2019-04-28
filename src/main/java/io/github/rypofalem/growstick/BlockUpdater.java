package io.github.rypofalem.growstick;

import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.IBlockData;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;

import java.util.Random;


public abstract class BlockUpdater {
	//schedules a "random tick" block update
	//see https://minecraft.gamepedia.com/Tick#Block_tick
	static void update(Location loc, Random random){
		net.minecraft.server.v1_14_R1.World mcWorld = ((org.bukkit.craftbukkit.v1_14_R1.CraftWorld) loc.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		org.bukkit.block.Block bukkitBlock = loc.getBlock();
		if(!(bukkitBlock.getBlockData() instanceof CraftBlockData)) return;
		IBlockData blockData = ((CraftBlockData)bukkitBlock.getBlockData()).getState();
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());
		block.tick(blockData, mcWorld, blockPos, random);

	}
}
