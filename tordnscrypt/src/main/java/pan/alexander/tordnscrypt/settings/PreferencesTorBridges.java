package pan.alexander.tordnscrypt.settings;
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


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.TopFragment;
import pan.alexander.tordnscrypt.utils.GetNewBridges;
import pan.alexander.tordnscrypt.utils.NoRootService;
import pan.alexander.tordnscrypt.utils.NotificationHelper;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.RootCommands;
import pan.alexander.tordnscrypt.utils.RootExecService;
import pan.alexander.tordnscrypt.utils.Verifier;

import static pan.alexander.tordnscrypt.TopFragment.TOP_BROADCAST;
import static pan.alexander.tordnscrypt.TopFragment.appSign;
import static pan.alexander.tordnscrypt.TopFragment.wrongSign;
import static pan.alexander.tordnscrypt.utils.GetNewBridges.dialogPleaseWait;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class PreferencesTorBridges extends Fragment implements View.OnClickListener {
    RadioButton rbNoBridges;
    RadioButton rbDefaultBridges;
    Spinner spDefaultBridges;
    RadioButton rbOwnBridges;
    Spinner spOwnBridges;
    Button btnRequestBridges;
    Button btnAddBridges;
    TextView tvBridgesListEmpty;
    RecyclerView rvBridges;

    PathVars pathVars;
    List<String> tor_conf;
    List<String> tor_conf_orig;
    //List<String> bridges;
    List<ObfsBridge> bridgeList;
    List<String> currentBridges;
    List<String> anotherBridges;
    String currentBridgesType;
    BridgeAdapter bridgeAdapter;
    String bridges_file_path;


    public PreferencesTorBridges() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        pathVars = new PathVars(getActivity());

        tor_conf = new LinkedList<>();
        tor_conf_orig = new LinkedList<>();

        //bridges = new LinkedList<>();
        bridgeList = new LinkedList<>();
        currentBridges = new LinkedList<>();
        anotherBridges = new LinkedList<>();

        tor_conf = readFile(pathVars.appDataDir+"/app_data/tor/tor.conf");
        if (tor_conf!=null)
            tor_conf_orig.addAll(tor_conf);
       }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_preferences_tor_bridges, container, false);

        rbNoBridges = view.findViewById(R.id.rbNoBridges);
        rbNoBridges.setOnCheckedChangeListener(onCheckedChangeListener);

        rbDefaultBridges = view.findViewById(R.id.rbDefaultBridges);
        rbDefaultBridges.setOnCheckedChangeListener(onCheckedChangeListener);

        spDefaultBridges = view.findViewById(R.id.spDefaultBridges);
        spDefaultBridges.setOnItemSelectedListener(onItemSelectedListener);
        spDefaultBridges.setPrompt(getString(R.string.pref_fast_use_tor_bridges_obfs));

        rbOwnBridges = view.findViewById(R.id.rbOwnBridges);
        rbOwnBridges.setOnCheckedChangeListener(onCheckedChangeListener);

        spOwnBridges = view.findViewById(R.id.spOwnBridges);
        spOwnBridges.setOnItemSelectedListener(onItemSelectedListener);
        spOwnBridges.setPrompt(getString(R.string.pref_fast_use_tor_bridges_obfs));

        btnRequestBridges = view.findViewById(R.id.btnRequestBridges);
        btnRequestBridges.setOnClickListener(this);
        btnAddBridges = view.findViewById(R.id.btnAddBridges);
        btnAddBridges.setOnClickListener(this);

        tvBridgesListEmpty = view.findViewById(R.id.tvBridgesListEmpty);

        rvBridges = view.findViewById(R.id.rvBridges);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        rvBridges.setLayoutManager(mLayoutManager);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!new PrefManager(getActivity()).getStrPref("defaultBridgesObfs").isEmpty())
            spDefaultBridges.setSelection(Integer.parseInt(new PrefManager(getActivity()).getStrPref("defaultBridgesObfs")));

        if (!new PrefManager(getActivity()).getStrPref("ownBridgesObfs").isEmpty())
            spOwnBridges.setSelection(Integer.parseInt(new PrefManager(getActivity()).getStrPref("ownBridgesObfs")));

        for (int i=0;i<tor_conf.size();i++) {
            String line = tor_conf.get(i);
            if (!line.contains("#") && line.contains("Bridge ")) {
                currentBridges.add(line.replace("Bridge ","").trim());
            }
        }

        if (!currentBridges.isEmpty()) {
            String testBridge = currentBridges.get(0);
            if (testBridge.contains("obfs4")) {
                currentBridgesType = "obfs4";
            } else if (testBridge.contains("obfs3")) {
                currentBridgesType = "obfs3";
            } else if (testBridge.contains("scramblesuit")) {
                currentBridgesType = "scramblesuit";
            } else if (testBridge.contains("meek_lite")) {
                currentBridgesType = "meek_lite";
            } else {
                currentBridgesType = "none";
            }
        } else {
            currentBridgesType = "";
        }


        bridgeAdapter = new BridgeAdapter(bridgeList);
        rvBridges.setAdapter(bridgeAdapter);

        boolean useNoBridges = new PrefManager(getActivity()).getBoolPref("useNoBridges");
        boolean useDefaultBridges = new PrefManager(getActivity()).getBoolPref("useDefaultBridges");
        boolean useOwnBridges = new PrefManager(getActivity()).getBoolPref("useOwnBridges");

        if (!useNoBridges && !useDefaultBridges && !useOwnBridges) {
            rbNoBridges.setChecked(true);
            tvBridgesListEmpty.setVisibility(View.GONE);
        } else if (useNoBridges){
            noBridgesOperation();
            rbNoBridges.setChecked(true);
        } else if (useDefaultBridges) {
            defaultBridgesOperation();
            rbDefaultBridges.setChecked(true);
        } else {
            ownBridgesOperation();
            rbOwnBridges.setChecked(true);
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Verifier verifier = new Verifier(getActivity());
                    String appSignAlt = verifier.getApkSignature();
                    if (!verifier.decryptStr(wrongSign,appSign,appSignAlt).equals(TOP_BROADCAST)) {
                        NotificationHelper notificationHelper = NotificationHelper.setHelperMessage(
                                getActivity(),getText(R.string.verifier_error).toString(),"3458");
                        if (notificationHelper != null) {
                            notificationHelper.show(getFragmentManager(),NotificationHelper.TAG_HELPER);
                        }
                    }

                } catch (Exception e) {
                    NotificationHelper notificationHelper = NotificationHelper.setHelperMessage(
                            getActivity(),getText(R.string.verifier_error).toString(),"64539");
                    if (notificationHelper != null) {
                        notificationHelper.show(getFragmentManager(),NotificationHelper.TAG_HELPER);
                    }
                    Log.e(LOG_TAG,"PreferencesTorBridges fault "+e.getMessage() + " " + e.getCause() + System.lineSeparator() +
                            Arrays.toString(e.getStackTrace()));
                }
            }
        });
        thread.start();
    }

    @Override
    public void onStop() {
        super.onStop();


        List<String> tor_conf_clean = new LinkedList<>();

        for (int i=0;i<tor_conf.size();i++) {
            String line = tor_conf.get(i);
            if ((line.contains("#") || (!line.contains("Bridge ") && !line.contains("ClientTransportPlugin "))) && !line.isEmpty()) {
                tor_conf_clean.add(line);
            }
        }

        //TopFragment.NotificationDialogFragment commandResult = TopFragment.NotificationDialogFragment.newInstance(tor_conf_clean.toString());
        //commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);

        tor_conf = tor_conf_clean;

        String currentBridgesTypeToSave;
        if (currentBridgesType.equals("none")) {
            currentBridgesTypeToSave = "";
        } else {
            currentBridgesTypeToSave = currentBridgesType;
        }

        if (!currentBridges.isEmpty()) {

            if (!currentBridgesType.equals("none")) {
                String clientTransportPlugin = "ClientTransportPlugin " + currentBridgesTypeToSave + " exec "
                        + pathVars.appDataDir+"/app_bin/obfs4proxy";

                tor_conf.add(clientTransportPlugin);
            }

            for (int i=0;i<currentBridges.size();i++) {
                tor_conf.add("Bridge "+currentBridges.get(i));
            }
        } else {
            for (int i=0;i<tor_conf.size();i++) {
                if (tor_conf.get(i).contains("UseBridges")) {
                    String line = tor_conf.get(i);
                    String result = line.replace("1","0");
                    if (!result.equals(line)) {
                        tor_conf.set(i,result);
                    }
                }
            }
        }

        if (Arrays.equals(tor_conf.toArray(), tor_conf_orig.toArray()))
            return;
        writeToFile(pathVars.appDataDir+"/app_data/tor/tor.conf",tor_conf);

        ///////////////////////Tor restart/////////////////////////////////////////////
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean rnTorWithRoot = shPref.getBoolean("swUseModulesRoot",false);
        boolean torRunning = new PrefManager(getActivity()).getBoolPref("Tor Running");
        String[] commandsRestart;
        if (rnTorWithRoot) {
            commandsRestart = new String[] {
                    pathVars.busyboxPath+ "killall tor; if [[ $? -eq 0 ]] ; " +
                            "then "+pathVars.torPath+" -f "+pathVars.appDataDir+"/app_data/tor/tor.conf; fi"};
        } else {
            commandsRestart = new String[] {
                    pathVars.busyboxPath+ "killall tor"};
            if (torRunning)
                runTorNoRoot();
        }
        RootCommands rootCommands  = new RootCommands(commandsRestart);
        Intent intent = new Intent(getActivity(), RootExecService.class);
        intent.setAction(RootExecService.RUN_COMMAND);
        intent.putExtra("Commands",rootCommands);
        intent.putExtra("Mark", RootExecService.NullMark);
        RootExecService.performAction(getActivity(),intent);
        Toast.makeText(getActivity(),getText(R.string.toastSettings_saved),Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (dialogPleaseWait!=null){
            dialogPleaseWait.cancel();
        }
    }

    private List<String> readFile(String filePath) {
        BufferedReader br = null;
        FileInputStream fstream = null;
        try {
            String appUID = new PrefManager(getActivity()).getStrPref("appUID");
            PathVars pathVars = new PathVars(getActivity());
            String[] commands = {pathVars.busyboxPath + "chown -R " + appUID + "." + appUID + " " + filePath,
                    "restorecon " + filePath};
            RootCommands rootCommands = new RootCommands(commands);
            Intent intent = new Intent(getActivity(), RootExecService.class);
            intent.setAction(RootExecService.RUN_COMMAND);
            intent.putExtra("Commands",rootCommands);
            intent.putExtra("Mark", RootExecService.NullMark);
            RootExecService.performAction(getActivity(),intent);


            File f = new File(filePath);
            if (f.isFile() && f.setReadable(true)) {
                Log.i(LOG_TAG,"PreferencesTorBridges take " + filePath + " success");
            } else {
                return null;
            }


            fstream = new FileInputStream(filePath);
            br = new BufferedReader(new InputStreamReader(fstream));
            List<String> list = new LinkedList<>();

            for(String tmp; (tmp = br.readLine()) != null;) {
                list.add(tmp.trim());
            }
            fstream.close();
            fstream = null;
            br.close();
            br = null;
            return list;
        } catch (IOException e) {
            Log.e(LOG_TAG,"PreferencesTorBridges readFile Exception " + e.getMessage());
        } finally {
            try {
                if (fstream!= null)fstream.close();
                if (br != null)br.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG,"PreferencesTorBridges readFile Error when close file" + ex.getMessage());
            }
        }
        return null;
    }

    private void writeToFile(String filePath, List<String> lines) {
        PrintWriter writer = null;
        try {
            File f = new File(filePath);
            if (f.isFile() && f.setWritable(true)) {
                Log.i(LOG_TAG,"PreferencesTorBridges writeTo " + filePath + " success");
            } else {
                Toast.makeText(getActivity(),getText(R.string.write_file_error) + " " + filePath,Toast.LENGTH_LONG).show();
            }

            writer = new PrintWriter(filePath);
            for (String line:lines)
                writer.println(line);
            writer.close();
            writer = null;
        } catch (IOException e) {
            Log.e(LOG_TAG,"PreferencesTorBridges writeFile Exception " + e.getMessage());
            Toast.makeText(getActivity(),getText(R.string.write_file_error) + " " + filePath,Toast.LENGTH_LONG).show();
        } finally {
            if (writer != null)writer.close();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnAddBridges:
                addBridges();
                break;
            case R.id.btnRequestBridges:
                GetNewBridges getNewBridges = new GetNewBridges(getActivity());
                getNewBridges.selectTransport();
                break;
        }
    }

    void addBridges() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(false);
        builder.setView(input);

        builder.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<String> bridgesListNew = new LinkedList<>();
                String[] bridgesArrNew = input.getText().toString().split(System.lineSeparator());
                String bridges_custom_file_path = pathVars.appDataDir+"/app_data/tor/bridges_custom.lst";

                if (bridgesArrNew.length!=0) {
                    for (String brgNew : bridgesArrNew) {
                        if (!brgNew.isEmpty()) {
                            bridgesListNew.add(brgNew.trim());
                        }
                    }

                    List<String> persistList = readFile(bridges_custom_file_path);

                    if (persistList!=null) {
                        List<String> retainList = new LinkedList<>(persistList);
                        retainList.retainAll(bridgesListNew);
                        bridgesListNew.removeAll(retainList);
                        persistList.addAll(bridgesListNew);
                        bridgesListNew = persistList;
                    }


                    Collections.sort(bridgesListNew);
                    writeToFile(bridges_custom_file_path,bridgesListNew);

                    boolean useOwnBridges = new PrefManager(getActivity()).getBoolPref("useOwnBridges");
                    if (useOwnBridges) {
                        ownBridgesOperation();
                    } else {
                        rbOwnBridges.performClick();
                    }
                }
            }
        });

        builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        builder.setTitle(R.string.pref_fast_use_tor_bridges_add);
        builder.show();
    }

    public void addRequestedBridges(String bridges) {
        List<String> bridgesListNew = new LinkedList<>();
        String[] bridgesArrNew = bridges.split(System.lineSeparator());
        String bridges_custom_file_path = pathVars.appDataDir+"/app_data/tor/bridges_custom.lst";

        if (bridgesArrNew.length!=0) {
            for (String brgNew : bridgesArrNew) {
                if (!brgNew.isEmpty()) {
                    bridgesListNew.add(brgNew.trim());
                }
            }

            List<String> persistList = readFile(bridges_custom_file_path);

            if (persistList!=null) {
                List<String> retainList = new LinkedList<>(persistList);
                retainList.retainAll(bridgesListNew);
                bridgesListNew.removeAll(retainList);
                persistList.addAll(bridgesListNew);
                bridgesListNew = persistList;
            }


            Collections.sort(bridgesListNew);
            writeToFile(bridges_custom_file_path,bridgesListNew);

            if (!bridges.isEmpty()) {
                if (bridges.contains("obfs4")) {
                    currentBridgesType = "obfs4";
                    if (!spOwnBridges.getSelectedItem().toString().equals("obfs4")){
                        spOwnBridges.setSelection(0);
                    } else {
                        ownBridgesOperation();
                    }
                } else if (bridges.contains("obfs3")) {
                    currentBridgesType = "obfs3";
                    if (!spOwnBridges.getSelectedItem().toString().equals("obfs3")){
                        spOwnBridges.setSelection(1);
                    } else {
                        ownBridgesOperation();
                    }
                } else if (bridges.contains("scramblesuit")) {
                    currentBridgesType = "scramblesuit";
                    if (!spOwnBridges.getSelectedItem().toString().equals("scramblesuit")){
                        spOwnBridges.setSelection(2);
                    } else {
                        ownBridgesOperation();
                    }
                } else if (bridges.contains("meek_lite")) {
                    currentBridgesType = "meek_lite";
                    if (!spOwnBridges.getSelectedItem().toString().equals("meek_lite")){
                        spOwnBridges.setSelection(3);
                    } else {
                        ownBridgesOperation();
                    }
                } else {
                    currentBridgesType = "none";
                    if (!spOwnBridges.getSelectedItem().toString().equals("none")){
                        spOwnBridges.setSelection(4);
                    } else {
                        ownBridgesOperation();
                    }
                }

            }

            boolean useOwnBridges = new PrefManager(getActivity()).getBoolPref("useOwnBridges");

            if (!useOwnBridges) {
                rbOwnBridges.performClick();
            }
        }
    }




    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
            switch (compoundButton.getId()) {
                case R.id.rbNoBridges:
                    if (newValue) {
                        new PrefManager(getActivity()).setBoolPref("useNoBridges",true);
                        new PrefManager(getActivity()).setBoolPref("useDefaultBridges",false);
                        new PrefManager(getActivity()).setBoolPref("useOwnBridges",false);

                        noBridgesOperation();

                        for (int i=0;i<tor_conf.size();i++) {
                            if (tor_conf.get(i).contains("UseBridges")) {
                                String line = tor_conf.get(i);
                                String result = line.replace("1","0");
                                if (!result.equals(line)) {
                                    tor_conf.set(i,result);
                                }
                            }
                        }
                    }

                    break;
                case R.id.rbDefaultBridges:
                    if (newValue) {
                        new PrefManager(getActivity()).setBoolPref("useNoBridges",false);
                        new PrefManager(getActivity()).setBoolPref("useDefaultBridges",true);
                        new PrefManager(getActivity()).setBoolPref("useOwnBridges",false);

                        defaultBridgesOperation();

                        for (int i=0;i<tor_conf.size();i++) {
                            if (tor_conf.get(i).contains("UseBridges")) {
                                String line = tor_conf.get(i);
                                String result = line.replace("0","1");
                                if (!result.equals(line)) {
                                    tor_conf.set(i,result);
                                }
                            }
                        }
                    }

                    break;
                case R.id.rbOwnBridges:
                    if (newValue) {
                        new PrefManager(getActivity()).setBoolPref("useNoBridges",false);
                        new PrefManager(getActivity()).setBoolPref("useDefaultBridges",false);
                        new PrefManager(getActivity()).setBoolPref("useOwnBridges",true);

                        ownBridgesOperation();

                        for (int i=0;i<tor_conf.size();i++) {
                            if (tor_conf.get(i).contains("UseBridges")) {
                                String line = tor_conf.get(i);
                                String result = line.replace("0","1");
                                if (!result.equals(line)) {
                                    tor_conf.set(i,result);
                                }
                            }
                        }
                    }

                    break;
            }

        }
    };

    void noBridgesOperation() {
        rbDefaultBridges.setChecked(false);
        rbOwnBridges.setChecked(false);

        bridgeList.clear();
        currentBridges.clear();
        bridgeAdapter.notifyDataSetChanged();

        tvBridgesListEmpty.setVisibility(View.GONE);
    }

    void defaultBridgesOperation() {
        rbNoBridges.setChecked(false);
        rbOwnBridges.setChecked(false);

        bridgeList.clear();
        anotherBridges.clear();

        bridges_file_path = pathVars.appDataDir+"/app_data/tor/bridges_default.lst";
        List<String> bridges_default = readFile(bridges_file_path);
        String obfsTypeSp = spDefaultBridges.getSelectedItem().toString();

        if (bridges_default==null)
            return;

        for (String line:bridges_default) {
            ObfsBridge obfsBridge;
            if (line.contains(obfsTypeSp)) {
                obfsBridge = new ObfsBridge(line, obfsTypeSp,false);
                if (currentBridges.contains(line)) {
                    obfsBridge.active = true;
                }
                bridgeList.add(obfsBridge);
            } else {
                anotherBridges.add(line);
            }
        }

        bridgeAdapter.notifyDataSetChanged();

        if (bridgeList.isEmpty()) {
            tvBridgesListEmpty.setVisibility(View.VISIBLE);
        } else {
            tvBridgesListEmpty.setVisibility(View.GONE);
        }
    }

    void ownBridgesOperation() {
        rbNoBridges.setChecked(false);
        rbDefaultBridges.setChecked(false);

        bridgeList.clear();
        anotherBridges.clear();

        bridges_file_path = pathVars.appDataDir+"/app_data/tor/bridges_custom.lst";
        List<String> bridges_custom = readFile(bridges_file_path);
        String obfsTypeSp = spOwnBridges.getSelectedItem().toString();
        //Toast.makeText(getActivity(),spOwnBridges.getSelectedItem().toString(),Toast.LENGTH_LONG).show();

        if (bridges_custom==null)
            return;

        for (String line:bridges_custom) {
            ObfsBridge obfsBridge;
            if (!obfsTypeSp.equals("none") && line.contains(obfsTypeSp)) {
                obfsBridge = new ObfsBridge(line,obfsTypeSp,false);
                if (currentBridges.contains(line)) {
                    obfsBridge.active = true;
                }
                bridgeList.add(obfsBridge);
            } else if (obfsTypeSp.equals("none") && !line.contains("obfs4") && !line.contains("obfs3")
                    && !line.contains("scramblesuit") && !line.contains("meek_lite") && !line.isEmpty()) {
                obfsBridge = new ObfsBridge(line,obfsTypeSp,false);
                if (currentBridges.contains(line)) {
                    obfsBridge.active = true;
                }
                bridgeList.add(obfsBridge);
            } else {
                anotherBridges.add(line);
            }
        }

        bridgeAdapter.notifyDataSetChanged();

        if (bridgeList.isEmpty()) {
            tvBridgesListEmpty.setVisibility(View.VISIBLE);
        } else {
            tvBridgesListEmpty.setVisibility(View.GONE);
        }
    }

    AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            switch (adapterView.getId()) {
                case R.id.spDefaultBridges:
                    new PrefManager(getActivity()).setStrPref("defaultBridgesObfs",String.valueOf(i));
                    if (rbDefaultBridges.isChecked())
                        defaultBridgesOperation();
                    break;
                case R.id.spOwnBridges:
                    new PrefManager(getActivity()).setStrPref("ownBridgesObfs",String.valueOf(i));
                    if (rbOwnBridges.isChecked())
                        ownBridgesOperation();
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    class ObfsBridge {
        String bridge;
        String obfsType;
        boolean active;

        ObfsBridge(String bridge, String obfsType, boolean active) {
            this.bridge = bridge;
            this.obfsType = obfsType;
            this.active = active;
        }
    }

    public class BridgeAdapter extends RecyclerView.Adapter<PreferencesTorBridges.BridgeAdapter.BridgeViewHolder> {
        List<ObfsBridge> listBridges;

        LayoutInflater lInflater = (LayoutInflater) Objects.requireNonNull(getActivity()).getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        BridgeAdapter(List<ObfsBridge> listBridges) {
            this.listBridges = listBridges;
        }

        @NonNull
        @Override
        public BridgeAdapter.BridgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = lInflater.inflate(R.layout.item_bridge,parent,false);
            return new BridgeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BridgeAdapter.BridgeViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return listBridges.size();
        }

        ObfsBridge getItem(int position) {
            return listBridges.get(position);
        }

        void setActive(int position, boolean active) {
            ObfsBridge brg = listBridges.get(position);
            brg.active = active;
            listBridges.set(position,brg);
        }

        class BridgeViewHolder extends RecyclerView.ViewHolder {

            TextView tvBridge;
            Switch swBridge;
            ImageButton ibtnBridgeDel;
            LinearLayout llBridge;

            BridgeViewHolder(@NonNull View itemView) {
                super(itemView);

                tvBridge = itemView.findViewById(R.id.tvBridge);
                swBridge = itemView.findViewById(R.id.swBridge);
                swBridge.setOnCheckedChangeListener(onCheckedChangeListener);
                ibtnBridgeDel = itemView.findViewById(R.id.ibtnBridgeDel);
                ibtnBridgeDel.setOnClickListener(onClickListener);
                llBridge = itemView.findViewById(R.id.llBridge);
                llBridge.setOnClickListener(onClickListener);
            }

            void bind(int position) {
                String[] bridgeIP = listBridges.get(position).bridge.split(" ");
                String tvBridgeText;
                if (bridgeIP[0].contains("obfs3") || bridgeIP[0].contains("obfs4")
                        || bridgeIP[0].contains("scramblesuit") || bridgeIP[0].contains("meek_lite")) {
                    tvBridgeText = bridgeIP[0] + " " + bridgeIP[1];
                } else {
                    tvBridgeText = bridgeIP[0];
                }

                tvBridge.setText(tvBridgeText);
                if (listBridges.get(position).active) {
                    swBridge.setChecked(true);
                } else {
                    swBridge.setChecked(false);
                }

            }

            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean newValue) {
                    if (newValue) {
                        String obfsType = getItem(getAdapterPosition()).obfsType;
                        if (!obfsType.equals(currentBridgesType)) {
                            currentBridges.clear();
                            currentBridgesType = obfsType;
                        }

                        boolean unicBridge = true;
                        for (int i=0;i<currentBridges.size();i++) {
                            String brg = currentBridges.get(i);
                            if (brg.equals(getItem(getAdapterPosition()).bridge)) {
                                unicBridge = false;
                                break;
                            }
                        }
                        if (unicBridge)
                            currentBridges.add(getItem(getAdapterPosition()).bridge);
                    } else {
                        for (int i=0;i<currentBridges.size();i++) {
                            String brg = currentBridges.get(i);
                            if (brg.equals(getItem(getAdapterPosition()).bridge)) {
                                currentBridges.remove(i);
                                break;
                            }

                        }
                    }
                    setActive(getAdapterPosition(),newValue);
                }
            };

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.llBridge:
                            editBridge(getAdapterPosition());
                            break;
                        case R.id.ibtnBridgeDel:
                            deleteBridge(getAdapterPosition());
                            break;
                    }
                }
            };

            void editBridge(final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.pref_fast_use_tor_bridges_edit);

                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setSingleLine(false);
                String brgEdit = bridgeList.get(position).bridge;
                final String obfsTypeEdit = bridgeList.get(position).obfsType;
                final boolean activeEdit = bridgeList.get(position).active;
                if (activeEdit) {
                    TopFragment.NotificationDialogFragment commandResult
                            = TopFragment.NotificationDialogFragment.newInstance(getString(R.string.pref_fast_use_tor_bridges_deactivate));
                    commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
                    return;
                }
                input.setText(brgEdit,TextView.BufferType.EDITABLE);
                builder.setView(input);

                builder.setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        ObfsBridge brg = new ObfsBridge(input.getText().toString(),obfsTypeEdit, false);
                        bridgeList.set(position,brg);
                        bridgeAdapter.notifyItemChanged(position);

                        List<String> tmpList = new LinkedList<>();
                        for (ObfsBridge tmpObfs:bridgeList) {
                            tmpList.add(tmpObfs.bridge);
                        }
                        tmpList.addAll(anotherBridges);
                        Collections.sort(tmpList);
                        if (bridges_file_path!=null)
                            writeToFile(bridges_file_path,tmpList);
                    }
                });
                builder.setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }

            void deleteBridge(int position) {
                if (bridgeList.get(position).active) {
                    TopFragment.NotificationDialogFragment commandResult
                            = TopFragment.NotificationDialogFragment.newInstance(getString(R.string.pref_fast_use_tor_bridges_deactivate));
                    commandResult.show(getFragmentManager(),TopFragment.NotificationDialogFragment.TAG_NOT_FRAG);
                    return;
                }
                bridgeList.remove(position);
                bridgeAdapter.notifyItemRemoved(position);

                List<String> tmpList = new LinkedList<>();
                for (ObfsBridge tmpObfs:bridgeList) {
                    tmpList.add(tmpObfs.bridge);
                }
                tmpList.addAll(anotherBridges);
                Collections.sort(tmpList);
                if (bridges_file_path!=null)
                    writeToFile(bridges_file_path,tmpList);
            }
        }
    }

    private void runTorNoRoot() {
        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean showNotification = shPref.getBoolean("swShowNotification",true);
        Intent intent = new Intent(getActivity(), NoRootService.class);
        intent.setAction(NoRootService.actionStartTor);
        intent.putExtra("showNotification",showNotification);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(intent);
        } else {
            getActivity().startService(intent);
        }
    }
}