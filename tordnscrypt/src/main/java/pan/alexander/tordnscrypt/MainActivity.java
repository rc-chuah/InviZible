package pan.alexander.tordnscrypt;
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

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import pan.alexander.tordnscrypt.utils.ApManager;
import pan.alexander.tordnscrypt.utils.LangAppCompatActivity;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.Registration;

import static pan.alexander.tordnscrypt.TopFragment.LOG_TAG;

public class MainActivity extends LangAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public boolean childLockActive = false;
    private Timer timer;
    private static final int CODE_IS_AP_ON = 100;
    public static DialogInterface modernDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            String theme = defaultSharedPreferences.getString("pref_fast_theme","4");
            switch (theme) {
                case "1":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case "2":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case "3":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    break;
                case "4":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Intent intent = getIntent();
        if (Objects.equals(intent.getAction(), "check_update")) {
            TopFragment topFragment = (TopFragment) getFragmentManager().findFragmentByTag("topFragmentTAG");
            if (topFragment!=null) {
                topFragment.checkNewVer();
                modernDialog = modernProgressDialog();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        boolean show_com_res = new PrefManager(this).getBoolPref("Show com result");
        if(show_com_res){
            menu.findItem(R.id.menu_com_result).setChecked(true);
        } else {
            menu.findItem(R.id.menu_com_result).setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        switch (id){
            case R.id.menu_com_result:

                break;
            case R.id.menu_dns_scr:

                break;
            case R.id.menu_tor_scr:

                break;
            case R.id.menu_hide_dns_scr:

                break;
            case R.id.menu_hide_tor_scr:
                break;
        }


        return super.onOptionsItemSelected(item);
    }*/



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            //String saved_pass = new String(Base64.decode(new PrefManager(getApplicationContext()).getStrPref("passwd"),16));
            if (childLockActive) {
                menu.findItem(R.id.item_unlock).setIcon(R.drawable.ic_lock_white_24dp);
            } else {
                menu.findItem(R.id.item_unlock).setIcon(R.drawable.ic_lock_open_white_24dp);
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG,"MainActivity Child Lock Exeption "+e.getMessage());
        }

        ApManager apManager = new ApManager(this);
        int apState = apManager.isApOn();
        if (apState == ApManager.apStateON) {
            menu.findItem(R.id.item_hotspot).setIcon(R.drawable.ic_wifi_tethering_green_24dp);
            if (!new PrefManager(this).getBoolPref("APisON")) {
                //Tethering tethering = new Tethering(this);
                //tethering.overrideAFWallDNSrules();
                boolean dnsCryptRunning = new PrefManager(this).getBoolPref("DNSCrypt Running");
                boolean torRunning = new PrefManager(this).getBoolPref("Tor Running");
                boolean itpdRunning = new PrefManager(this).getBoolPref("I2PD Running");
                if (dnsCryptRunning) {
                    TopFragment.NotificationDialogFragment commandResult =
                            TopFragment.NotificationDialogFragment.newInstance(getText(R.string.pref_common_restart_dnscrypt).toString());
                    commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
                } else if (torRunning) {
                    TopFragment.NotificationDialogFragment commandResult =
                            TopFragment.NotificationDialogFragment.newInstance(getText(R.string.pref_common_restart_tor).toString());
                    commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
                } else if (itpdRunning) {
                    TopFragment.NotificationDialogFragment commandResult =
                            TopFragment.NotificationDialogFragment.newInstance(getText(R.string.pref_common_restart_itpd).toString());
                    commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
                }
                new PrefManager(this).setBoolPref("APisON",true);
            }

        } else if (apState == ApManager.apStateOFF){
            menu.findItem(R.id.item_hotspot).setIcon(R.drawable.ic_portable_wifi_off_white_24dp);
            new PrefManager(this).setBoolPref("APisON",false);
        } else {
            menu.findItem(R.id.item_hotspot).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.item_unlock:

                try {
                    String saved_pass = new String(Base64.decode(new PrefManager(getApplicationContext()).getStrPref("passwd"),16));
                    //Toast.makeText(getApplicationContext(),saved_pass,Toast.LENGTH_LONG).show();
                    if (saved_pass.contains("-l-o-c-k-e-d")) {
                        childUnlock(item);
                    } else {
                        childLock(item);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG,"MainActivity Child Lock Exeption "+e.getMessage());
                }
                break;
            case R.id.item_hotspot:

                try {
                    ApManager apManager = new ApManager(this);
                    if (apManager.configApState()) {
                        checkHotspotState();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_MAIN, null);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                        intent.setComponent(cn);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        this.startActivityForResult(intent,CODE_IS_AP_ON);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkHotspotState() {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {

            int loop = 0;

            @Override
            public void run() {

                if (++loop > 3) {
                    timer.cancel();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidateOptionsMenu();
                    }
                });
            }
        },3000,5000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODE_IS_AP_ON) {
            checkHotspotState();
        }
    }


    private void childLock(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_mode_child_lock);
        builder.setMessage(R.string.action_mode_dialog_message_lock);
        builder.setIcon(R.drawable.ic_lock_outline_blue_24dp);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        final MainActivity mainActivity = this;

        String saved_pass = new PrefManager(getApplicationContext()).getStrPref("passwd");
        if (!saved_pass.isEmpty()) {
            saved_pass = new String(Base64.decode(saved_pass,16));
            input.setText(saved_pass);
            input.setSelection(saved_pass.length());
        }
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (input.getText().toString().equals("debug")) {
                    TopFragment.debug = !TopFragment.debug;
                    Toast.makeText(getApplicationContext(),"Debug mode " + TopFragment.debug,Toast.LENGTH_LONG).show();
                } else {
                    String pass = Base64.encodeToString((input.getText().toString() + "-l-o-c-k-e-d").getBytes(), 16);
                    new PrefManager(getApplicationContext()).setStrPref("passwd", pass);
                    Toast.makeText(getApplicationContext(), getText(R.string.action_mode_dialog_locked), Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_lock_white_24dp);
                    childLockActive = true;

                    DrawerLayout mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    private void childUnlock(final MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_mode_child_lock);
        builder.setMessage(R.string.action_mode_dialog_message_unlock);
        builder.setIcon(R.drawable.ic_lock_outline_blue_24dp);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setView(input);

        final MainActivity mainActivity = this;

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String saved_pass = new String(Base64.decode(new PrefManager(getApplicationContext()).getStrPref("passwd"),16));
                if (saved_pass.replace("-l-o-c-k-e-d","").equals(input.getText().toString())) {
                    String pass = Base64.encodeToString((input.getText().toString()).getBytes(),16);
                    new PrefManager(getApplicationContext()).setStrPref("passwd", pass);
                    Toast.makeText(getApplicationContext(),getText(R.string.action_mode_dialog_unlocked),Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_lock_open_white_24dp);
                    childLockActive = false;

                    DrawerLayout mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                } else {
                    childUnlock(item);
                    Toast.makeText(getApplicationContext(),getText(R.string.action_mode_dialog_wrong_pass),Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (childLockActive) {
            Toast.makeText(this,getText(R.string.action_mode_dialog_locked),Toast.LENGTH_LONG).show();
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return false;
        }

        if (id == R.id.nav_backup) {
            Intent intent = new Intent(this,BackupActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_DNS_Pref) {
            Intent intent = new Intent(this,SettingsActivity.class);
            intent.setAction("DNS_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_Tor_Pref) {
            Intent intent = new Intent(this,SettingsActivity.class);
            intent.setAction("Tor_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_I2PD_Pref) {
            Intent intent = new Intent(this,SettingsActivity.class);
            intent.setAction("I2PD_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_fast_Pref) {
            Intent intent = new Intent(this,SettingsActivity.class);
            intent.setAction("fast_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this,AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_common_Pref) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setAction("common_Pref");
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        } else if (id==R.id.nav_Donate) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://invizible.net/donate"));
            startActivity(intent);
        } else if (id==R.id.nav_Code) {
            Registration registration = new Registration(this);
            registration.showEnterCodeDialog();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            String saved_pass = new String(Base64.decode(new PrefManager(this).getStrPref("passwd"),16));
            childLockActive = saved_pass.contains("-l-o-c-k-e-d");
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG,"MainActivity Child Lock Exeption "+e.getMessage());
        }



    }


    public void showUpdateMessage(String message){
        if (modernDialog!=null)
            modernDialog.dismiss();
        modernDialog = null;
        TopFragment.NotificationDialogFragment commandResult = TopFragment.NotificationDialogFragment.newInstance(message);
        commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
    }

    public DialogInterface modernProgressDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.update_checking_title);
        builder.setMessage(R.string.update_checking_message);
        builder.setIcon(R.drawable.ic_visibility_off_black_24dp);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (modernDialog!=null) {
                    modernDialog.dismiss();
                    modernDialog = null;

                    //////////////To STOP UPDATES CHECK/////////////////////////////////////////////////////
                    TopFragment topFragment = (TopFragment) getFragmentManager().findFragmentByTag("topFragmentTAG");
                    if (topFragment!=null) {
                        topFragment.updateCheck.context = null;
                    }

                }
            }
        });

        ProgressBar progressBar = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        builder.setView(progressBar);
        builder.setCancelable(false);
        return builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer!=null)
            timer.cancel();
        if (modernDialog!=null) {
            modernDialog.dismiss();
        }
    }


}