package com.reph3x.wot.custom;

import com.reph3x.wot.WandOfTranslocation;
import com.reph3x.wot.component.WandOfTranslocationComponents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.*;

//TODO: - Implement air gaps between items moved between inventories
//      - Make double chests able to be picked up, maybe with left click?
//      - If you get really crazy maybe make a codec for inventories so air gaps work

public class WandOfTranslocationItem extends Item {

    Logger LOGGER = WandOfTranslocation.LOGGER;

    public WandOfTranslocationItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        initializeComponents(stack);
        super.onCraft(stack, world);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos positionClicked = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack itemStack = context.getStack();
        assert player != null;
        BlockState blockClicked = world.getBlockState(positionClicked);

        if(!itemStack.contains(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT) && !itemStack.contains(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT)) {
            initializeComponents(itemStack);
        }

        if(Boolean.TRUE.equals(itemStack.get(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT))) {
            if(Boolean.TRUE.equals(itemStack.get(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT))) {
                if(handleBarrelPlacement(world, positionClicked, player, itemStack)) return ActionResult.SUCCESS;
            }
            else if(handleChestPlacement(world, positionClicked, player, itemStack)) return ActionResult.SUCCESS;
        }
        else {
            if(blockClicked.isOf(Blocks.CHEST)) {
                handleContainerRemoval(world, positionClicked, player, itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
                return ActionResult.SUCCESS;
            }
            else if(blockClicked.isOf(Blocks.BARREL)) {
                handleContainerRemoval(world, positionClicked, player, itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, true);
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(Boolean.TRUE.equals(itemStack.get(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT))) {
            if(Boolean.TRUE.equals(itemStack.get(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT))) {
                tooltip.add(Text.translatable("A barrel is stored!").formatted(Formatting.BLUE));
            }
            else {
                tooltip.add(Text.translatable("A chest is stored!").formatted(Formatting.BLUE));
            }
        }
        else {
            tooltip.add(Text.translatable("Nothing is stored").formatted(Formatting.DARK_RED));
        }
        if(itemStack.getComponents().contains(WandOfTranslocationComponents.WAND_ABUNDANT_ITEM_NAME_COMPONENT)) {
            tooltip.add(Text.translatable("Container holds mostly: "+itemStack.get(WandOfTranslocationComponents.WAND_ABUNDANT_ITEM_NAME_COMPONENT)));
        }
    }

    private void initializeComponents(ItemStack itemStack) {
        itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
        itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
    }

    private boolean handleBarrelPlacement(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
        if(!world.isClient()) {
            if(determineBarrelPlacement(world, positionClicked, player)) {
                BlockHitResult hitResult = new BlockHitResult(positionClicked.toBottomCenterPos(), Direction.UP, positionClicked, false);
                ItemPlacementContext itemContext = new ItemPlacementContext(player, player.getActiveHand(), new ItemStack(Items.BARREL), hitResult);
                world.setBlockState(positionClicked.up(), Blocks.BARREL.getPlacementState(itemContext));
                populateInventory(world, positionClicked.up(), itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
                itemStack.remove(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT);
                itemStack.remove(WandOfTranslocationComponents.WAND_ABUNDANT_ITEM_NAME_COMPONENT);
                return true;
            }
        }
        else {
            if(determineBarrelPlacement(world, positionClicked, player)) {
                spawnParticles(world, positionClicked);
                world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1, 0);
                return true;
            }
        }
        return false;
    }

    private boolean handleChestPlacement(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
        if(!world.isClient()) {
            if(determineChestPlacement(world, positionClicked, player)) {
                BlockHitResult hitResult = new BlockHitResult(positionClicked.toBottomCenterPos(), Direction.UP, positionClicked, false);
                ItemPlacementContext itemContext = new ItemPlacementContext(player, player.getActiveHand(), new ItemStack(Items.CHEST), hitResult);
                world.setBlockState(positionClicked.up(), Blocks.CHEST.getPlacementState(itemContext));
                populateInventory(world, positionClicked.up(), itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
                itemStack.remove(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT);
                itemStack.remove(WandOfTranslocationComponents.WAND_ABUNDANT_ITEM_NAME_COMPONENT);
                return true;
            }
        }
        else {
            if(determineChestPlacement(world, positionClicked, player)) {
                spawnParticles(world, positionClicked);
                world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1, 0);
                return true;
            }
        }
        return false;
    }

    private void handleContainerRemoval(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
        if(!world.isClient()) {
            populateWand(world, positionClicked, itemStack);
            itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, true);
            world.removeBlock(positionClicked, false);
        }
        else {
            spawnParticles(world, positionClicked);
            world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1, 0);
        }
    }

    private boolean determineChestPlacement(World world, BlockPos pos, PlayerEntity player) {
        BlockState blockAbove = world.getBlockState(pos.up());
        BlockState blockTwoAbove = world.getBlockState(pos.up(2));

        if(!blockAbove.isAir()) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Chest Placement: Block For Chest Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }
        if(blockTwoAbove.isSolidBlock(world, pos.up(2))) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Chest Placement: Block Above Chest Is Solid.").formatted(Formatting.RED), false);
            return false;
        }
        return true;
    }
    private boolean determineBarrelPlacement(World world, BlockPos pos, PlayerEntity player) {
        BlockState blockAbove = world.getBlockState(pos.up());
        if(!blockAbove.isAir()) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Barrel Placement: Block For Barrel Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }
        return true;
    }

    private void populateWand(World world, BlockPos pos, ItemStack itemStack) {
        LootableContainerBlockEntity target = (LootableContainerBlockEntity) world.getBlockEntity(pos);
        List<ItemStack> inventory = new ArrayList<>();
        Map<String, Integer> itemNames = new HashMap<>();
        for(int i = 0; i < target.size(); i++) {
            ItemStack currentItemStack = target.getStack(i);
            String itemName = currentItemStack.getName().getString();

            if(!currentItemStack.isEmpty()) {
                inventory.add(currentItemStack);
                if(!itemNames.containsKey(itemName)) itemNames.put(itemName, currentItemStack.getCount());
                else itemNames.compute(itemName, (k, count) -> currentItemStack.getCount() + count);
            }
        }
        /*int highestCount = Collections.max(itemNames.values());
        for(Map.Entry<String, Integer> entry : itemNames.entrySet()) {
            if(entry.getValue() == highestCount) itemStack.set(WandOfTranslocationComponents.WAND_ABUNDANT_ITEM_NAME_COMPONENT, entry.getKey());
        }*/
        itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, true);
        itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT, inventory);
        target.clear();
    }

    private void populateInventory(World world, BlockPos pos, ItemStack itemStack) {
        List<ItemStack> inventory = itemStack.get(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT);
        LootableContainerBlockEntity target = (LootableContainerBlockEntity) world.getBlockEntity(pos);
        for(int i = 0; i < inventory.size(); i++) {
            target.setStack(i, inventory.get(i));
        }
    }

    private void spawnParticles(World world, BlockPos pos) {
        Random rand = new Random();
        for(int i = 0; i <= rand.nextInt(3,7); i++) {
            double x = pos.getX() + rand.nextDouble(0.9);
            double y = pos.getY() + rand.nextDouble(0.9);
            double z = pos.getZ() + rand.nextDouble(0.9);
            double xA = rand.nextDouble(-0.2, 0.2);
            double yA = rand.nextDouble(0, 0.2);
            double zA = rand.nextDouble(-0.2, 0.2);
            world.addParticle(ParticleTypes.POOF, x, y, z, xA, yA, zA);
        }
    }

}
