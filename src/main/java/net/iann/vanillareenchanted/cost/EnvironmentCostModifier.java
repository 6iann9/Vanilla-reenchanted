package net.iann.vanillareenchanted.cost;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EnvironmentCostModifier {

    private static final int SCAN_RADIUS = 5;

    private static final int XP_DISCOUNT_PER_CANDLE_PERCENT = 5;
    private static final int MAX_XP_DISCOUNT_PERCENT = 25;

    private static final int LAPIS_DISCOUNT_PER_HEAD_PERCENT = 10;
    private static final int MAX_LAPIS_DISCOUNT_PERCENT = 30;

    public static int applyCandleResearchDiscount(
            Level level,
            BlockPos tablePos,
            int costAfterBookDiscount
    ) {
        if (costAfterBookDiscount <= 0) {
            return 0;
        }

        int candleCount = countCandles(level, tablePos);

        int discountPercent = Math.min(
                candleCount * XP_DISCOUNT_PER_CANDLE_PERCENT,
                MAX_XP_DISCOUNT_PERCENT
        );

        return applyPercentDiscountKeepingMinimumOne(
                costAfterBookDiscount,
                discountPercent
        );
    }

    public static int applyMobHeadEnchantDiscount(
            Level level,
            BlockPos tablePos,
            int costAfterBookDiscount
    ) {
        if (costAfterBookDiscount <= 0) {
            return 0;
        }

        int mobHeadCount = countMobHeads(level, tablePos);

        int discountPercent = Math.min(
                mobHeadCount * LAPIS_DISCOUNT_PER_HEAD_PERCENT,
                MAX_LAPIS_DISCOUNT_PERCENT
        );

        return applyPercentDiscountKeepingMinimumOne(
                costAfterBookDiscount,
                discountPercent
        );
    }

    public static int getCandleResearchDiscountPercent(
            Level level,
            BlockPos tablePos
    ) {
        int candleCount = countCandles(level, tablePos);

        return Math.min(
                candleCount * XP_DISCOUNT_PER_CANDLE_PERCENT,
                MAX_XP_DISCOUNT_PERCENT
        );
    }

    public static int getMobHeadEnchantDiscountPercent(
            Level level,
            BlockPos tablePos
    ) {
        int mobHeadCount = countMobHeads(level, tablePos);

        return Math.min(
                mobHeadCount * LAPIS_DISCOUNT_PER_HEAD_PERCENT,
                MAX_LAPIS_DISCOUNT_PERCENT
        );
    }

    private static int countCandles(
            Level level,
            BlockPos tablePos
    ) {
        int candleCount = 0;

        for (BlockPos currentPos : BlockPos.betweenClosed(
                tablePos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                tablePos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)
        )) {
            BlockState blockState = level.getBlockState(currentPos);

            if (!blockState.is(BlockTags.CANDLES)) {
                continue;
            }

            if (blockState.hasProperty(CandleBlock.CANDLES)) {
                candleCount += blockState.getValue(CandleBlock.CANDLES);
            } else {
                candleCount++;
            }
        }

        return candleCount;
    }

    private static int countMobHeads(
            Level level,
            BlockPos tablePos
    ) {
        int mobHeadCount = 0;

        for (BlockPos currentPos : BlockPos.betweenClosed(
                tablePos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                tablePos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)
        )) {
            BlockState blockState = level.getBlockState(currentPos);

            if (blockState.getBlock() instanceof SkullBlock
                    || blockState.getBlock() instanceof WallSkullBlock) {

                mobHeadCount++;
            }
        }

        return mobHeadCount;
    }

    private static int applyPercentDiscountKeepingMinimumOne(
            int cost,
            int discountPercent
    ) {
        if (cost <= 0) {
            return 0;
        }

        if (discountPercent <= 0) {
            return cost;
        }

        int discountAmount = cost * discountPercent / 100;
        int discountedCost = cost - discountAmount;

        return Math.max(1, discountedCost);
    }
}