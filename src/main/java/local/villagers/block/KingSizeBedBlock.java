package local.villagers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Two independent sleeping lanes forming one placeable 2 x 2 bed. */
public final class KingSizeBedBlock extends BedBlock {
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public KingSizeBedBlock(Properties properties) {
        super(DyeColor.RED, properties);
        registerDefaultState(defaultBlockState().setValue(RIGHT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RIGHT);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        Direction facing = state.getValue(FACING);
        BlockPos origin = context.getClickedPos();
        for (BlockPos pos : new BlockPos[]{origin, origin.relative(facing), origin.relative(facing.getClockWise()),
                origin.relative(facing).relative(facing.getClockWise())}) {
            if (!context.getLevel().isLoaded(pos) || !context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context)
                    || !context.getLevel().getBlockState(pos.below()).isFaceSturdy(context.getLevel(), pos.below(), Direction.UP)) return null;
        }
        return state;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Install every part before notifying neighbours, so partial placement cannot dismantle the bed.
        Direction facing = state.getValue(FACING);
        BlockPos right = pos.relative(facing.getClockWise());
        level.setBlock(pos.relative(facing), state.setValue(PART, BedPart.HEAD), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        level.setBlock(right, state.setValue(RIGHT, true), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        level.setBlock(right.relative(facing), state.setValue(RIGHT, true).setValue(PART, BedPart.HEAD), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        for (BlockPos part : new BlockPos[]{pos, pos.relative(facing), right, right.relative(facing)})
            level.updateNeighborsAt(part, this);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()) {
            Direction facing = state.getValue(FACING);
            BlockPos leftFoot = (state.getValue(PART) == BedPart.HEAD ? pos.relative(facing.getOpposite()) : pos);
            if (state.getValue(RIGHT)) leftFoot = leftFoot.relative(facing.getCounterClockWise());
            for (int lane = 0; lane < 2; lane++) for (int part = 0; part < 2; part++) {
                BlockPos other = leftFoot.relative(facing.getClockWise(), lane).relative(facing, part);
                if (!other.equals(pos) && level.getBlockState(other).is(this))
                    level.setBlock(other, Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction side, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        Direction across = state.getValue(RIGHT) ? state.getValue(FACING).getCounterClockWise() : state.getValue(FACING).getClockWise();
        if (side == across && (!neighbour.is(this) || neighbour.getValue(RIGHT) == state.getValue(RIGHT)
                || neighbour.getValue(FACING) != state.getValue(FACING) || neighbour.getValue(PART) != state.getValue(PART)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, side, neighbourPos, neighbour, random);
    }
}
