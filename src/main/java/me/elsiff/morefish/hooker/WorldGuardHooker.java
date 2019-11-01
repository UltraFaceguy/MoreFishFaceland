package me.elsiff.morefish.hooker;

import static com.sk89q.worldedit.math.BlockVector3.at;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.internal.platform.StringMatcher;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

/**
 * Created by elsiff on 2017-06-20.
 */
public class WorldGuardHooker {

  private RegionContainer regionContainer = WorldGuard.getInstance().getPlatform()
      .getRegionContainer();
  private StringMatcher stringMatcher = WorldGuard.getInstance().getPlatform().getMatcher();

  public boolean containsLocation(Location loc, String regionId) {
    BlockVector3 vectorLoc = at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    World world = stringMatcher.getWorldByName(loc.getWorld().getName());
    RegionManager manager = regionContainer.get(world);
    ApplicableRegionSet regions = manager.getApplicableRegions(vectorLoc);
    for (ProtectedRegion region : regions.getRegions()) {
      if (regionId.equals(region.getId())) {
        return true;
      }
    }
    return false;
  }
}
