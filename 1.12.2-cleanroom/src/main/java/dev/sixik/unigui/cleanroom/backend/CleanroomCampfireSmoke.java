package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.backend.minecraft_impl.LegacyCampfireSmoke;

/**
 * Seekable campfire-style smoke for the Cleanroom Furnace floor.
 */
public final class CleanroomCampfireSmoke extends LegacyCampfireSmoke {
    /**
     * @param start inclusive time of the first birth in seconds
     * @param end   exclusive end of emission in seconds
     * @param rate  number of births per second
     * @throws IllegalArgumentException if the interval or rate is invalid
     */
    public CleanroomCampfireSmoke(double start, double end, double rate) {
        super(start, end, rate);
    }
}
