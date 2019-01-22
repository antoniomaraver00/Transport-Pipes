package de.robotricker.transportpipes.listener;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.InventoryHolder;

import javax.inject.Inject;

import de.robotricker.transportpipes.container.BlockContainer;
import de.robotricker.transportpipes.container.BrewingStandContainer;
import de.robotricker.transportpipes.container.FurnaceContainer;
import de.robotricker.transportpipes.container.SimpleInventoryContainer;
import de.robotricker.transportpipes.container.TPContainer;
import de.robotricker.transportpipes.duct.Duct;
import de.robotricker.transportpipes.duct.DuctRegister;
import de.robotricker.transportpipes.duct.manager.DuctManager;
import de.robotricker.transportpipes.duct.manager.GlobalDuctManager;
import de.robotricker.transportpipes.duct.manager.PipeManager;
import de.robotricker.transportpipes.duct.pipe.Pipe;
import de.robotricker.transportpipes.location.BlockLocation;
import de.robotricker.transportpipes.location.TPDirection;

import static de.robotricker.transportpipes.utils.WorldUtils.isIdContainerBlock;

public class TPContainerListener implements Listener {

    @Inject
    private DuctRegister ductRegister;

    @Inject
    private GlobalDuctManager globalDuctManager;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isIdContainerBlock(e.getBlockPlaced().getTypeId())) {
            updateContainerBlock(e.getBlock(), true, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (isIdContainerBlock(e.getBlock().getTypeId())) {
            updateContainerBlock(e.getBlock(), false, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (isIdContainerBlock(b.getTypeId())) {
                updateContainerBlock(b, false, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (isIdContainerBlock(b.getTypeId())) {
                updateContainerBlock(b, false, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        handleChunkLoadSync(e.getChunk(), false);
    }

    public void updateContainerBlock(Block block, boolean add, boolean updateNeighborPipes) {
        PipeManager pipeManager = (PipeManager) (DuctManager<? extends Duct>) ductRegister.baseDuctTypeOf("pipe").getDuctManager();

        BlockLocation blockLoc = new BlockLocation(block.getLocation());
        if (add) {
            if (pipeManager.getContainerAtLoc(block.getLocation()) == null) {
                TPContainer container = createContainerFromBlock(block);
                pipeManager.getContainers(block.getWorld()).put(blockLoc, container);

                // only update the neighbor pipes if this updateContainerBlock method call is because of a chunk load that was not issued inside the onEnable method
                if (updateNeighborPipes) {
                    for (TPDirection dir : TPDirection.values()) {
                        Duct duct = globalDuctManager.getDuctAtLoc(block.getWorld(), blockLoc.getNeighbor(dir));
                        if (duct instanceof Pipe) {
                            globalDuctManager.updateDuctConnections(duct);
                            globalDuctManager.updateDuctInRenderSystems(duct, true);
                        }
                    }
                }
            }
        } else {
            TPContainer container = pipeManager.getContainerAtLoc(block.getLocation());
            if (container != null) {
                pipeManager.getContainers(block.getWorld()).remove(blockLoc);

                // only update the neighbor pipes if this updateContainerBlock method call is because of a chunk load that was not issued inside the onEnable method
                if (updateNeighborPipes) {
                    for (TPDirection dir : TPDirection.values()) {
                        Duct duct = globalDuctManager.getDuctAtLoc(block.getWorld(), blockLoc.getNeighbor(dir));
                        if (duct instanceof Pipe) {
                            globalDuctManager.updateDuctConnections(duct);
                            globalDuctManager.updateDuctInRenderSystems(duct, true);
                        }
                    }
                }
            }
        }
    }

    public TPContainer createContainerFromBlock(Block block) {
        if (block.getState() instanceof Furnace) {
            return new FurnaceContainer(block);
        } else if (block.getState() instanceof BrewingStand) {
            return new BrewingStandContainer(block);
        } else if (block.getState() instanceof InventoryHolder) {
            return new SimpleInventoryContainer(block);
        }
        return null;
    }

    public void handleChunkLoadSync(Chunk loadedChunk, boolean onServerStart) {
        PipeManager pipeManager = (PipeManager) (DuctManager<? extends Duct>) ductRegister.baseDuctTypeOf("pipe").getDuctManager();

        if (loadedChunk.getTileEntities() != null) {
            for (BlockState bs : loadedChunk.getTileEntities()) {
                if (isIdContainerBlock(bs.getTypeId())) {

                    //automatically ignores this block if it is already registered as container block
                    updateContainerBlock(bs.getBlock(), true, !onServerStart);

                    //if this block is already registered, update the block, because the blockState object changes after a chunk unload and load
                    TPContainer container = pipeManager.getContainerAtLoc(bs.getLocation());
                    if (container instanceof BlockContainer) {
                        ((BlockContainer) container).updateBlock();
                    }
                }
            }
        }
    }


}