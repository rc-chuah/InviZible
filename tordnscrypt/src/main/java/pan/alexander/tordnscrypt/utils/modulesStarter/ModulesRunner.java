package pan.alexander.tordnscrypt.utils.modulesStarter;

/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.preference.PreferenceManager;

public class ModulesRunner {

    private ModulesRunner() {
    }

    public static void runDNSCrypt(Context context) {
       sendStarterIntent(context, ModulesStarterService.actionStartDnsCrypt);
    }

    public static void runTor(Context context) {
        sendStarterIntent(context, ModulesStarterService.actionStartTor);
    }

    public static void runITPD(Context context) {
        sendStarterIntent(context, ModulesStarterService.actionStartITPD);
    }

    public static void recoverService(Context context) {
        sendStarterIntent(context, ModulesStarterService.actionRecoverService);
    }

    private static void sendStarterIntent(Context context, String action) {
        Intent intent = new Intent(context, ModulesStarterService.class);
        intent.setAction(action);
        intent.putExtra("showNotification", isShowNotification(context));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static boolean isShowNotification(Context context) {
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        return shPref.getBoolean("swShowNotification", true);
    }
}
