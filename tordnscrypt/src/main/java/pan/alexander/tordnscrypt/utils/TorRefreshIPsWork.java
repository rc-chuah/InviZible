package pan.alexander.tordnscrypt.utils;
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
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import pan.alexander.tordnscrypt.settings.PathVars;

import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

public class TorRefreshIPsWork {
    private Context context;
    private ArrayList<String> unlockHostsDevice;
    private  ArrayList<String> unlockIPsDevice;
    private ArrayList<String> unlockHostsTether;
    private  ArrayList<String> unlockIPsTether;
    private String[] commandsSaveIPs;
    private String appDataDir;
    private String busyboxPath;
    private boolean torTethering;
    private boolean routeAllThroughTorDevice;
    private boolean routeAllThroughTorTether;
    private GetIPsJobService getIPsJobService;

    public TorRefreshIPsWork(Context context, GetIPsJobService getIPsJobService) {
        this.context = context;
        this.getIPsJobService = getIPsJobService;
    }

    private void getBridgesIP() {
        Runnable getBridgesIPRunnable = new Runnable() {
            @Override
            public void run() {
                //////////////TO GET BRIDGES WITH TOR//////////////////////////////////////
                ArrayList<String> bridgesIPlist = handleActionGetIP("https://bridges.torproject.org");
                //////////////TO GET UPDATES WITH TOR//////////////////////////////////////
                bridgesIPlist.addAll(handleActionGetIP("https://invizible.net"));
                writeToFile(appDataDir+"/app_data/tor/bridgesIP",bridgesIPlist);
            }
        };
        Thread thread = new Thread(getBridgesIPRunnable);
        thread.start();
    }

    public void refreshIPs() {
        boolean rootOK = new PrefManager(context.getApplicationContext()).getBoolPref("rootOK");
        if (!rootOK) return;

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        torTethering = shPref.getBoolean("pref_common_tor_tethering",false);
        routeAllThroughTorDevice = shPref.getBoolean("pref_fast_all_through_tor",true);
        routeAllThroughTorTether = shPref.getBoolean("pref_common_tor_route_all",false);
        boolean runModulesWithRoot = shPref.getBoolean("swUseModulesRoot",false);
        boolean blockHttp = shPref.getBoolean("pref_fast_block_http",false);

        PathVars pathVars = new PathVars(context);
        appDataDir = pathVars.appDataDir;
        String dnsCryptPort = pathVars.dnsCryptPort;
        String itpdHttpProxyPort = pathVars.itpdHttpProxyPort;
        String torTransPort = pathVars.torTransPort;
        String torVirtAdrNet = pathVars.torVirtAdrNet;
        busyboxPath = pathVars.busyboxPath;
        String iptablesPath = pathVars.iptablesPath;
        String torSOCKSPort = pathVars.torSOCKSPort;
        String torHTTPTunnelPort = pathVars.torHTTPTunnelPort;
        String itpdSOCKSPort = pathVars.itpdSOCKSPort;
        String torDNSPort = pathVars.torDNSPort;
        String rejectAddress = pathVars.rejectAddress;


        getBridgesIP();

        String torSitesBypassNatTCP = "";
        String torSitesBypassFilterTCP = "";
        String torSitesBypassNatUDP = "";
        String torSitesBypassFilterUDP = "";
        String torAppsBypassNatTCP = "";
        String torAppsBypassNatUDP = "";
        String torAppsBypassFilterTCP = "";
        String torAppsBypassFilterUDP = "";
        if (routeAllThroughTorDevice) {
            torSitesBypassNatTCP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnet | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -d $var1 -j RETURN; done";
            torSitesBypassFilterTCP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnet | while read var1; do "+iptablesPath+"iptables -A tordnscrypt -p tcp -d $var1 -j RETURN; done";
            torSitesBypassNatUDP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnet | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p udp -d $var1 -j RETURN; done";
            torSitesBypassFilterUDP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnet | while read var1; do "+iptablesPath+"iptables -A tordnscrypt -p udp -d $var1 -j RETURN; done";
            torAppsBypassNatTCP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnetApps | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -m owner --uid-owner $var1 -j RETURN; done";
            torAppsBypassNatUDP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnetApps | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p udp -m owner --uid-owner $var1 -j RETURN; done";
            torAppsBypassFilterTCP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnetApps | while read var1; do "+iptablesPath+"iptables -A tordnscrypt -p tcp -m owner --uid-owner $var1 -j RETURN; done";
            torAppsBypassFilterUDP = busyboxPath+ "cat "+appDataDir+"/app_data/tor/clearnetApps | while read var1; do "+iptablesPath+"iptables -A tordnscrypt -p udp -m owner --uid-owner $var1 -j RETURN; done";
        }

        String blockHttpRuleFilterAll = "";
        String blockHttpRuleNatTCP = "";
        String blockHttpRuleNatUDP = "";
        if (blockHttp) {
            blockHttpRuleFilterAll = iptablesPath + "iptables -A tordnscrypt -d +"+ rejectAddress +" -j REJECT";
            blockHttpRuleNatTCP = iptablesPath + "iptables -t nat -A tordnscrypt_nat_output -p tcp --dport 80 -j DNAT --to-destination "+rejectAddress;
            blockHttpRuleNatUDP = iptablesPath + "iptables -t nat -A tordnscrypt_nat_output -p udp --dport 80 -j DNAT --to-destination "+rejectAddress;
        }

        String appUID = new PrefManager(context).getStrPref("appUID");
        if (runModulesWithRoot) {
            appUID ="0";
        }


        String ipsToUnlock = "";

        String ipsToUnlockTether = "";
        String chmodIpsToUnlockTether = "";
        if (torTethering) {
            if (!routeAllThroughTorTether) {
                ipsToUnlockTether = busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/unlock_tether";
                chmodIpsToUnlockTether = busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/unlock_tether";
            } else {
                ipsToUnlockTether = busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/clearnet_tether";
                chmodIpsToUnlockTether = busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/clearnet_tether";
            }
        }

        if( new PrefManager(Objects.requireNonNull(context)).getBoolPref("Tor Running") &&
                new PrefManager(context).getBoolPref("DNSCrypt Running")){

            if (!routeAllThroughTorDevice) {
                commandsSaveIPs = new String[] {
                        busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/unlock",
                        ipsToUnlockTether,
                        busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/unlock",
                        chmodIpsToUnlockTether,
                        busyboxPath+ "sleep 1",
                        "ip6tables -D OUTPUT -j DROP || true",
                        "ip6tables -I OUTPUT -j DROP",
                        iptablesPath+ "iptables -t nat -F tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -D OUTPUT -j tordnscrypt_nat_output || true",
                        iptablesPath+ "iptables -t nat -N tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -I OUTPUT -j tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d "+torVirtAdrNet+" -j DNAT --to-destination 127.0.0.1:"+torTransPort,
                        blockHttpRuleNatTCP,
                        blockHttpRuleNatUDP,
                        //iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d "+dnsCryptFallbackRes+" --dport 53 -j ACCEPT",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:"+dnsCryptPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp --dport 53 -j DNAT --to-destination 127.0.0.1:"+dnsCryptPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d 10.191.0.1 -j DNAT --to-destination 127.0.0.1:"+itpdHttpProxyPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d 10.191.0.1 -j DNAT --to-destination 127.0.0.1:"+itpdHttpProxyPort,
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/bridgesIP | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -d $var1 -j REDIRECT --to-port "+torTransPort+"; done",
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/unlock | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -d $var1 -j REDIRECT --to-port "+torTransPort+"; done",
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/unlockApps | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -m owner --uid-owner $var1 -j REDIRECT --to-port "+torTransPort+"; done"};
            } else {
                commandsSaveIPs = new String[] {
                        busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/clearnet",
                        ipsToUnlockTether,
                        busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/clearnet",
                        chmodIpsToUnlockTether,
                        busyboxPath+ "sleep 1",
                        "ip6tables -D OUTPUT -j DROP || true",
                        "ip6tables -I OUTPUT -j DROP",
                        iptablesPath+ "iptables -t nat -F tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -D OUTPUT -j tordnscrypt_nat_output || true",
                        iptablesPath+ "iptables -F tordnscrypt",
                        iptablesPath+ "iptables -D OUTPUT -j tordnscrypt || true",
                        busyboxPath+ "sleep 1",
                        "TOR_UID="+ appUID,
                        iptablesPath+ "iptables -t nat -N tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -I OUTPUT -j tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d 10.191.0.1 -j DNAT --to-destination 127.0.0.1:"+itpdHttpProxyPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d 10.191.0.1 -j DNAT --to-destination 127.0.0.1:"+itpdHttpProxyPort,
                        //iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d "+dnsCryptFallbackRes+" --dport 53 -j ACCEPT",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:"+dnsCryptPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp --dport 53 -j DNAT --to-destination 127.0.0.1:"+dnsCryptPort,
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/bridgesIP | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -d $var1 -j REDIRECT --to-port "+torTransPort+"; done",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -m owner --uid-owner $TOR_UID -j RETURN",
                        iptablesPath + "iptables -t nat -A tordnscrypt_nat_output -p tcp -d " + torVirtAdrNet + " -j DNAT --to-destination 127.0.0.1:" + torTransPort,
                        blockHttpRuleNatTCP,
                        blockHttpRuleNatUDP,
                        torSitesBypassNatTCP,
                        torSitesBypassNatUDP,
                        torAppsBypassNatTCP,
                        torAppsBypassNatUDP,
                        //iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp --syn -j DNAT --to-destination 127.0.0.1:"+torTransPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -j DNAT --to-destination 127.0.0.1:"+torTransPort,
                        iptablesPath+ "iptables -N tordnscrypt",
                        iptablesPath+ "iptables -A tordnscrypt -m state --state ESTABLISHED,RELATED -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torHTTPTunnelPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torHTTPTunnelPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+itpdSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+itpdSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+itpdHttpProxyPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+itpdHttpProxyPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torTransPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+dnsCryptPort+" -m owner --uid-owner 0 -j ACCEPT",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+dnsCryptPort+" -m owner --uid-owner 0 -j ACCEPT",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+dnsCryptPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+dnsCryptPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -m owner --uid-owner $TOR_UID -j RETURN",
                        blockHttpRuleFilterAll,
                        torSitesBypassFilterTCP,
                        torSitesBypassFilterUDP,
                        torAppsBypassFilterTCP,
                        torAppsBypassFilterUDP,
                        iptablesPath+ "iptables -A tordnscrypt -j REJECT",
                        iptablesPath+ "iptables -I OUTPUT -j tordnscrypt"};
            }

        } else if (new PrefManager(Objects.requireNonNull(context)).getBoolPref("Tor Running")) {
            if (!routeAllThroughTorDevice) {
                commandsSaveIPs = new String[] {
                        busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/unlock",
                        ipsToUnlockTether,
                        busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/unlock",
                        chmodIpsToUnlockTether,
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/bridgesIP | while read var1; do "+iptablesPath+"iptables -t nat -I tordnscrypt_nat_output -p tcp -d $var1 -j REDIRECT --to-port "+torTransPort+"; done"
                };
            } else {
                commandsSaveIPs = new String[] {
                        busyboxPath+ "echo \"" + ipsToUnlock + "\" > "+appDataDir+"/app_data/tor/clearnet",
                        ipsToUnlockTether,
                        busyboxPath+ "chmod 644 "+appDataDir+"/app_data/tor/clearnet",
                        chmodIpsToUnlockTether,
                        busyboxPath+ "sleep 1",
                        "ip6tables -D OUTPUT -j DROP || true",
                        "ip6tables -I OUTPUT -j DROP",
                        iptablesPath+ "iptables -t nat -F tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -D OUTPUT -j tordnscrypt_nat_output || true",
                        iptablesPath+ "iptables -F tordnscrypt",
                        iptablesPath+ "iptables -D OUTPUT -j tordnscrypt || true",
                        busyboxPath+ "sleep 1",
                        "TOR_UID="+ appUID,
                        iptablesPath+ "iptables -t nat -N tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -I OUTPUT -j tordnscrypt_nat_output",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp -d 127.0.0.1/32 -j RETURN",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:"+ torDNSPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp --dport 53 -j DNAT --to-destination 127.0.0.1:"+ torDNSPort,
                        busyboxPath+ "cat "+appDataDir+"/app_data/tor/bridgesIP | while read var1; do "+iptablesPath+"iptables -t nat -A tordnscrypt_nat_output -p tcp -d $var1 -j REDIRECT --to-port "+torTransPort+"; done",
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -m owner --uid-owner $TOR_UID -j RETURN",
                        iptablesPath + "iptables -t nat -A tordnscrypt_nat_output -p tcp -d " + torVirtAdrNet + " -j DNAT --to-destination 127.0.0.1:" + torTransPort,
                        blockHttpRuleNatTCP,
                        blockHttpRuleNatUDP,
                        torSitesBypassNatTCP,
                        torSitesBypassNatUDP,
                        torAppsBypassNatTCP,
                        torAppsBypassNatUDP,
                        //iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp --syn -j DNAT --to-destination 127.0.0.1:"+torTransPort,
                        iptablesPath+ "iptables -t nat -A tordnscrypt_nat_output -p tcp -j DNAT --to-destination 127.0.0.1:"+torTransPort,
                        iptablesPath+ "iptables -N tordnscrypt",
                        iptablesPath+ "iptables -A tordnscrypt -m state --state ESTABLISHED,RELATED -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torHTTPTunnelPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torHTTPTunnelPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+itpdSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+itpdSOCKSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+itpdHttpProxyPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+itpdHttpProxyPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torTransPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torDNSPort+" -m owner --uid-owner 0 -j ACCEPT",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torDNSPort+" -m owner --uid-owner 0 -j ACCEPT",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p udp -m udp --dport "+torDNSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -d 127.0.0.1/32 -p tcp -m tcp --dport "+torDNSPort+" -j RETURN",
                        iptablesPath+ "iptables -A tordnscrypt -m owner --uid-owner $TOR_UID -j RETURN",
                        blockHttpRuleFilterAll,
                        torSitesBypassFilterTCP,
                        torSitesBypassFilterUDP,
                        torAppsBypassFilterTCP,
                        torAppsBypassFilterUDP,
                        iptablesPath+ "iptables -A tordnscrypt -j REJECT",
                        iptablesPath+ "iptables -I OUTPUT -j tordnscrypt"};
            }

        }

        if (commandsSaveIPs==null)
            return;

        if (torTethering) {
            Tethering tethering = new Tethering(context);
            String[] commandsTether = tethering.activateTethering(false);
            if (commandsTether!=null && commandsTether.length>0)
                commandsSaveIPs = Arr.ADD2(commandsSaveIPs, commandsTether);
        }

        Set<String> setUnlockHosts;
        Set<String> setUnlockIPs;
        if (!routeAllThroughTorDevice) {
            setUnlockHosts = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("unlockHosts");
            setUnlockIPs = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("unlockIPs");
        } else {
            setUnlockHosts = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("clearnetHosts");
            setUnlockIPs = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("clearnetIPs");
        }
        unlockHostsDevice = new ArrayList<>(setUnlockHosts);
        unlockIPsDevice = new ArrayList<>(setUnlockIPs);

        if (!routeAllThroughTorTether) {
            setUnlockHosts = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("unlockHostsTether");
            setUnlockIPs = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("unlockIPsTether");
        } else {
            setUnlockHosts = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("clearnetHostsTether");
            setUnlockIPs = new PrefManager(Objects.requireNonNull(context)).getSetStrPref("clearnetIPsTether");
        }
        unlockHostsTether = new ArrayList<>(setUnlockHosts);
        unlockIPsTether = new ArrayList<>(setUnlockIPs);

        performBackgroundWork();
    }

    private void performBackgroundWork() {

        Runnable performWorkRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG,"TorRefreshIPsWork performBackgroundWork");

                if (!unlockHostsDevice.isEmpty() || !unlockIPsDevice.isEmpty()) {

                    String unlockIPsReadyDevice = universalGetIPs(unlockHostsDevice, unlockIPsDevice);

                    if (unlockIPsReadyDevice == null)
                        unlockIPsReadyDevice = "";

                    String unlockIPsReadyTether = universalGetIPs(unlockHostsTether, unlockIPsTether);

                    if (unlockIPsReadyTether == null)
                        unlockIPsReadyTether = "";


                    if (!routeAllThroughTorDevice) {
                        commandsSaveIPs[0] = busyboxPath + "echo \"" + unlockIPsReadyDevice + "\" > " + appDataDir + "/app_data/tor/unlock";
                    } else {
                        commandsSaveIPs[0] = busyboxPath + "echo \"" + unlockIPsReadyDevice + "\" > " + appDataDir + "/app_data/tor/clearnet";
                    }

                    if (torTethering) {
                        if (!routeAllThroughTorTether) {
                            commandsSaveIPs[1] = busyboxPath + "echo \"" + unlockIPsReadyTether + "\" > " + appDataDir + "/app_data/tor/unlock_tether";
                        } else {
                            commandsSaveIPs[1] = busyboxPath + "echo \"" + unlockIPsReadyTether + "\" > " + appDataDir + "/app_data/tor/clearnet_tether";
                        }
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                RootCommands rootCommands = new RootCommands(commandsSaveIPs);
                Intent intent = new Intent(context, RootExecService.class);
                intent.setAction(RootExecService.RUN_COMMAND);
                intent.putExtra("Commands",rootCommands);
                intent.putExtra("Mark", RootExecService.NullMark);
                RootExecService.performAction(context,intent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getIPsJobService!=null) {
                    getIPsJobService.finishJob();
                }
            }
        };
        Thread thread = new Thread(performWorkRunnable);
        thread.start();
    }




    private String universalGetIPs(ArrayList<String> hosts, ArrayList<String> IPs) {


        ArrayList<String> unlockIPsPrepared = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        if (hosts!=null) {
            for (String host:hosts) {
                if (!host.startsWith("#")) {
                    ArrayList<String> preparedIPs = handleActionGetIP(host);
                    unlockIPsPrepared.addAll(preparedIPs);
                }
            }

            for (String unlockIPprepared:unlockIPsPrepared) {
                if (unlockIPprepared.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}"))
                    sb.append(unlockIPprepared).append((char)10);
            }
        }

        if (IPs!=null) {
            for (String unlockIP:IPs) {
                if (!unlockIP.startsWith("#")) {
                    sb.append(unlockIP).append((char)10);
                }
            }

        }

        if (sb.toString().isEmpty())
            return null;

        return sb.toString();
    }

    private ArrayList<String> handleActionGetIP(String host) {
        ArrayList<String> preparedIPs = new ArrayList<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(new URL(host).getHost());
            for (InetAddress address:addresses) {
                preparedIPs.add(address.getHostAddress());
            }
        } catch (UnknownHostException | MalformedURLException e) {
            e.printStackTrace();
        }
        return preparedIPs;
    }

    private void writeToFile(String filePath, ArrayList<String> lines) {
        PrintWriter writer = null;
        try {
            File f = new File(filePath);
            if (f.isFile() && f.setWritable(true)) {
                Log.i(LOG_TAG,"TorRefreshIPsWork writeTo " + filePath + " success");
            }

            writer = new PrintWriter(filePath);
            for (String line:lines)
                writer.println(line);
            writer.close();
            writer = null;
        } catch (IOException e) {
            Log.e(LOG_TAG,"TorRefreshIPsWork writeFile Exception " + e.getMessage());
        } finally {
            if (writer != null)writer.close();
        }
    }
}