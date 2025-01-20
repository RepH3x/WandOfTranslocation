package com.reph3x.wot.custom;

import com.reph3x.wot.WandOfTranslocation;
import com.reph3x.wot.component.WandOfTranslocationComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

//TODO: - Implement air gaps between items moved between inventories
//      - If you get really crazy maybe make a codec for inventories?

public class WandOfTranslocationItem extends Item {

    private static final int CHEST_SLOT_COUNT = 27;
    private static final int DOUBLE_CHEST_SLOT_COUNT = 54;

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
                handleRemoval(world, positionClicked, player, itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
                return ActionResult.SUCCESS;
            }
            else if(blockClicked.isOf(Blocks.BARREL)) {
                handleRemoval(world, positionClicked, player, itemStack);
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
    }

    private void initializeComponents(ItemStack itemStack) {
        itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
        itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_IS_BARREL_COMPONENT, false);
    }

    private boolean handleBarrelPlacement(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
        if(!world.isClient()) {
            if(determineBarrelPlacement(world, positionClicked, player)) {
                itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
                world.setBlockState(positionClicked.up(), Blocks.BARREL.getDefaultState());
                populateInventory(world, positionClicked.up(), itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT, new ArrayList<ItemStack>());
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

    private void handleRemoval(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
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

    private boolean handleChestPlacement(World world, BlockPos positionClicked, PlayerEntity player, ItemStack itemStack) {
        if(!world.isClient()) {
            if(determineChestPlacement(world, positionClicked, player)) {
                itemStack.set(WandOfTranslocationComponents.WAND_IS_FULL_COMPONENT, false);
                world.setBlockState(positionClicked.up(), Blocks.CHEST.getDefaultState());
                populateInventory(world, positionClicked.up(), itemStack);
                itemStack.set(WandOfTranslocationComponents.WAND_INVENTORY_COMPONENT, new ArrayList<ItemStack>());
                attemptChestMerging(world, positionClicked.up());
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

    private boolean attemptChestMerging(World world, BlockPos pos) {
        BlockState neighborChest = null;
        switch(neighborIsChest(world, pos)) {
            case -1: return false;
            case 0: //north
                neighborChest = world.getBlockState(pos.north());
                break;
            case 1: //south
                neighborChest = world.getBlockState(pos.south());
                break;
            case 2: //east
                neighborChest = world.getBlockState(pos.east());
                break;
            case 3: //west
                neighborChest = world.getBlockState(pos.west());
                break;
        }
        assert neighborChest != null;
        if(neighborChest.get(ChestBlock.CHEST_TYPE).equals(ChestType.SINGLE)) {
            WandOfTranslocation.LOGGER.info("Placed next to a single chest.");
        } else WandOfTranslocation.LOGGER.info("Placed next to a double chest.");
        return true;
    }

    private int neighborIsChest(World world, BlockPos pos) {
        if (world.getBlockState(pos.north()).isOf(Blocks.CHEST)) return 0;
        if (world.getBlockState(pos.south()).isOf(Blocks.CHEST)) return 1;
        if (world.getBlockState(pos.east()).isOf(Blocks.CHEST)) return 2;
        if (world.getBlockState(pos.west()).isOf(Blocks.CHEST)) return 3;
        return -1;
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
        List<ItemStack> inventory = new ArrayList<ItemStack>();
        for(int i = 0; i < target.size(); i++) {
            if(!target.getStack(i).isEmpty())
                inventory.add(target.getStack(i));
        }
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
            double x = pos.getX() + 0.5 + rand.nextDouble(0.5);
            double y = pos.getY() + 0.5 + rand.nextDouble(0.5);
            double z = pos.getZ() + 0.5 + rand.nextDouble(0.5);
            double xA = rand.nextDouble(0, 0.2);
            double yA = rand.nextDouble(0, 0.2);
            double zA = rand.nextDouble(0, 0.2);
            world.addParticle(ParticleTypes.POOF, x, y, z, xA, yA, zA);
        }
    }

}
