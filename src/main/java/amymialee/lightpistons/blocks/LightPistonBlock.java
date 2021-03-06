package amymialee.lightpistons.blocks;

import amymialee.lightpistons.RegisterPistons;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.PistonType;
import net.minecraft.tileentity.PistonTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Map;

import static net.minecraft.block.PistonHeadBlock.SHORT;
import static net.minecraft.block.PistonHeadBlock.TYPE;

public class LightPistonBlock extends PistonBlock {
    public static final IntegerProperty EXTENDEDLIGHT = IntegerProperty.create("extendlight", 0, 2);
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED, EXTENDEDLIGHT);
    }

    public LightPistonBlock(boolean sticky, AbstractBlock.Properties properties) {
        super(sticky, properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(EXTENDED, Boolean.FALSE).with(EXTENDEDLIGHT, 0));
        this.isSticky = sticky;
    }
    private final boolean isSticky;

    @Override
    public boolean eventReceived(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        if (id < 3) {
            return eventReceived1(state, worldIn, pos, id, param);
        } else {
            return eventReceived2(state, worldIn, pos, id - 3, param);
        }
    }
    public boolean eventReceived1(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        Direction direction = state.get(FACING);
        if (!worldIn.isRemote) {
            boolean flag = this.shouldBeExtended(worldIn, pos, direction);
            if (flag && (id == 1 || id == 2)) {
                worldIn.setBlockState(pos, state.with(EXTENDED, Boolean.TRUE), 2);
                return false;
            }
            if (!flag && id == 0) {
                return false;
            }
        }
        if (id == 0) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(worldIn, pos, direction, true)) return false;
            if (!this.doMove(worldIn, pos, direction, true)) {
                return false;
            }
            worldIn.setBlockState(pos, state.with(EXTENDED, Boolean.TRUE), 67);
            worldIn.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, worldIn.rand.nextFloat() * 0.25F + 0.6F);
        } else if (id == 1 || id == 2) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(worldIn, pos, direction, false)) return false;
            TileEntity tileentity1 = worldIn.getTileEntity(pos.offset(direction));
            if (tileentity1 instanceof PistonTileEntity) {
                ((PistonTileEntity)tileentity1).clearPistonTileEntity();
            }
            BlockState blockstate = Blocks.MOVING_PISTON
                    .getDefaultState()
                    .with(MovingPistonBlock.FACING, direction)
                    .with(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            worldIn.setBlockState(pos, blockstate, 20);
            worldIn.setTileEntity(pos, MovingPistonBlock.createTilePiston(this.getDefaultState().with(FACING, Direction.byIndex(param & 7)),
                    direction, false, true));
            worldIn.updateBlock(pos, blockstate.getBlock());
            blockstate.updateNeighbours(worldIn, pos, 2);
            if (this.isSticky) {
                BlockPos blockpos = pos.add(direction.getXOffset() * 2, direction.getYOffset() * 2, direction.getZOffset() * 2);
                BlockState blockstate1 = worldIn.getBlockState(blockpos);
                boolean flag1 = false;
                if (blockstate1.matchesBlock(Blocks.MOVING_PISTON)) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof PistonTileEntity) {
                        PistonTileEntity pistontileentity = (PistonTileEntity)tileentity;
                        if (pistontileentity.getFacing() == direction && pistontileentity.isExtending()) {
                            pistontileentity.clearPistonTileEntity();
                            flag1 = true;
                        }
                    }
                }
                if (!flag1) {
                    if (blockstate1.matchesBlock(Blocks.OBSIDIAN) || blockstate1.matchesBlock(Blocks.CRYING_OBSIDIAN) || blockstate1.matchesBlock(Blocks.RESPAWN_ANCHOR)) {
                        this.doMoveForward(worldIn, pos, direction);
                    } else if (id != 1
                            || blockstate1.isAir()
                            || !canPush(blockstate1, worldIn, blockpos, direction.getOpposite(), false, direction)
                            || blockstate1.getPushReaction() != PushReaction.NORMAL
                            && !blockstate1.matchesBlock(Blocks.PISTON)
                            && !blockstate1.matchesBlock(Blocks.STICKY_PISTON)
                            && !blockstate1.matchesBlock(RegisterPistons.lightPiston)
                            && !blockstate1.matchesBlock(RegisterPistons.lightStickyPiston)) {
                        worldIn.removeBlock(pos.offset(direction), false);
                    } else {
                        this.doMove(worldIn, pos, direction, false);
                    }
                }
            } else {
                worldIn.removeBlock(pos.offset(direction), false);
            }
            worldIn.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, worldIn.rand.nextFloat() * 0.15F + 0.6F);
        }
        net.minecraftforge.event.ForgeEventFactory.onPistonMovePost(worldIn, pos, direction, (id == 0));
        return true;
    }
    public boolean eventReceived2(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        Direction direction = state.get(FACING).getOpposite();
        if (!worldIn.isRemote) {
            boolean flag = this.shouldBeExtended(worldIn, pos, direction);
            if (flag && (id == 1 || id == 2)) {
                worldIn.setBlockState(pos, state.with(EXTENDED, Boolean.TRUE), 2);
                return false;
            }
            if (!flag && id == 0) {
                return false;
            }
        }
        if (id == 0) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(worldIn, pos, direction, true)) return false;
            if (!this.doMoveBack(worldIn, pos, direction, true)) {
                return false;
            }
            worldIn.setBlockState(pos, Blocks.PISTON_HEAD.getDefaultState().with(SHORT, false).with(FACING, direction.getOpposite()).with(TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT), 67);
            worldIn.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, worldIn.rand.nextFloat() * 0.25F + 0.6F);
        } else if (id == 1 || id == 2) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(worldIn, pos, direction, false)) return false;
            BlockPos blockpos = pos.add(direction.getXOffset() * 2, direction.getYOffset() * 2, direction.getZOffset() * 2);
            BlockState blockstate1 = worldIn.getBlockState(blockpos);
            TileEntity tileentity1 = worldIn.getTileEntity(pos.offset(direction));
            if (tileentity1 instanceof PistonTileEntity) {
                ((PistonTileEntity)tileentity1).clearPistonTileEntity();
            }
            BlockState blockstate = Blocks.MOVING_PISTON
                    .getDefaultState()
                    .with(MovingPistonBlock.FACING, direction)
                    .with(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            worldIn.setBlockState(pos, blockstate, 20);
            worldIn.setTileEntity(pos, MovingPistonBlock.createTilePiston(this.getDefaultState().with(FACING, Direction.byIndex(param & 7)),
                    direction, false, true));
            worldIn.updateBlock(pos, blockstate.getBlock());
            blockstate.updateNeighbours(worldIn, pos, 2);
            if (this.isSticky) {
                boolean flag1 = false;
                if (blockstate1.matchesBlock(Blocks.MOVING_PISTON)) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof PistonTileEntity) {
                        PistonTileEntity pistontileentity = (PistonTileEntity)tileentity;
                        if (pistontileentity.getFacing() == direction && pistontileentity.isExtending()) {
                            pistontileentity.clearPistonTileEntity();
                            flag1 = true;
                        }
                    }
                }

                if (!flag1) {
                    if (
                            id != 1
                                    || blockstate1.isAir()
                                    || !canPush(blockstate1, worldIn, blockpos, direction.getOpposite(), false, direction)
                                    || blockstate1.getPushReaction() != PushReaction.NORMAL
                                    && !blockstate1.matchesBlock(Blocks.PISTON)
                                    && !blockstate1.matchesBlock(Blocks.STICKY_PISTON)
                                    && !blockstate1.matchesBlock(RegisterPistons.lightPiston)
                                    && !blockstate1.matchesBlock(RegisterPistons.lightStickyPiston))
                    {
                        worldIn.removeBlock(pos.offset(direction), false);
                    } else {
                        this.doMoveBack(worldIn, pos, direction, false);
                    }
                }
            } else {
                worldIn.removeBlock(pos.offset(direction), false);
            }

            worldIn.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, worldIn.rand.nextFloat() * 0.15F + 0.6F);
        }
        net.minecraftforge.event.ForgeEventFactory.onPistonMovePost(worldIn, pos, direction, (id == 0));
        return true;
    }

    private boolean shouldBeExtended(World worldIn, BlockPos pos, Direction facing) {
        for(Direction direction : Direction.values()) {
            if (direction != facing && worldIn.isSidePowered(pos.offset(direction), direction)) {
                return true;
            }
        }
        if (worldIn.isSidePowered(pos, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockpos = pos.up();
            for(Direction direction1 : Direction.values()) {
                if (direction1 != Direction.DOWN && worldIn.isSidePowered(blockpos.offset(direction1), direction1)) {
                    return true;
                }
            }
            return false;
        }
    }
    private boolean doMove(World worldIn, BlockPos pos, Direction directionIn, boolean extending) {
        BlockPos blockpos = pos.offset(directionIn);
        if (!extending && worldIn.getBlockState(blockpos).matchesBlock(Blocks.PISTON_HEAD)) {
            worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 20);
        }

        PistonBlockStructureHelper pistonblockstructurehelper = new PistonBlockStructureHelper(worldIn, pos, directionIn, extending);
        if (!pistonblockstructurehelper.canMove()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();
            List<BlockPos> list = pistonblockstructurehelper.getBlocksToMove();
            List<BlockState> list1 = Lists.newArrayList();

            for (BlockPos blockpos1 : list) {
                BlockState blockstate = worldIn.getBlockState(blockpos1);
                list1.add(blockstate);
                map.put(blockpos1, blockstate);
            }

            List<BlockPos> list2 = pistonblockstructurehelper.getBlocksToDestroy();
            BlockState[] ablockstate = new BlockState[list.size() + list2.size()];
            Direction direction = extending ? directionIn : directionIn.getOpposite();
            int j = 0;

            for(int k = list2.size() - 1; k >= 0; --k) {
                BlockPos blockpos2 = list2.get(k);
                BlockState blockstate1 = worldIn.getBlockState(blockpos2);
                TileEntity tileentity = blockstate1.hasTileEntity() ? worldIn.getTileEntity(blockpos2) : null;
                spawnDrops(blockstate1, worldIn, blockpos2, tileentity);
                worldIn.setBlockState(blockpos2, Blocks.AIR.getDefaultState(), 18);
                ablockstate[j++] = blockstate1;
            }

            for(int l = list.size() - 1; l >= 0; --l) {
                BlockPos blockpos3 = list.get(l);
                BlockState blockstate5 = worldIn.getBlockState(blockpos3);
                blockpos3 = blockpos3.offset(direction);
                map.remove(blockpos3);
                worldIn.setBlockState(blockpos3, Blocks.MOVING_PISTON.getDefaultState().with(FACING, directionIn), 68);
                worldIn.setTileEntity(blockpos3, MovingPistonBlock.createTilePiston(list1.get(l), directionIn, extending, false));
                ablockstate[j++] = blockstate5;
            }

            if (extending) {
                PistonType pistontype = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
                BlockState blockstate4 = Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, directionIn).with(PistonHeadBlock.TYPE, pistontype);
                BlockState blockstate6 = Blocks.MOVING_PISTON.getDefaultState().with(MovingPistonBlock.FACING, directionIn).with(MovingPistonBlock.TYPE, pistontype);
                map.remove(blockpos);
                worldIn.setBlockState(blockpos, blockstate6, 68);
                worldIn.setTileEntity(blockpos, MovingPistonBlock.createTilePiston(blockstate4, directionIn, true, true));
            }

            BlockState blockstate3 = Blocks.AIR.getDefaultState();

            for(BlockPos blockpos4 : map.keySet()) {
                worldIn.setBlockState(blockpos4, blockstate3, 82);
            }

            for(Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
                BlockPos blockpos5 = entry.getKey();
                BlockState blockstate2 = entry.getValue();
                blockstate2.updateDiagonalNeighbors(worldIn, blockpos5, 2);
                blockstate3.updateNeighbours(worldIn, blockpos5, 2);
                blockstate3.updateDiagonalNeighbors(worldIn, blockpos5, 2);
            }

            j = 0;

            for(int i1 = list2.size() - 1; i1 >= 0; --i1) {
                BlockState blockstate7 = ablockstate[j++];
                BlockPos blockpos6 = list2.get(i1);
                blockstate7.updateDiagonalNeighbors(worldIn, blockpos6, 2);
                worldIn.notifyNeighborsOfStateChange(blockpos6, blockstate7.getBlock());
            }

            for(int j1 = list.size() - 1; j1 >= 0; --j1) {
                worldIn.notifyNeighborsOfStateChange(list.get(j1), ablockstate[j++].getBlock());
            }

            if (extending) {
                worldIn.notifyNeighborsOfStateChange(blockpos, Blocks.PISTON_HEAD);
            }

            return true;
        }
    }
    private boolean doMoveBack(World worldIn, BlockPos pos, Direction directionIn, boolean extending) {
        BlockPos blockpos = pos.offset(directionIn);
        if (!extending && worldIn.getBlockState(blockpos).matchesBlock(Blocks.PISTON_HEAD)) {
            worldIn.setBlockState(blockpos, Blocks.AIR.getDefaultState(), 20);
        }
        PistonBlockStructureHelper pistonblockstructurehelper = new PistonBlockStructureHelper(worldIn, pos, directionIn, extending);
        if (!pistonblockstructurehelper.canMove()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();
            List<BlockPos> list = pistonblockstructurehelper.getBlocksToMove();
            List<BlockState> list1 = Lists.newArrayList();

            for (BlockPos blockpos1 : list) {
                BlockState blockstate = worldIn.getBlockState(blockpos1);
                list1.add(blockstate);
                map.put(blockpos1, blockstate);
            }

            List<BlockPos> list2 = pistonblockstructurehelper.getBlocksToDestroy();
            BlockState[] ablockstate = new BlockState[list.size() + list2.size()];
            Direction direction = extending ? directionIn : directionIn.getOpposite();
            int j = 0;

            for(int k = list2.size() - 1; k >= 0; --k) {
                BlockPos blockpos2 = list2.get(k);
                BlockState blockstate1 = worldIn.getBlockState(blockpos2);
                TileEntity tileentity = blockstate1.hasTileEntity() ? worldIn.getTileEntity(blockpos2) : null;
                spawnDrops(blockstate1, worldIn, blockpos2, tileentity);
                worldIn.setBlockState(blockpos2, Blocks.AIR.getDefaultState(), 18);
                ablockstate[j++] = blockstate1;
            }

            for(int l = list.size() - 1; l >= 0; --l) {
                BlockPos blockpos3 = list.get(l);
                BlockState blockstate5 = worldIn.getBlockState(blockpos3);
                blockpos3 = blockpos3.offset(direction);
                map.remove(blockpos3);
                worldIn.setBlockState(blockpos3, Blocks.MOVING_PISTON.getDefaultState().with(FACING, directionIn), 68);
                worldIn.setTileEntity(blockpos3, MovingPistonBlock.createTilePiston(list1.get(l), directionIn, extending, false));
                ablockstate[j++] = blockstate5;
            }

            if (extending) {
                PistonType pistontype = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
                BlockState blockstate4;
                if (!isSticky) {
                    blockstate4 = RegisterPistons.lightPiston.getDefaultState().with(LightPistonBlock.FACING, directionIn.getOpposite())
                            .with(LightPistonBlock.EXTENDED, true).with(LightPistonBlock.EXTENDEDLIGHT, 1);
                } else {
                    blockstate4 = RegisterPistons.lightStickyPiston.getDefaultState().with(LightPistonBlock.FACING, directionIn.getOpposite())
                            .with(LightPistonBlock.EXTENDED, true).with(LightPistonBlock.EXTENDEDLIGHT, 1);
                }
                BlockState blockstate6 = Blocks.MOVING_PISTON.getDefaultState().with(MovingPistonBlock.FACING, directionIn).with(MovingPistonBlock.TYPE, pistontype);
                map.remove(blockpos);
                worldIn.setBlockState(blockpos, blockstate6, 68);
                worldIn.setTileEntity(blockpos, MovingPistonBlock.createTilePiston(blockstate4, directionIn, true, true));
            }

            BlockState blockstate3 = Blocks.AIR.getDefaultState();

            for(BlockPos blockpos4 : map.keySet()) {
                worldIn.setBlockState(blockpos4, blockstate3, 82);
            }

            for(Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
                BlockPos blockpos5 = entry.getKey();
                BlockState blockstate2 = entry.getValue();
                blockstate2.updateDiagonalNeighbors(worldIn, blockpos5, 2);
                blockstate3.updateNeighbours(worldIn, blockpos5, 2);
                blockstate3.updateDiagonalNeighbors(worldIn, blockpos5, 2);
            }

            j = 0;

            for(int i1 = list2.size() - 1; i1 >= 0; --i1) {
                BlockState blockstate7 = ablockstate[j++];
                BlockPos blockpos6 = list2.get(i1);
                blockstate7.updateDiagonalNeighbors(worldIn, blockpos6, 2);
                worldIn.notifyNeighborsOfStateChange(blockpos6, blockstate7.getBlock());
            }

            for(int j1 = list.size() - 1; j1 >= 0; --j1) {
                worldIn.notifyNeighborsOfStateChange(list.get(j1), ablockstate[j++].getBlock());
            }

            if (extending) {
                worldIn.notifyNeighborsOfStateChange(blockpos, Blocks.PISTON_HEAD);
            }

            return true;
        }
    }
    private void doMoveForward(World worldIn, BlockPos pos, Direction directionIn) {
        BlockPos blockpos = pos.offset(directionIn);
        if (worldIn.getBlockState(blockpos).matchesBlock(Blocks.PISTON_HEAD)) {
            BlockState blockstate4 = RegisterPistons.lightStickyPiston.getDefaultState().with(LightPistonBlock.FACING, directionIn)
                    .with(LightPistonBlock.EXTENDED, false).with(LightPistonBlock.EXTENDEDLIGHT, 0);
            BlockState blockstate6 = Blocks.MOVING_PISTON.getDefaultState().with(MovingPistonBlock.FACING, directionIn.getOpposite()).with(MovingPistonBlock.TYPE, PistonType.STICKY);
            worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 20);
            worldIn.setBlockState(blockpos, blockstate6, 20);
            worldIn.setTileEntity(blockpos, MovingPistonBlock.createTilePiston(blockstate4, directionIn.getOpposite(), false, false));
        }
    }

    private void checkForMove(World worldIn, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        boolean flag1 = this.shouldBeExtended(worldIn, pos, direction);
        boolean flag2 = this.shouldBeExtended(worldIn, pos, direction.getOpposite());
        if (flag1 && (state.get(EXTENDEDLIGHT) == 0) && new PistonBlockStructureHelper(worldIn, pos, direction, true).canMove()) {
            worldIn.setBlockState(pos, state.with(EXTENDEDLIGHT, 1).with(EXTENDED, Boolean.TRUE), 2);
            worldIn.addBlockEvent(pos, this, 0, direction.getIndex());
        } else if (flag2 && (state.get(EXTENDEDLIGHT) == 0) && new PistonBlockStructureHelper(worldIn, pos, direction.getOpposite(), true).canMove()) {
            worldIn.setBlockState(pos, state.with(EXTENDEDLIGHT, 2).with(EXTENDED, Boolean.TRUE), 2);
            worldIn.addBlockEvent(pos, this, 3, direction.getOpposite().getIndex());
        } else {
            if (!flag1 && (state.get(EXTENDEDLIGHT) == 1)) {
                BlockPos blockpos = pos.offset(direction, 2);
                BlockState blockstate = worldIn.getBlockState(blockpos);
                int i = 1;
                if (blockstate.matchesBlock(Blocks.MOVING_PISTON) && blockstate.get(FACING) == direction) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof PistonTileEntity) {
                        PistonTileEntity pistontileentity = (PistonTileEntity) tileentity;
                        if (pistontileentity.isExtending() && (pistontileentity.getProgress(0.0F) < 0.5F || worldIn.getGameTime() == pistontileentity.getLastTicked() || ((ServerWorld) worldIn).isInsideTick())) {
                            i = 2;
                        }
                    }
                }
                worldIn.addBlockEvent(pos, this, i, direction.getIndex());
            }
            if (!flag2 && state.get(EXTENDEDLIGHT) == 2) {
                BlockPos blockpos = pos.offset(direction, 2);
                BlockState blockstate = worldIn.getBlockState(blockpos);
                int i = 4;
                if (blockstate.matchesBlock(Blocks.MOVING_PISTON) && blockstate.get(FACING) == direction) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof PistonTileEntity) {
                        PistonTileEntity pistontileentity = (PistonTileEntity)tileentity;
                        if (pistontileentity.isExtending() && (pistontileentity.getProgress(0.0F) < 0.5F || worldIn.getGameTime() == pistontileentity.getLastTicked() || ((ServerWorld)worldIn).isInsideTick())) {
                            i = 5;
                        }
                    }
                }
                worldIn.addBlockEvent(pos, this, i, direction.getIndex());
            }
        }
    }
    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.matchesBlock(state.getBlock())) {
            if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
                this.checkForMove(worldIn, pos, state);
            }
        }
    }
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!worldIn.isRemote) {
            this.checkForMove(worldIn, pos, state);
        }
    }
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!worldIn.isRemote) {
            this.checkForMove(worldIn, pos, state);
        }
    }
}
