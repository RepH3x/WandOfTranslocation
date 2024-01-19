package com.reph3x.wot.item.custom;

import com.reph3x.wot.WandOfTranslocation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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
//TODO: Make name of most plentiful item in stored inventory to show on tooltip
    public static final String FULL_KEY = "isfull";
    public static final String TYPE_KEY = "wandinventorytype"; // 0 = chest, 1 = barrel
    public static final String INVENTORY_KEY = "wandinventory";
    public static final int CHEST_SLOT_COUNT = 27;
    public static final int DOUBLE_CHEST_SLOT_COUNT = 54;

    public WandOfTranslocationItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        initializeNbt(stack);
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
        if(stackNbt == null) stackNbt = initializeNbt(player.getMainHandStack());

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


    private NbtCompound initializeNbt(ItemStack itemStack) {
        NbtCompound stackNbt = itemStack.getOrCreateNbt();
        stackNbt.putBoolean(FULL_KEY, false);
        stackNbt.putInt(TYPE_KEY, -1);
        return stackNbt;
    }

    private boolean wandIsFull(NbtCompound stackNbt) { return stackNbt.contains(FULL_KEY) && stackNbt.getBoolean(FULL_KEY); }
    private boolean wandContainsChest(NbtCompound stackNbt) { return stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 0; }
    private boolean wandContainsBarrel(NbtCompound stackNbt) { return stackNbt.contains(TYPE_KEY) && stackNbt.getInt(TYPE_KEY) == 1; }
    private int neighborIsChest(World world, BlockPos pos) {
        if(world.getBlockState(pos.north()).isOf(Blocks.CHEST)) return 0;
        if(world.getBlockState(pos.south()).isOf(Blocks.CHEST)) return 1;
        if(world.getBlockState(pos.east()).isOf(Blocks.CHEST)) return 2;
        if(world.getBlockState(pos.west()).isOf(Blocks.CHEST)) return 3;
        return -1;
    }

    private boolean handleChestPlacement(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
        //ChestBlock getPlacementState() for chest facing on placement reference I think
        if(!world.isClient()) {
            if(determineChestPlacement(world, pos, player, stackNbt)) {
                stackNbt.putBoolean(FULL_KEY, false);
                world.setBlockState(pos.up(), Blocks.CHEST.getDefaultState());
                populateChest(world, pos.up(), stackNbt);
                attemptChestMerging(world, pos.up());
                return true;
            }
        } else {
            if(determineChestPlacement(world, pos, player, stackNbt)) {
                spawnParticles(world, pos);
                world.playSound(player, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1, 0);
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
    private boolean handleBarrelPlacement(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) {
            if(determineBarrelPlacement(world, pos, player, stackNbt)) {
                stackNbt.putBoolean(FULL_KEY, false);
                world.setBlockState(pos.up(), Blocks.BARREL.getDefaultState());
                populateBarrel(world, pos.up(), stackNbt);
                return true;
            }
        } else {
            if(determineBarrelPlacement(world, pos, player, stackNbt)) {
                spawnParticles(world, pos);
                world.playSound(player, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1,0);
                return true;
            }
        }
        return false;
    }
    private void handleChestRemoval(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) {
            populateWandWithChest(world, pos, stackNbt);
            world.removeBlock(pos, false);
        }
        if(world.isClient()) {
            spawnParticles(world, pos);
            world.playSound(player, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1, 0);
        }

    }
    private void handleBarrelRemoval(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
        if(!world.isClient()) {
            populateWandWithBarrel(world, pos, stackNbt);
            world.removeBlock(pos, false);
        }
        if(world.isClient()) {
            spawnParticles(world, pos);
            world.playSound(player, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1, 0);
        }
    }
    private boolean determineChestPlacement(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
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
        //I don't like how dirty these messages are but too small brain to figure out how else to do this only on server while still giving good feedback short of using another NBT tag
        return true;
    }
    private boolean determineBarrelPlacement(World world, BlockPos pos, PlayerEntity player, NbtCompound stackNbt) {
        BlockState blockAbove = world.getBlockState(pos.up());
        if(!blockAbove.isAir()) {
            if(!world.isClient()) player.sendMessage(Text.literal("Invalid Barrel Placement: Block For Barrel Is Not Air.").formatted(Formatting.RED), false);
            return false;
        }

        stackNbt.putBoolean(FULL_KEY, false);
        world.setBlockState(pos.up(), Blocks.BARREL.getDefaultState());
        return true;
    }

    private void populateWandWithChest(World world, BlockPos pos, NbtCompound stackNbt) {
        ChestBlockEntity chest = (ChestBlockEntity)world.getBlockEntity(pos);
        SimpleInventory inventory = new SimpleInventory(CHEST_SLOT_COUNT);
        for(int i = 0; i < chest.size(); i++) {
                inventory.setStack(i, chest.getStack(i).copy());
        }
        stackNbt.putBoolean(FULL_KEY, true);
        stackNbt.putInt(TYPE_KEY, 0);
        stackNbt.put(INVENTORY_KEY, inventory.toNbtList());
        chest.clear();
    }
    private void populateWandWithBarrel(World world, BlockPos pos, NbtCompound stackNbt) {
        BarrelBlockEntity barrel = (BarrelBlockEntity)world.getBlockEntity(pos);
        SimpleInventory inventory = new SimpleInventory(CHEST_SLOT_COUNT);
        for(int i = 0; i < barrel.size(); i++) {
                inventory.setStack(i, barrel.getStack(i).copy());
        }
        stackNbt.putBoolean(FULL_KEY, true);
        stackNbt.putInt(TYPE_KEY, 1);
        stackNbt.put(INVENTORY_KEY, inventory.toNbtList());
        barrel.clear();
    }

    private void populateChest(World world, BlockPos pos, NbtCompound stackNbt) {
        SimpleInventory inventory = new SimpleInventory(CHEST_SLOT_COUNT);
        inventory.readNbtList((NbtList)stackNbt.get(INVENTORY_KEY));
        ChestBlockEntity chest = (ChestBlockEntity)world.getBlockEntity(pos);
        for(int i = 0; i < chest.size(); i++) {
            chest.setStack(i, inventory.getStack(i).copy());
        }
    }
    private void populateBarrel(World world, BlockPos pos, NbtCompound stackNbt) {
        SimpleInventory inventory = new SimpleInventory(CHEST_SLOT_COUNT);
        inventory.readNbtList((NbtList)stackNbt.get(INVENTORY_KEY));
        BarrelBlockEntity barrel = (BarrelBlockEntity)world.getBlockEntity(pos);
        for(int i = 0; i < barrel.size(); i++) {
            barrel.setStack(i, inventory.getStack(i).copy());
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