package de.robotricker.transportpipes.pipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.Tag;

import de.robotricker.transportpipes.TransportPipes;
import de.robotricker.transportpipes.pipeitems.ItemData;
import de.robotricker.transportpipes.pipeitems.PipeItem;
import de.robotricker.transportpipes.pipes.interfaces.Clickable;
import de.robotricker.transportpipes.pipeutils.PipeDirection;

public class GoldenPipe extends Pipe implements Clickable {

	//1st dimension: output dirs in order of PipeDirection.values() | 2nd dimension: output items in this direction
	private ItemData[][] outputItems = new ItemData[6][8];
	private boolean ignoreNBT = false;

	public GoldenPipe(Location blockLoc) {
		super(blockLoc);
	}

	@Override
	public PipeDirection itemArrivedAtMiddle(PipeItem item, PipeDirection before, List<PipeDirection> dirs) {

		ItemData itemMAD = new ItemData(item.getItem());

		List<PipeDirection> possibleDirections = getPossibleDirectionsForItem(itemMAD, before);

		return possibleDirections.get(new Random().nextInt(possibleDirections.size()));

	}

	public List<PipeDirection> getPossibleDirectionsForItem(ItemData itemData, PipeDirection before) {
		//all directions in which is an other pipe or inventory-block
		List<PipeDirection> connectionDirections = getAllConnections();

		//the possible directions in which the item could go
		List<PipeDirection> possibleDirections = new ArrayList<>();

		for (int line = 0; line < 6; line++) {
			PipeDirection dir = PipeDirection.fromID(line);
			//ignore the direction in which is no pipe or inv-block
			if (!connectionDirections.contains(dir)) {
				continue;
			}
			for (int i = 0; i < 8; i++) {
				if (ignoreNBT) {
					ItemStack item = itemData.toItemStack();
					if (outputItems[line][i] != null) {
						ItemStack sample = outputItems[line][i].toItemStack();
						if (sample.getType().equals(item.getType()) && sample.getData().getData() == item.getData().getData()) {
							possibleDirections.add(dir);
						}
					}
				} else if (itemData.equals(outputItems[line][i])) {
					possibleDirections.add(dir);
				}
			}
		}

		//if this item isn't in the list, it will take a random direction from the empty dirs
		if (possibleDirections.isEmpty()) {

			List<PipeDirection> emptyList = new ArrayList<>();

			for (int line = 0; line < 6; line++) {
				PipeDirection dir = PipeDirection.fromID(line);
				//ignore the direction in which is no pipe or inv-block
				if (!connectionDirections.contains(dir)) {
					continue;
				}
				boolean empty = true;
				for (int i = 0; i < 8; i++) {
					if (outputItems[line][i] != null) {
						empty = false;
					}
				}
				if (empty) {
					emptyList.add(dir);
				}
			}

			for (PipeDirection dir : emptyList) {
				//add all possible empty directions without the direction back. Only if this is the only possible way, it will go back.
				if (dir != before.getOpposite() || emptyList.size() == 1) {
					possibleDirections.add(dir);
				}
			}

		}

		//if all lines are full with items, it will simply go back.
		if (possibleDirections.isEmpty()) {
			possibleDirections.add(before.getOpposite());
		}
		return possibleDirections;
	}

	@Override
	public void saveToNBTTag(HashMap<String, Tag> tags) {
		super.saveToNBTTag(tags);

		for (int line = 0; line < 6; line++) {
			List<Tag> lineList = new ArrayList<Tag>();
			ListTag lineTag = new ListTag("Line" + line, CompoundTag.class, lineList);

			for (int i = 0; i < 8; i++) {
				ItemData mad = outputItems[line][i];
				if (mad != null) {
					lineList.add(mad.toNBTTag());
				}
			}

			tags.put("Line" + line, lineTag);
		}

	}

	@Override
	public void loadFromNBTTag(CompoundTag tag) {
		super.loadFromNBTTag(tag);

		Map<String, Tag> map = tag.getValue();

		for (int line = 0; line < 6; line++) {

			ListTag lineTag = (ListTag) map.get("Line" + line);
			List<Tag> lineList = lineTag.getValue();

			for (int i = 0; i < 8; i++) {
				if (lineList.size() > i) {
					ItemData mad = ItemData.fromNBTTag((CompoundTag) lineList.get(i));
					outputItems[line][i] = mad;
				}
			}
		}

	}

	@Override
	public void click(Player p, PipeDirection side) {
		GoldenPipeInv.openGoldenPipeInv(p, this);
	}

	public ItemData[] getOutputItems(PipeDirection pd) {
		return outputItems[pd.getId()];
	}

	public boolean isIgnoreNBT() {
		return ignoreNBT;
	}

	public void setIgnoreNBT(boolean ignoreNBT) {
		this.ignoreNBT = ignoreNBT;
	}

	public void changeOutputItems(PipeDirection pd, List<ItemData> items) {
		for (int i = 0; i < outputItems[pd.getId()].length; i++) {
			if (items.size() > i) {
				outputItems[pd.getId()][i] = items.get(i);
			} else {
				//set item null if the items size is too small -> when you take an item and decrease the items size it will don't override the item with null
				outputItems[pd.getId()][i] = null;
			}
		}
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.GOLDEN;
	}

	@Override
	public List<ItemStack> getDroppedItems() {
		List<ItemStack> is = new ArrayList<ItemStack>();
		is.add(TransportPipes.instance.getGoldenPipeItem());
		for (int line = 0; line < 6; line++) {
			for (int i = 0; i < 8; i++) {
				if (outputItems[line][i] != null) {
					is.add(outputItems[line][i].toItemStack());
				}
			}
		}
		return is;
	}

}
