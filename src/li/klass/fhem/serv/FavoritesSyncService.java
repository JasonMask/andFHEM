/*
 * AndFHEM - Open Source Android application to control a FHEM home automation
 * server.
 *
 * Copyright (c) 2011, Matthias Klass or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLIC LICENSE, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
 * for more details.
 *
 * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
 * along with this distribution; if not, write to:
 *   Free Software Foundation, Inc.
 *   51 Franklin Street, Fifth Floor
 *   Boston, MA  02110-1301  USA
 */

package li.klass.fhem.serv;

import android.app.Activity;
import android.content.SharedPreferences;
import li.klass.fhem.AndFHEMApplication;
import li.klass.fhem.domain.Device;
import li.klass.fhem.domain.RoomDeviceList;

import java.util.Set;

public class FavoritesSyncService {
    public static final FavoritesSyncService INSTANCE = new FavoritesSyncService();

    private FavoritesSyncService() {
    }

    private static final String PREFERENCES_NAME = "favorites";

    /**
     * Adds a new favorite device.
     * @param device device to add.
     */
    public void addFavorite(Device device) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(device.getName(), device.getName()).commit();
    }

    /**
     * Removes a favorite.
     * @param device favorite to remove.
     */
    public void removeFavorite(Device device) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.remove(device.getName()).commit();
    }

    /**
     * Reads all saved favorite devices. The result will be provided to the given listener.
     * @param refresh should the underlying {@link RoomDeviceList} be refreshed by asking FHEM for new values?
     * @return favorite {@link RoomDeviceList}
     */
    public RoomDeviceList getFavorites(boolean refresh) {

        RoomDeviceList allRoomsDeviceList = RoomListSyncService.INSTANCE.getAllRoomsDeviceList(refresh);
        RoomDeviceList favoritesList = new RoomDeviceList("favorites");
        Set<String> favoriteDeviceNames = getPreferences().getAll().keySet();
        for (String favoriteDeviceName : favoriteDeviceNames) {
            Device device = allRoomsDeviceList.getDeviceFor(favoriteDeviceName);
            if (device != null) {
                favoritesList.addDevice(device);
            }
        }

        return favoritesList;
    }

    /**
     * @return the {@link android.content.SharedPreferences} object.
     */
    private SharedPreferences getPreferences() {
        return AndFHEMApplication.getContext().getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);
    }
}