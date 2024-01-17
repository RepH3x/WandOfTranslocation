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
        isFull = false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if(!world.isClient()) {
            BlockPos positionClicked = context.getBlockPos();
            PlayerEntity player = context.getPlayer();
            BlockState blockClicked = world.getBlockState(positionClicked);
            if(isFull) {

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
