package se.mickelus.tetra.items.modular.impl.toolbelt.inventory;

import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.items.ItemPredicateComposite;
import se.mickelus.tetra.items.modular.impl.toolbelt.ModularToolbeltItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.SlotType;
import se.mickelus.tetra.effect.ItemEffect;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ToolbeltInventory implements IInventory {
    protected static final String slotKey = "slot";

    protected ItemStack toolbeltItemStack;

    protected SlotType inventoryType;

    protected final String inventoryKey;
    protected NonNullList<ItemStack> inventoryContents;
    protected int numSlots = 0;
    protected int maxSize = 0;

    ItemPredicate predicate = ItemPredicate.ANY;
    public static ItemPredicate potionPredicate = ItemPredicate.ANY;
    public static ItemPredicate quickPredicate = ItemPredicate.ANY;
    public static ItemPredicate quiverPredicate = ItemPredicate.ANY;
    public static ItemPredicate storagePredicate = ItemPredicate.ANY;

    public ToolbeltInventory(String inventoryKey, ItemStack stack, int maxSize, SlotType inventoryType) {
        this.inventoryKey = inventoryKey;
        toolbeltItemStack = stack;

        this.inventoryType = inventoryType;

        this.maxSize = maxSize;
        inventoryContents = NonNullList.withSize(maxSize, ItemStack.EMPTY);
    }

    public static void initializePredicates() {
        DataManager.predicateData.onReload(() -> {
            potionPredicate = getPredicate("potion");
            quickPredicate = getPredicate("quick");
            quiverPredicate = getPredicate("quiver");
            storagePredicate = getPredicate("storage");
        });
    }

    private static ItemPredicate getPredicate(String inventory) {
        ItemPredicate[] predicates = Arrays.stream(DataManager.predicateData.getData(
                new ResourceLocation(TetraMod.MOD_ID, String.format("toolbelt/%s", inventory))))
                .filter(Objects::nonNull)
                .toArray(ItemPredicate[]::new);

        // todo: add debug log
        if (predicates.length > 0) {
            return new ItemPredicateComposite(predicates);
        }

        return ItemPredicate.ANY;
    }


    public void readFromNBT(CompoundNBT compound) {
        ListNBT items = compound.getList(inventoryKey, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < items.size(); i++) {
            CompoundNBT itemTag = items.getCompound(i);
            int slot = itemTag.getByte(slotKey) & 255;

            if (0 <= slot && slot < maxSize) {
                inventoryContents.set(slot, ItemStack.of(itemTag));
            }
        }
    }

    public void writeToNBT(CompoundNBT tagcompound) {
        ListNBT items = new ListNBT();

        for (int i = 0; i < maxSize; i++) {
            if (getItem(i) != null) {
                CompoundNBT compound = new CompoundNBT();
                getItem(i).save(compound);
                compound.putByte(slotKey, (byte)i);
                items.add(compound);
            }
        }

        tagcompound.put(inventoryKey, items);
    }

    @Override
    public int getContainerSize() {
        return numSlots;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return inventoryContents.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemstack = ItemStackHelper.removeItem(this.inventoryContents, index, count);

        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack itemStack = this.inventoryContents.get(index);

        if (itemStack.isEmpty()) {
            return itemStack;
        } else {
            this.inventoryContents.set(index, ItemStack.EMPTY);
            return itemStack;
        }
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.inventoryContents.set(index, stack);

        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        for (int i = 0; i < getContainerSize(); ++i) {
            if (getItem(i).getCount() == 0) {
                inventoryContents.set(i, ItemStack.EMPTY);
            }
        }

        writeToNBT(toolbeltItemStack.getOrCreateTag());
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public void startOpen(PlayerEntity player) {}

    @Override
    public void stopOpen(PlayerEntity player) {}

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return isItemValid(stack);
    }

    @Override
    public void clearContent() {
        inventoryContents.clear();
    }

    public ItemStack takeItemStack(int index) {
        ItemStack itemStack = getItem(index);
        setItem(index, ItemStack.EMPTY);
        return itemStack;
    }

    public void emptyOverflowSlots(PlayerEntity player) {
        for (int i = getContainerSize(); i < maxSize; i++) {
            moveStackToPlayer(removeItemNoUpdate(i), player);
        }
        this.setChanged();
    }

    protected void moveStackToPlayer(ItemStack itemStack, PlayerEntity player) {
        if (!itemStack.isEmpty()) {
            if (!player.inventory.add(itemStack)) {
                player.drop(itemStack, false);
            }
        }
    }

    public boolean isItemValid(ItemStack itemStack) {
        return !ModularToolbeltItem.instance.equals(itemStack.getItem()) && predicate.matches(itemStack);
    }

    public boolean storeItemInInventory(ItemStack itemStack) {
        if (!isItemValid(itemStack)) {
            return false;
        }

        // attempt to merge the itemstack with itemstacks in the toolbelt
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack storedStack = getItem(i);
            if (ItemStack.isSame(itemStack, storedStack)
                    && ItemStack.tagMatches(itemStack, storedStack)
                    && storedStack.getCount() < storedStack.getMaxStackSize()) {

                int moveCount = Math.min(itemStack.getCount(), storedStack.getMaxStackSize() - storedStack.getCount());
                storedStack.grow(moveCount);
                setItem(i, storedStack);
                itemStack.shrink(moveCount);

                if (itemStack.isEmpty()) {
                    return true;
                }
            }
        }

        // put item in the first empty slot
        for (int i = 0; i < getContainerSize(); i++) {
            if (getItem(i).isEmpty()) {
                setItem(i, itemStack);
                return true;
            }
        }
        return false;
    }

    public int getFirstIndexForItem(Item item) {
        for (int i = 0; i < inventoryContents.size(); i++) {
            if (!inventoryContents.get(i).isEmpty() && inventoryContents.get(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public List<Collection<ItemEffect>> getSlotEffects() {
        return ModularToolbeltItem.instance.getSlotEffects(toolbeltItemStack, inventoryType);
    }
}
