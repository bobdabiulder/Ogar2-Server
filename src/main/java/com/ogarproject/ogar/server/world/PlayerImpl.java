/**
 * This file is part of Ogar.
 *
 * Ogar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ogar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ogar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ogarproject.ogar.server.world;

import com.ogarproject.ogar.api.Ogar;
import com.ogarproject.ogar.api.Player;
import com.ogarproject.ogar.server.net.PlayerConnection;
import com.ogarproject.ogar.server.entity.impl.CellEntityImpl;
import com.ogarproject.ogar.server.net.packet.outbound.PacketOutAddNode;
import com.ogarproject.ogar.server.net.packet.universal.PacketOMPMessage;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class PlayerImpl implements Player {

    private final PlayerConnection playerConnection;
    private final Set<CellEntityImpl> cells = new HashSet<>();
    private final PlayerTracker tracker;
    private String name;
    private boolean ompCapable;

    public PlayerImpl(Channel channel) {
        this.playerConnection = new PlayerConnection(this, channel);
        this.tracker = new PlayerTracker(this);
    }

    public SocketAddress getAddress() {
        return this.playerConnection.getRemoteAddress();
    }

    public PlayerConnection getConnection() {
        return this.playerConnection;
    }

    public void addCell(CellEntityImpl cell) {
        cells.add(cell);
        tracker.updateView();
        tracker.updateNodes();
        playerConnection.sendPacket(new PacketOutAddNode(cell.getID()));
    }

    public void removeCell(CellEntityImpl cell) {
        cells.remove(cell);
        tracker.updateView();
        tracker.updateNodes();
    }

    public void removeCell(int entityId) {
        Iterator<CellEntityImpl> it = cells.iterator();
        while (it.hasNext()) {
            if (it.next().getID() == entityId) {
                it.remove();
            }
        }
    }

    public Collection<CellEntityImpl> getCells() {
        return cells;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public PlayerTracker getTracker() {
        return tracker;
    }

    @Override
    public boolean isUsingOMP() {
        return ompCapable;
    }

    public void setOMPCapable(boolean ompCapable) {
        this.ompCapable = ompCapable;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.playerConnection);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerImpl other = (PlayerImpl) obj;
        if (!Objects.equals(this.playerConnection, other.playerConnection)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sendPluginMessage(String channel, byte[] data) {
        if (!isUsingOMP()) {
            return false;
        }

        if (channel.toUpperCase().startsWith("OMP|") || channel.toUpperCase().startsWith("O2|")) {
            throw new IllegalArgumentException("Attempted to send a message on reserved channel \"" + channel + "\"!");
        }

        if (!Ogar.getMessenger().isChannelRegistered(channel)) {
            throw new IllegalStateException("Attempted to send a message on channel \"" + channel + "\", but the channel was not registered!");
        }

        PacketOMPMessage packet = new PacketOMPMessage(channel, data);
        playerConnection.sendPacket(packet);
        return true;
    }

}
