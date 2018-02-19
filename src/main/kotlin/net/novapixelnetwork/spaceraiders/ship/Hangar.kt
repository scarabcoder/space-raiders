package net.novapixelnetwork.spaceraiders.ship

import com.boydti.fawe.`object`.schematic.Schematic
import com.sk89q.worldedit.Vector
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import net.novapixelnetwork.gamecore.sql.Connections
import net.novapixelnetwork.spaceraiders.data.DataFolders
import net.novapixelnetwork.spaceraiders.data.DataManager
import net.novapixelnetwork.spaceraiders.player.SRPlayer
import net.novapixelnetwork.spaceraiders.world.Planet
import org.bukkit.Location
import java.io.File
import java.sql.SQLException
import java.sql.Statement
import java.util.*

/**
 * Created by owner on 1/5/2018.
 */
class Hangar(val id: Int, val center: Location, val size: Size, val owner: UUID, val shipID: Int, val autoGenerated: Boolean, val planet: Planet?) {

    init {
        size.getSchematic().paste(BukkitWorld(center.world), Vector(center.blockX, center.blockY, center.blockZ), false, true, null)
    }

    val ship: Ship by lazy {
        DataManager.getShip(shipID)!!
    }

    companion object {

        private fun findNextGeneratedHangarLocation(): Location {
            throw UnsupportedOperationException("Function needs to be created!")
        }

        val generatedSchem: Schematic = ClipboardFormat.SCHEMATIC.load(File(DataFolders.hangars, "defaultGenerated.schematic"))

        fun placeAndGenerate(location: Location?, size: Size, planet: Planet?, autoGenerated: Boolean, player: SRPlayer): Hangar {
            val c = Connections.grabConnection()
            try {
                val hangarID = DataManager.IDGen.genHangarID
                val shipID = DataManager.IDGen.genShipID
                var ps = c.prepareStatement("INSERT INTO hangars (id, x, y, z, planet, size, ship, auto_generated, owner) (id, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
                val loc = if(autoGenerated) location!! else findNextGeneratedHangarLocation()
                ps.setInt(1, hangarID)
                ps.setInt(2, loc.blockX)
                ps.setInt(3, loc.blockY)
                ps.setInt(4, loc.blockZ)
                ps.setInt(5, if(autoGenerated) -1 else planet!!.id)
                ps.setString(6, size.name)
                ps.setInt(7, shipID)
                ps.setBoolean(8, autoGenerated)
                ps.setString(9, player.uuid.toString())
                ps.executeUpdate()

                val hangar = Hangar(hangarID, loc, size, player.uuid, shipID, autoGenerated, planet)
                DataManager.addToCache(hangar)

                ps = c.prepareStatement("INSERT INTO ships (id, owner, engine, hull) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
                ps.setInt(1, shipID)
                ps.setString(2, player.uuid.toString())
                ps.setString(3, Engine.getDefault(size).nameID)
                ps.setString(4, Hull.getDefault(size).nameID)
                ps.executeUpdate()
                var rs = ps.generatedKeys
                val ship = Ship(shipID, hangar, size, player.uuid, null, Hull.getDefault(size), Engine.getDefault(size))
                DataManager.addToCache(ship)
                return hangar

            } finally {
                c.close()
            }

        }

        fun autoGenerate(player: SRPlayer, size: Hangar.Size): Hangar {
            return placeAndGenerate(null, size, null, true, player)
        }

        fun createTable(): String{
            return "CREATE TABLE IF NOT EXISTS hangars (" +
                    "id INTEGER NOT NULL, " +
                    "x INT NOT NULL, " +
                    "y INT NOT NULL, " +
                    "z INT NOT NULL, " +
                    "planet INT, " +
                    "size VARCHAR(12) NOT NULL, " +
                    "auto_generated TINYINT(1) NOT NULL, " +
                    "owner VARCHAR(36) NOT NULL, " +
                    "PRIMARY KEY (`id`));"
        }
    }

    enum class Size(val sizeName: String) {




        SMALL("Scout"), MEDIUM("Fighter"), LARGE("Destroyer");

        override fun toString(): String {
            return sizeName
        }


        fun getSchematic(): Schematic {
            return ClipboardFormat.SCHEMATIC.load(File(DataFolders.hangars, this.name.toLowerCase() + ".schematic"))
        }
    }
}