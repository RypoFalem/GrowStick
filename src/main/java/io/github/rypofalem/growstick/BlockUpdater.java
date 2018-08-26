package io.github.rypofalem.growstick;

import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.IBlockData;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;

import java.util.Random;


public abstract class BlockUpdater {
	private static Random random = new Random();

	//schedules a "random tick" block update
	//see https://minecraft.gamepedia.com/Tick#Block_tick
	static void update(Location loc){
		net.minecraft.server.v1_13_R2.World mcWorld = ((org.bukkit.craftbukkit.v1_13_R2.CraftWorld) loc.getWorld()).getHandle();
		BlockPosition blockPos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		org.bukkit.block.Block bukkitBlock = loc.getBlock();
		if(!(bukkitBlock.getBlockData() instanceof CraftBlockData)) return;
		IBlockData blockData = ((CraftBlockData)bukkitBlock.getBlockData()).getState();
		Block block = CraftMagicNumbers.getBlock(bukkitBlock.getType());

		block.a(blockData, mcWorld, blockPos, random);
	}
}
