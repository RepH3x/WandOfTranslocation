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
            NbtCompound stackNbt = player.getMainHandStack().getOrCreateNbt();

            //IF THERE IS SOMETHING STORED IN THE WAND
            if(stackNbt.contains(FULL_KEY) && stackNbt.getBoolean(FULL_KEY)) {
                //IF THERE IS A CHEST STORED
                if(stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 0) {
                    BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
                    BlockState blockTwoAboveClicked = world.getBlockState(positionClicked.up(2));

                    if(!blockAboveClicked.isAir()) {
                        player.sendMessage(Text.literal("Invalid Chest Placement: Block For Chest Is Not Air."), false);
                        return ActionResult.FAIL;
                    }
                    if(blockTwoAboveClicked.isSolidBlock(world, positionClicked.up(2))) {
                        player.sendMessage(Text.literal("Invalid Chest Placement: Block Above Chest Is Solid."), false);
                        return ActionResult.FAIL;
                    }

                    player.sendMessage(Text.literal("Valid Chest Placement!"),false);
                    stackNbt.putBoolean(FULL_KEY, false);
                    world.setBlockState(positionClicked.up(), Blocks.CHEST.getDefaultState());
                    return ActionResult.SUCCESS;
                }
                //IF THERE IS A BARREL STORED
                if(stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 1) {
                    BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
                    if(!blockAboveClicked.isAir()) {
                        player.sendMessage(Text.literal("Invalid Barrel Placement: Block For Barrel Is Not Air."), false);
                        return ActionResult.FAIL;
                    }
                    player.sendMessage(Text.literal("Valid Barrel Placement!"),false);
                    stackNbt.putBoolean(FULL_KEY, false);
                    world.setBlockState(positionClicked.up(), Blocks.BARREL.getDefaultState());
                    return ActionResult.SUCCESS;
                }

            //IF THERE IS NOTHING STORED IN THE WAND
            } else {
                assert player != null;
                if(blockClicked.isOf(Blocks.CHEST)) {
                    stackNbt.putBoolean(FULL_KEY, true);
                    stackNbt.putInt(TYPE_KEY, 0);
                    world.removeBlock(positionClicked, false);
                    return ActionResult.SUCCESS;
                }
                if(blockClicked.isOf(Blocks.BARREL)) {
                    stackNbt.putBoolean(FULL_KEY, true);
                    stackNbt.putInt(TYPE_KEY, 1);
                    world.removeBlock(positionClicked, false);
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

}


