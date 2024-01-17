package com.reph3x.wot.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WandOfTranslocationItem extends Item {

    private boolean isFull;

    public WandOfTranslocationItem(Settings settings) {
        super(settings);
        isFull = true;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if(!world.isClient()) {
            BlockPos positionClicked = context.getBlockPos();
            PlayerEntity player = context.getPlayer();
            BlockState blockClicked = world.getBlockState(positionClicked);

            if(isFull) {
                //TODO: Must differentiate if the stored inventory is a chest or barrel because barrels do not require a non-solid block above them.
                assert player!= null;
                BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
                BlockState blockTwoAboveClicked = world.getBlockState(positionClicked.up(2));

                if(!blockClicked.isSolidBlock(world, positionClicked)) {
                    player.sendMessage(Text.literal("Invalid Chest Placement: Block Below Chest Is Not Solid."), false);
                    return ActionResult.FAIL;
                }
                if(!blockAboveClicked.isAir()) {
                    player.sendMessage(Text.literal("Invalid Chest Placement: Block For Chest Is Not Air."), false);
                    return ActionResult.FAIL;
                }
                if(blockTwoAboveClicked.isSolidBlock(world, positionClicked.up(2))) {
                    player.sendMessage(Text.literal("Invalid Chest Placement: Block Above Chest Is Solid."), false);
                    return ActionResult.FAIL;
                }

                player.sendMessage(Text.literal("Valid Chest Placement!"),false);


            } else {
                assert player != null;
                if(blockClicked.isOf(Blocks.CHEST)) {
                    player.sendMessage(Text.literal("Attempted to use on a chest."), false);
                }
                if(blockClicked.isOf(Blocks.BARREL)) {
                    player.sendMessage(Text.literal("Attempted to use on a barrel."), false);
                }
            }

        }


        return ActionResult.FAIL;
    }
}
