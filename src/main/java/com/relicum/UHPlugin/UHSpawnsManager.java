/**
 *  Plugin UltraHardcore Reloaded (UHPlugin)
 *  Copyright (C) 2013 azenet
 *  Copyright (C) 2014-2015 Amaury Carrade
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see [http://www.gnu.org/licenses/].
 */

package com.relicum.UHPlugin;

import com.relicum.UHPlugin.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class UHSpawnsManager {

    UHPlugin p = null;
    I18n i = null;

    private LinkedList<Location> spawnPoints = new LinkedList<Location>();

    private boolean avoidWater;

    public UHSpawnsManager(UHPlugin plugin) {
        this.p = plugin;
        this.i = p.getI18n();

        avoidWater = p.getConfig().getBoolean("map.spawnPoints.dontGenerateAboveWater");
    }

    /**
     * Adds a spawn point at (x;z) in the default world.
     *
     * @param x The X coordinate.
     * @param z The Z coordinate.
     */
    public void addSpawnPoint(final Double x, final Double z) {
        addSpawnPoint(p.getServer().getWorlds().get(0), x, z);
    }

    /**
     * Adds a spawn point at (x;z) in the given world.
     *
     * @param world The world.
     * @param x     The X coordinate.
     * @param z     The Z coordinate.
     */
    public void addSpawnPoint(final World world, final Double x, final Double z) {
        addSpawnPoint(new Location(world, x, 0, z));
    }

    /**
     * Adds a spawn point from a location.
     *
     * @param location The location. Cloned, so you can use the same location object with
     *                 modifications between two calls.
     * @throws RuntimeException         If the spawn point is in the Nether and no safe spot was found.
     * @throws IllegalArgumentException If the spawn point is out of the current border.
     */
    public void addSpawnPoint(final Location location) {
        Location spawnPoint = location.clone();

        // Initial fall, except in the nether.
        if (!(spawnPoint.getWorld().getEnvironment() == Environment.NETHER)) {
            spawnPoint.setY(location.getWorld().getHighestBlockYAt(location.getBlockX(), location.getBlockZ()) + 120);
        } else {
            Location safeSpot = UHUtils.searchSafeSpot(location);
            if (safeSpot == null) {
                throw new RuntimeException("Unable to find a safe spot to set the spawn point " + location.toString());
            }

            spawnPoint.setY(safeSpot.getY());
        }

        if (!p.getBorderManager().isInsideBorder(spawnPoint)) {
            throw new IllegalArgumentException("The given spawn location is outside the current border");
        }

        spawnPoints.add(spawnPoint);
    }

    /**
     * Returns the registered spawn points.
     *
     * @return The spawn points.
     */
    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    /**
     * Removes all spawn points with the same coordinates as the given location object
     * (X, Z, world).
     *
     * @param location The location to be removed.
     * @param precise  If true, only the spawn points at the exact same location will be removed.
     *                 Else, the points in the same block.
     * @return true if something were removed.
     */
    public boolean removeSpawnPoint(Location location, boolean precise) {
        List<Location> toRemove = new LinkedList<Location>();

        for (Location spawn : getSpawnPoints()) {
            if (location.getWorld().equals(spawn.getWorld())) {
                if (precise
                      && location.getX() == spawn.getX()
                      && location.getZ() == spawn.getZ()) {
                    toRemove.add(spawn);
                } else if (!precise
                             && location.getBlockX() == spawn.getBlockX()
                             && location.getBlockZ() == spawn.getBlockZ()) {
                    toRemove.add(spawn);
                }
            }
        }

        for (Location spawnToRemove : toRemove) {
            while (spawnPoints.remove(spawnToRemove)) ; // Used to remove all occurrences of the spawn point
        }

        return toRemove.size() != 0;
    }

    /**
     * Removes all registered spawn points.
     * <p/>
     * CANNOT BE CANCELLED.
     */
    public void reset() {
        spawnPoints = new LinkedList<Location>();
    }

    /**
     * Imports spawn points from the configuration.
     *
     * @return The number of spawn points imported.
     */
    public int importSpawnPointsFromConfig() {
        if (p.getConfig().getList("spawnpoints") != null) {
            int spawnCount = 0;
            for (Object position : p.getConfig().getList("spawnpoints")) {
                if (position instanceof String && position != null) {
                    String[] coords = ((String) position).split(",");
                    try {
                        addSpawnPoint(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
                        p.getLogger().info(i.t("load.spawnPointAdded", coords[0], coords[1]));
                        spawnCount++;
                    } catch (Exception e) { // Not an integer or not enough coords
                        p.getLogger().warning(i.t("load.invalidSpawnPoint", (String) position));
                    }
                }
            }

            return spawnCount;
        }

        return 0;
    }

    /**
     * Generates a random number between min and max.
     *
     * @param min The minimum value. May be negative. Inclusive.
     * @param max The maximum value. May be negative. Inclusive.
     * @return A random number between these two points.
     */
    public Integer random(int min, int max) {
        if (min == max) {
            return min;
        }

        Random rand = new Random();

        if (min > max) { // swap
            min = min + max;
            max = min - max;
            min = min - max;
        }

        if (min >= 0 && max >= 0) {
            return rand.nextInt(max - min + 1) + min;
        } else if (min <= 0 && max <= 0) {
            return -1 * (rand.nextInt(Math.abs(min - max)) + Math.abs(max));
        } else { // min <= 0 && max >= 0
            return rand.nextInt(Math.abs(min) + Math.abs(max)) - Math.abs(min);
        }
    }

    /**
     * Generates randomly some spawn points in the map, with a minimal distance.
     *
     * @param world                           The world where the spawn points will be generated.
     * @param spawnCount                      The number of spawn points to generate.
     * @param regionDiameter                  The diameter of the region where the spawn points will be generated.<br>
     *                                        This is limited by the size of the map. This will be seen as the diameter of a circular or
     *                                        of a squared map, following the shape of the world set in the configuration.
     * @param minimalDistanceBetweenTwoPoints The minimal distance between two points.
     * @param xCenter                         The x coordinate of the point in the center of the region where the points will be generated.
     * @param zCenter                         The z coordinate of the point in the center of the region where the points will be generated.
     * @return False if there's too many spawn points / not enough surface to generate them.
     * True if the generation succeeded.
     */
    public boolean generateRandomSpawnPoints(World world, int spawnCount, int regionDiameter, int minimalDistanceBetweenTwoPoints, double xCenter, double zCenter) {

        /** Possible? **/

        // If the surface of the map is too close of the sum of the surfaces of the private part
        // around each spawn point (a circle with, as radius, the minimal distance between two spawn
        // points), the generation will fail.

        double surfacePrivatePartsAroundSpawnPoints = (int) (spawnCount * (Math.PI * Math.pow(minimalDistanceBetweenTwoPoints, 2)));
        double surfaceRegion;
        if (p.getBorderManager().isCircularBorder()) {
            surfaceRegion = (Math.PI * Math.pow(regionDiameter, 2)) / 4;
        } else {
            surfaceRegion = Math.pow(regionDiameter, 2);
        }

        Double packingDensity = surfacePrivatePartsAroundSpawnPoints / surfaceRegion;

        // According to Lagrange and Thue's works on circle packagings, the highest density possible is
        // approximately 0.9069 (with an hexagonal arrangement of the circles).
        // Even with a packaging density very close to this limit, the generation time is correct.
        // So we uses this as a limit.
        if (packingDensity >= 0.9069) {
            return false;
        }

        /** Generation **/

        LinkedList<Location> randomSpawnPoints = new LinkedList<Location>();
        int generatedSpawnPoints = 0;

        // If the first points are badly located, and if the density is high, the generation may
        // be impossible to end.
        // So, after 15 generation fails of a point due to this point being placed too close
        // of other ones, we restarts all the generation.
        int currentErrorCount = 0;

        // With the "avoid above water" option, if there's a lot of water, the genaration may
        // fail even if the surface seems to be ok to host the requested spawn points.
        // So, after 2*{points requested} points above the water, we cancels the generation.
        int pointsAboveWater = 0;

        generationLoop:
        while (generatedSpawnPoints != spawnCount) {

            // "Too many fails" test
            if (currentErrorCount >= 16) { // restart
                randomSpawnPoints = new LinkedList<Location>();
                generatedSpawnPoints = 0;
                currentErrorCount = 0;
            }

            // "Too many points above the water" test
            if (pointsAboveWater >= 2 * spawnCount) {
                return false;
            }

            // We generates a point in the square of side regionDiameter.
            // In case of a circular world, if the point was generated out of the circle, it will be
            // excluded when his presence inside the region will be checked.

            Location randomPoint = new Location(world,
                                                 random((int) (xCenter - Math.floor(regionDiameter / 2)), (int) (xCenter + (int) Math.floor(regionDiameter / 2))),
                                                 0,
                                                 random((int) (zCenter - Math.floor(regionDiameter / 2)), (int) (zCenter + (int) Math.floor(regionDiameter / 2))));

            // Inside the region?
            if (!p.getBorderManager().isInsideBorder(randomPoint, regionDiameter)) {
                continue generationLoop; // outside: nope
            }

            Block surfaceBlock = world.getHighestBlockAt(randomPoint);

            // Safe spot available?
            if (!UHUtils.isSafeSpot(surfaceBlock.getLocation())) {
                continue generationLoop; // not safe: nope
            }

            // Not above the water?
            if (avoidWater) {
                if (surfaceBlock.getType() == Material.WATER || surfaceBlock.getType() == Material.STATIONARY_WATER) {
                    pointsAboveWater++;
                    continue generationLoop;
                }
            }

            // Is that point at a correct distance of the other ones?
            for (Location spawn : randomSpawnPoints) {
                if (spawn.distance(randomPoint) < minimalDistanceBetweenTwoPoints) {
                    currentErrorCount++;
                    continue generationLoop; // too close: nope
                }
            }

            // Well, all done.
            randomSpawnPoints.add(randomPoint);
            generatedSpawnPoints++;
            currentErrorCount = 0;
        }

        // Generation done, let's register these points.

        for (Location spawn : randomSpawnPoints) {
            addSpawnPoint(spawn);
        }

        return true;
    }

    /**
     * Generates spawn points in a grid.
     *
     * @param world                           The world where the spawn points will be generated.
     * @param spawnCount                      The number of spawn points to generate.
     * @param regionDiameter                  The diameter of the region where the spawn points will be generated.<br>
     *                                        This is limited by the size of the map. This will be seen as the diameter of a circular or
     *                                        of a squared map, following the shape of the world set in the configuration.
     * @param minimalDistanceBetweenTwoPoints The minimal distance between two points.
     * @param xCenter                         The x coordinate of the point in the center of the region where the points will be generated.
     * @param zCenter                         The z coordinate of the point in the center of the region where the points will be generated.
     * @return False if there's too many spawn points / not enough surface to generate them.
     * True if the generation succeeded.
     */
    public boolean generateGridSpawnPoints(World world, int spawnCount, int regionDiameter, int minimalDistanceBetweenTwoPoints, double xCenter, double zCenter) {

        // We starts the generation on a smaller grid, to avoid false outside tests if the point is on the edge
        int usedRegionDiameter = regionDiameter - 1;

        // To check the possibility of the generation, we calculates the maximal number of columns/rows
        // possible, based on the minimal distance between two points.
        int maxColumnsCount = (int) Math.ceil(usedRegionDiameter / minimalDistanceBetweenTwoPoints);

        // The points are on a grid
        int neededColumnsCount = (int) Math.ceil(Math.sqrt(spawnCount));
        if (p.getBorderManager().isCircularBorder()) {
            // If the border is circular, the distance between two points needs to be decreased.
            // The space available is divided by PI/4, so the column count is multiplied by
            // this number.
            neededColumnsCount = (int) Math.ceil(neededColumnsCount / (Math.PI / 4));
        }

        // IS impossible.
        if (neededColumnsCount > maxColumnsCount) {
            return false;
        }
        // If the map is circular, the generation may be impossible, because this check was
        // performed for a squared map.
        // The test will be done after the generation.

        // We generates the points on a grid in squares, starting by the biggest square.
        int distanceBetweenTwoPoints = (int) (Double.valueOf(usedRegionDiameter) / (Double.valueOf(neededColumnsCount - 1)));

        // Check related to the case the column count was increased.
        if (distanceBetweenTwoPoints < minimalDistanceBetweenTwoPoints) {
            return false;
        }

        int countGeneratedPoints = 0;
        LinkedList<Location> generatedPoints = new LinkedList<Location>();

        int halfDiameter = (int) Math.floor(usedRegionDiameter / 2);

        Integer currentSquareSize = usedRegionDiameter;
        Location currentSquareStartPoint = new Location(world, xCenter + halfDiameter, 0, zCenter - halfDiameter);
        Location currentPoint;

        // Represents the location to add on each side of the squares
        Location[] addOnSide = new Location[4];
        addOnSide[0] = new Location(world, -distanceBetweenTwoPoints, 0, 0); // North side, direction east
        addOnSide[1] = new Location(world, 0, 0, distanceBetweenTwoPoints);  // East side,  direction south
        addOnSide[2] = new Location(world, distanceBetweenTwoPoints, 0, 0);  // South side, direction west
        addOnSide[3] = new Location(world, 0, 0, -distanceBetweenTwoPoints); // West side,  direction north

        // We generates the points until there isn't any point left to place. The loop will be broken.
        // On each step of this loop, a square is generated.
        generationLoop:
        while (true) {
            currentPoint = currentSquareStartPoint.clone();

            // First point
            if (p.getBorderManager().isInsideBorder(currentPoint, regionDiameter) && UHUtils.searchSafeSpot(currentPoint) != null) {
                generatedPoints.add(currentPoint.clone());
                countGeneratedPoints++;

                if (countGeneratedPoints >= spawnCount) {
                    break generationLoop;
                }
            }

            for (int j = 0; j < 4; j++) { // A step for each side, j is the side (see addOnSide).
                int plottedSize = 0;

                sideLoop:
                while (plottedSize < currentSquareSize) {
                    currentPoint.add(addOnSide[j]);
                    plottedSize += distanceBetweenTwoPoints;

                    // Inside the border?
                    if (!p.getBorderManager().isInsideBorder(currentPoint, regionDiameter)) {
                        continue sideLoop;
                    }

                    Block surfaceBlock = world.getHighestBlockAt(currentPoint);

                    // Safe spot available?
                    if (!UHUtils.isSafeSpot(surfaceBlock.getLocation())) {
                        continue sideLoop; // not safe: nope
                    }

                    // Not above the water?
                    if (avoidWater) {
                        if (surfaceBlock.getType() == Material.WATER || surfaceBlock.getType() == Material.STATIONARY_WATER) {
                            continue sideLoop;
                        }
                    }

                    generatedPoints.add(currentPoint.clone());
                    countGeneratedPoints++;

                    if (countGeneratedPoints >= spawnCount) {
                        break generationLoop;
                    }
                }
            }

            // This square is complete; preparing the next one...
            currentSquareSize -= 2 * distanceBetweenTwoPoints;
            currentSquareStartPoint.add(new Location(world, -distanceBetweenTwoPoints, 0, distanceBetweenTwoPoints));

            if (currentSquareSize < distanceBetweenTwoPoints) {
                // This may happens if we generates the points for a circular world
                break generationLoop;
            }
        }

        // If the generation was broken (circular world, not enough positions),
        // the generation was incomplete.
        if (countGeneratedPoints >= spawnCount) {
            // Generation OK
            for (Location spawn : generatedPoints) {
                addSpawnPoint(spawn);
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Generates spawn points in concentric circles.
     *
     * @param world                           The world where the spawn points will be generated.
     * @param spawnCount                      The number of spawn points to generate.
     * @param regionDiameter                  The diameter of the region where the spawn points will be generated.<br>
     *                                        This is limited by the size of the map. This will be seen as the diameter of a circular or
     *                                        of a squared map, following the shape of the world set in the configuration.
     * @param minimalDistanceBetweenTwoPoints The minimal distance between two points.
     * @param xCenter                         The x coordinate of the point in the center of the region where the points will be generated.
     * @param zCenter                         The z coordinate of the point in the center of the region where the points will be generated.
     * @return False if there's too many spawn points / not enough surface to generate them.
     * True if the generation succeeded.
     */
    public boolean generateCircularSpawnPoints(World world, int spawnCount, int regionDiameter, int minimalDistanceBetweenTwoPoints, double xCenter, double zCenter) {

        // We starts the generation on a smaller grid, to avoid false outside tests if the point is on the edge
        int usedRegionDiameter = regionDiameter - 1;

        int countGeneratedPoints = 0;
        LinkedList<Location> generatedPoints = new LinkedList<Location>();

        int currentCircleDiameter = usedRegionDiameter;

        // The generation loop. Each step generates a circle.
        generationLoop:
        while (currentCircleDiameter >= minimalDistanceBetweenTwoPoints) {
            // First step. We want to know if all the points left can be in one circle.
            // We calculates the maximal number of points in a circle, taking into account the
            // minimal distance between two points.

            // The link between the angle between two points and the fly distance between them
            // is, where R is the radius, d the fly distance, and a the angle:
            // a = 2 Arcsin((d/2)/R)
            // (Just draw the situation, you'll see.)

            double denseCircleAngle = 2 * Math.asin((Double.valueOf(minimalDistanceBetweenTwoPoints) / 2) / (Double.valueOf(currentCircleDiameter) / 2));
            int pointsPerDenseCircles = (int) Math.floor(2 * Math.PI / denseCircleAngle);

            double angleBetweenTwoPoints;

            // Not all the points can be in this circle. We generate the densiest circle.
            if (pointsPerDenseCircles < spawnCount - countGeneratedPoints) {
                angleBetweenTwoPoints = 2 * Math.PI / (Double.valueOf(pointsPerDenseCircles));
            }
            // All the remaining points can be in this circle. We generates the less dense circle with
            // these points.
            else {
                angleBetweenTwoPoints = 2 * Math.PI / (Double.valueOf(spawnCount - countGeneratedPoints));
            }

            // Let's generate these points.
            double startAngle = (new Random()).nextDouble() * 2 * Math.PI;
            double currentAngle = startAngle;

            circleLoop:
            while (currentAngle <= 2 * Math.PI - angleBetweenTwoPoints + startAngle) {
                // The coordinates of a point in the circle.
                // Cf. your trigonometry! ;)
                Location point = new Location(
                                               world,
                                               (currentCircleDiameter / 2) * Math.cos(currentAngle) + xCenter,
                                               0,
                                               (currentCircleDiameter / 2) * Math.sin(currentAngle) + zCenter
                );

                currentAngle += angleBetweenTwoPoints;

                if (!p.getBorderManager().isInsideBorder(point, regionDiameter)) { // Just in case
                    continue circleLoop;
                }

                Block surfaceBlock = world.getHighestBlockAt(point);

                // Safe spot available?
                if (!UHUtils.isSafeSpot(surfaceBlock.getLocation())) {
                    continue circleLoop; // not safe: nope
                }

                // Not above the water?
                if (avoidWater) {
                    if (surfaceBlock.getType() == Material.WATER || surfaceBlock.getType() == Material.STATIONARY_WATER) {
                        continue circleLoop;
                    }
                }

                generatedPoints.add(point);
                countGeneratedPoints++;

                if (countGeneratedPoints >= spawnCount) {
                    break generationLoop;
                }
            }

            // So, this circle is done.
            // We prepares the next one.
            currentCircleDiameter -= 2 * minimalDistanceBetweenTwoPoints;
        }

        // Generation done or failed (not enough space)?
        if (generatedPoints.size() < spawnCount) {
            return false; // Failed!
        } else {
            for (Location spawn : generatedPoints) {
                addSpawnPoint(spawn);
            }

            return true;
        }
    }
}
