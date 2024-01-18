package com.reph3x.wot.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class WandOfTranslocationItem extends Item {
//TODO: Chests don't merge with existing chests when placed currently, and chests and barrels do not face the way one would expect
//TODO: Does not yet transfer inventories or any data between chests at all
    public static final String FULL_KEY = "isfull";
    public static final String TYPE_KEY = "inventorytype"; // 0 = chest, 1 = barrel

    public WandOfTranslocationItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        initializeNbt(stack.getOrCreateNbt(), stack);
        super.onCraft(stack, world);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos positionClicked = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        assert player != null;
        BlockState blockClicked = world.getBlockState(positionClicked);
        NbtCompound stackNbt = player.getMainHandStack().getNbt();

        if(!player.getMainHandStack().hasNbt())  {
            initializeNbt(stackNbt, player.getMainHandStack());
        } else { stackNbt = player.getMainHandStack().getNbt(); }
        assert stackNbt != null;

        if(wandIsFull(stackNbt)) {
            if(wandContainsChest(stackNbt)) {
                if(handleChestPlacement(world, positionClicked, player, stackNbt)) return ActionResult.SUCCESS;
            }

            if(wandContainsBarrel(stackNbt)) {
                if(handleBarrelPlacement(world, positionClicked, player, stackNbt)) return ActionResult.SUCCESS;
            }

        }
        else {
            if(blockClicked.isOf(Blocks.CHEST)) {
                handleChestRemoval(world, positionClicked, player, stackNbt);
                return ActionResult.SUCCESS;
            }
            if(blockClicked.isOf(Blocks.BARREL)) {
                handleBarrelRemoval(world, positionClicked, player, stackNbt);
                return ActionResult.SUCCESS;
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

    private boolean handleChestPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) {
            if(determineChestPlacement(world, positionClicked, player, stackNbt)) {
                stackNbt.putBoolean(FULL_KEY, false);
                world.setBlockState(positionClicked.up(), Blocks.CHEST.getDefaultState());
                return true;
            }
        } else {
            if(determineChestPlacement(world, positionClicked, player, stackNbt)) {
                spawnParticles(world, positionClicked);
                world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1, 0);
                return true;
            }
        }
        return false;
    }
    private boolean handleBarrelPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) {
            if(determineBarrelPlacement(world, positionClicked, player, stackNbt)) {
                stackNbt.putBoolean(FULL_KEY, false);
                world.setBlockState(positionClicked.up(), Blocks.BARREL.getDefaultState());
                return true;
            }
        } else {
            if(determineBarrelPlacement(world, positionClicked, player, stackNbt)) {
                spawnParticles(world, positionClicked);
                world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1,0);
                return true;
            }
        }
        return false;
    }

    private void handleChestRemoval(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) fillWandWithChest(world, positionClicked, stackNbt);
        if(world.isClient()) {
            spawnParticles(world, positionClicked);
            world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1, 0);
        }
    }
    private void handleBarrelRemoval(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) fillWandWithBarrel(world, positionClicked, stackNbt);
        if(world.isClient()) {
            spawnParticles(world, positionClicked);
            world.playSound(player, positionClicked, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1, 0);
        }
    }

    private boolean determineChestPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
        BlockState blockTwoAboveClicked = world.getBlockState(positionClicked.up(2));

        if(!blockAboveClicked.isAir()) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Chest Placement: Block For Chest Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }
        if(blockTwoAboveClicked.isSolidBlock(world, positionClicked.up(2))) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Chest Placement: Block Above Chest Is Solid.").formatted(Formatting.RED), false);
            return false;
        }
        //I don't like how dirty these messages are but too small brain to figure out how else to do this only on server while still giving good feedback short of using another NBT tag
        return true;
    }
    private boolean determineBarrelPlacement(World world, BlockPos positionClicked, PlayerEntity player, NbtCompound stackNbt) {
        BlockState blockAboveClicked = world.getBlockState(positionClicked.up());
        if(!blockAboveClicked.isAir()) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Barrel Placement: Block For Barrel Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }

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

    private void spawnParticles(World world, BlockPos positionClicked) {
        Random rand = new Random();
        for(int i = 0; i <= rand.nextInt(3,7); i++) {
            double x = positionClicked.getX() + 0.5 + rand.nextDouble(0.5);
            double y = positionClicked.getY() + 0.5 + rand.nextDouble(0.5);
            double z = positionClicked.getZ() + 0.5 + rand.nextDouble(0.5);
            double xA = rand.nextDouble(-0.2, 0.2);
            double yA = rand.nextDouble(-0.2, 0.2);
            double zA = rand.nextDouble(-0.2, 0.2);
            world.addParticle(ParticleTypes.POOF, x, y, z, xA, yA, zA);
        }
    }

}


