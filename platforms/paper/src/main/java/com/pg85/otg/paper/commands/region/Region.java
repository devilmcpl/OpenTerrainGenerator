package com.pg85.otg.paper.commands.region;

import com.pg85.otg.customobject.util.Corner;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

/** A region selected with /otg region or WorldEdit, used by /otg export and /otg edit. */
@Getter
public class Region {
    private BlockPos pos1 = null;
    private BlockPos pos2 = null;
    @Setter
    private Corner center = null;
    private boolean flip = true;

    // Returns whether it set pos1 or not
    public boolean setPos(BlockPos blockPos) {
        // alternate between setting min and max
        // Flip initializes as true, meaning we set min first
        if (this.flip) {
            this.pos1 = blockPos;
        } else {
            this.pos2 = blockPos;
        }
        this.flip = !this.flip;
        return !this.flip;
    }

    public static Corner cornerFromBlockPos(BlockPos blockPos) {
        return new Corner(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public void clear() {
        this.pos1 = null;
        this.pos2 = null;
        this.center = null;
    }

    public Corner getMin() {
        if (this.pos1 == null || this.pos2 == null) {
            return null;
        }
        return new Corner(
            Math.min(this.pos1.getX(), this.pos2.getX()),
            Math.min(this.pos1.getY(), this.pos2.getY()),
            Math.min(this.pos1.getZ(), this.pos2.getZ())
        );
    }

    public Corner getMax() {
        if (this.pos1 == null || this.pos2 == null) {
            return null;
        }
        return new Corner(
            Math.max(this.pos1.getX(), this.pos2.getX()),
            Math.max(this.pos1.getY(), this.pos2.getY()),
            Math.max(this.pos1.getZ(), this.pos2.getZ())
        );
    }

    public void setPos1(BlockPos pos) {
        this.flip = false;
        this.pos1 = pos;
    }

    public void setPos2(BlockPos pos) {
        this.flip = true;
        this.pos2 = pos;
    }
}
