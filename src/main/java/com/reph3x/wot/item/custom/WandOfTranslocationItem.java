package com.reph3x.wot.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WandOfTranslocationItem extends Item {
    //TODO: Go through and make methods for those big long ugly loops so they are more readable


    public static final String FULL_KEY = "isfull";
    public static final String TYPE_KEY = "inventorytype"; // 0 = chest, 1 = barrel

    public WandOfTranslocationItem(Settings settings) {
        super(settings);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if(!world.isClient()) {
            BlockPos positionClicked = context.getBlockPos();
            PlayerEntity player = context.getPlayer();
            assert player != null;
            BlockState blockClicked = world.getBlockState(positionClicked);
            NbtCompound stackNbt = new NbtCompound();

            if(!player.getMainHandStack().hasNbt())  {
                initializeNbt(stackNbt, player.getMainHandStack());
            } else { stackNbt = player.getMainHandStack().getNbt(); }
            assert stackNbt != null;

            if(wandIsFull(stackNbt)) {

                if(wandContainsChest(stackNbt)) {
                    if(determineChestPlacement(world, positionClicked, player, stackNbt)) { return ActionResult.SUCCESS; }
                    else { return ActionResult.FAIL; }
                }

                if(wandContainsBarrel(stackNbt)) {
                    if (determineBarrelPlacement(world, positionClicked, player, stackNbt)) { return ActionResult.SUCCESS; }
                    else { return ActionResult.FAIL; }
                }

            } else {

                assert player != null;
                if(blockClicked.isOf(Blocks.CHEST)) {
                    fillWandWithChest(world, positionClicked, stackNbt);
                    return ActionResult.SUCCESS;
                }
                if(blockClicked.isOf(Blocks.BARREL)) {
                    fillWandWithBarrel(world, positionClicked, stackNbt);
                    return ActionResult.SUCCESS;
                }
            }

        }
        return ActionResult.FAIL;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasNbt()) {
            NbtCompound stackNbt = stack.getNbt();
            if(stackNbt.contains(FULL_KEY)) {
                if(stackNbt.getBoolean(FULL_KEY)) {
                    if(stackNbt.getInt(TYPE_KEY) == 0) {
                        tooltip.add(Text.literal("A chest is stored!").formatted(Formatting.BLUE));
                    } else {
                        tooltip.add(Text.literal("A barrel is stored!").formatted(Formatting.BLUE));
                    }
                } else {
                    tooltip.add(Text.literal("Nothing is stored").formatted(Formatting.DARK_RED));
                }
            }

        }
        super.appendTooltip(stack, world, tooltip, context);
    }


    private void initializeNbt(NbtCompound stackNbt, ItemStack itemStack) {
        stackNbt = itemStack.getOrCreateNbt();
        stackNbt.putBoolean(FULL_KEY, false);
        stackNbt.putInt(TYPE_KEY, -1);
    }

    private boolean wandIsFull(NbtCompound stackNbt) { return stackNbt.contains(FULL_KEY) && stackNbt.getBoolean(FULL_KEY); }
    private boolean wandContainsChest(NbtCompound stackNbt) { return stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 0; }
    private boolean wandContainsBarrel(NbtCompound stackNbt) { return stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 1; }

    private boolean determineChestPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
        BlockState blockTwoAboveClicked = world.getBlockState(positionClicked.up(2));

        if(!blockAboveClicked.isAir()) {
            player.sendMessage(Text.literal("Invalid Chest Placement: Block For Chest Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }
        if(blockTwoAboveClicked.isSolidBlock(world, positionClicked.up(2))) {
            player.sendMessage(Text.literal("Invalid Chest Placement: Block Above Chest Is Solid.").formatted(Formatting.RED), false);
            return false;
        }

        player.sendMessage(Text.literal("Valid Chest Placement!").formatted(Formatting.GREEN),false);
        stackNbt.putBoolean(FULL_KEY, false);
        world.setBlockState(positionClicked.up(), Blocks.CHEST.getDefaultState());
        return true;
    }
    private boolean determineBarrelPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
        if(!blockAboveClicked.isAir()) {
            player.sendMessage(Text.literal("Invalid Barrel Placement: Block For Barrel Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }
        player.sendMessage(Text.literal("Valid Barrel Placement!").formatted(Formatting.GREEN),false);
        stackNbt.putBoolean(FULL_KEY, false);
        world.setBlockState(positionClicked.up(), Blocks.BARREL.getDefaultState());
        return true;
    }

    private void fillWandWithChest(World world, BlockPos positionClicked, NbtCompound stackNbt) {
        stackNbt.putBoolean(FULL_KEY, true);
        stackNbt.putInt(TYPE_KEY, 0);
        world.removeBlock(positionClicked, false);
    }
    private void fillWandWithBarrel(World world, BlockPos positionClicked, NbtCompound stackNbt) {
        stackNbt.putBoolean(FULL_KEY, true);
        stackNbt.putInt(TYPE_KEY, 1);
        world.removeBlock(positionClicked, false);
    }

}


