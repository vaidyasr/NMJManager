/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nmj.nmjmanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nmj.base.NMJActivity;
import com.nmj.db.DbAdapterTvShows;
import com.nmj.functions.AsyncTask;
import com.nmj.functions.MenuItem;
import com.nmj.functions.NMJAdapter;
import com.nmj.functions.NMJDb;
import com.nmj.functions.NMJSource;
import com.nmj.functions.NMJLib;
import com.nmj.nmjmanager.fragments.AccountsFragment;
import com.nmj.nmjmanager.fragments.ContactDeveloperFragment;
import com.nmj.nmjmanager.fragments.MovieDiscoveryViewPagerFragment;
import com.nmj.nmjmanager.fragments.MovieLibraryOverviewFragment;
import com.nmj.nmjmanager.fragments.TvShowLibraryOverviewFragment;
import com.nmj.utils.LocalBroadcastUtils;
import com.nmj.utils.TypefaceUtils;
import com.nmj.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static com.nmj.functions.PreferenceKeys.CONFIRM_BACK_PRESS;
import static com.nmj.functions.PreferenceKeys.LOAD_LAST_DATABASE;
import static com.nmj.functions.PreferenceKeys.STARTUP_SELECTION;
import static com.nmj.functions.PreferenceKeys.TRAKT_FULL_NAME;
import static com.nmj.functions.PreferenceKeys.TRAKT_USERNAME;

@SuppressLint("NewApi")
public class Main extends NMJActivity {

    public static final int MOVIES = 1, SHOWS = 2, MUSIC = 3, SELECT = 4, SOURCE = 5;
    protected ListView mDrawerList;
    AlertDialog alertDialog;
    ArrayList<NMJDb> nmjdb = new ArrayList<>();
    AlertDialog.Builder alertDialogBuilder;
    private ArrayList<NMJSource> nmjsource;
    private EditText ip_address, display_name;
    private int mNumMovies, mNumShows, selectedIndex, mStartup, mNumMusic;
    private Typeface mTfMedium, mTfRegular;
    private DrawerLayout mDrawerLayout, mFilterLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NMJAdapter mDbHelper;
    private DbAdapterTvShows mDbHelperTv;
    private boolean mConfirmExit, mTriedOnce = false;
    private ArrayList<MenuItem> mMenuItems = new ArrayList<MenuItem>();
    private ArrayList<MenuItem> mNMJItems = new ArrayList<MenuItem>();
    private List<ApplicationInfo> mApplicationList;
    private Picasso mPicasso;
    private Context mContext;
    private String mDriveType = "local";
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.filterEquals(new Intent("NMJManager-update-library-count")))
                updateLibraryCounts();
            else if (intent.filterEquals(new Intent("NMJManager-reload-movie-fragment")))
                reloadFragment("frag1");
            else if (intent.filterEquals(new Intent("NMJManager-reload-show-fragment")))
                reloadFragment("frag2");
        }
    };

    private void reloadFragment(String fragment) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(fragment);
        getSupportFragmentManager().beginTransaction().detach(frag).commitAllowingStateLoss();
        getSupportFragmentManager().beginTransaction().attach(frag).commitAllowingStateLoss();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.menu_drawer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.NMJManager_Theme_Overview);

        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        mPicasso = NMJManagerApplication.getPicasso(getApplicationContext());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mConfirmExit = settings.getBoolean(CONFIRM_BACK_PRESS, false);
        mStartup = Integer.valueOf(settings.getString(STARTUP_SELECTION, "1"));
        ip_address = findViewById(R.id.ip_address);

        mDbHelper = NMJManagerApplication.getNMJAdapter();
        mDbHelperTv = NMJManagerApplication.getTvDbAdapter();

        mTfMedium = TypefaceUtils.getRobotoMedium(getApplicationContext());
        mTfRegular = TypefaceUtils.getRoboto(getApplicationContext());

        setupMenuItems(true);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_list_shadow, GravityCompat.START);

        mDrawerList = findViewById(R.id.listView1);
        mDrawerList.setLayoutParams(new FrameLayout.LayoutParams(ViewUtils.getNavigationDrawerWidth(this), FrameLayout.LayoutParams.MATCH_PARENT));
        mDrawerList.setAdapter(new MenuAdapter());
        //
        //PreferenceManager.getDefaultSharedPreferences(this).edit().putString(STORED_DB, "").apply();

        // Attach click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                System.out.println("Selected: " + arg2);
                switch (mMenuItems.get(arg2).getType()) {
                    case MenuItem.HEADER:

                        Intent intent = new Intent(getApplicationContext(), Preferences.class);
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AccountsFragment.class.getName());
                        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, getString(R.string.social));
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, getString(R.string.social));

                        startActivity(intent);
                        break;

                    case MenuItem.SECTION:
                        loadFragment(mMenuItems.get(arg2).getFragment(), 0);
                        break;
                    case MenuItem.NMJ_DB:
                        loadFragment(mMenuItems.get(arg2).getFragment(), arg2);
                        break;
                    case MenuItem.SETTINGS_AREA:

                        Intent smallIntent = new Intent(getApplicationContext(), Preferences.class);
                        if (mMenuItems.get(arg2).getIcon() == R.drawable.ic_help_grey600_24dp) {
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ContactDeveloperFragment.class.getName());
                            smallIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, getString(R.string.menuAboutContact));
                            smallIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_SHORT_TITLE, getString(R.string.menuAboutContact));
                        }

                        startActivity(smallIntent);

                        mDrawerLayout.closeDrawers();

                        break;
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        //System.out.println("Selected Index:" + selectedIndex);
        if (savedInstanceState != null && savedInstanceState.containsKey("selectedIndex")) {
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            loadFragment(selectedIndex, 0);
        } else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("startup")) {
            loadFragment(Integer.parseInt(getIntent().getExtras().getString("startup")), 0);
            //System.out.println("Startup:" + Integer.parseInt(getIntent().getExtras().getString("startup")));

        } else {
            //System.out.println("Startup Variable:" + mStartup);
            loadFragment(mStartup, 0);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_LIBRARY));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_LIBRARY_COUNT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.RELOAD_MOVIE_FRAGMENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.RELOAD_SHOW_FRAGMENT));

        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(LOAD_LAST_DATABASE, true)) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            String last_db = sharedPref.getString("LAST_DB", "");

            //System.out.println("Last DB: " + last_db);
            if (!last_db.equals("")) {
                try {
                    JSONObject jObject = new JSONObject(last_db);
                    NMJLib.setDbPath(NMJLib.getStringFromJSONObject(jObject, "DB_PATH", ""));
                    NMJLib.setDrivePath(NMJLib.getStringFromJSONObject(jObject, "DRIVE_PATH", ""));
                    NMJLib.setNMJPort(NMJLib.getStringFromJSONObject(jObject, "PORT", ""));
                    NMJLib.setNMJServer(NMJLib.getStringFromJSONObject(jObject, "IP_ADDRESS", ""));
                    showToast("Loading last opened database...");
                    LoadDatabase(null);
                } catch (Exception e) {
                    showToast("Exception Occurred: " + e.toString());
                }
            }
        }
    }

    public void loadFragment(int type, int index) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag("frag" + type);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            switch (type) {
                case MOVIES:
                    ft.replace(R.id.content_frame, MovieLibraryOverviewFragment.newInstance(), "frag" + type);
                    break;
                case SHOWS:
                    ft.replace(R.id.content_frame, TvShowLibraryOverviewFragment.newInstance(), "frag" + type);
                    break;
                case MUSIC:
                    ft.replace(R.id.content_frame, MovieDiscoveryViewPagerFragment.newInstance(), "frag" + type);
                    break;
                case SELECT:
                    getNMJServer();
                    break;
                case SOURCE:
                    //System.out.println("Selected: " + nmjsource.get(index - 7).getMachine());
                    NMJLib.setNMJPort(nmjsource.get(index - 7).getPort());
                    NMJLib.setNMJServer(nmjsource.get(index - 7).getMachine());
                    loadDriveData(mDriveType);
                    //ft.replace(R.id.content_frame, MovieLibraryOverviewFragment.newInstance(), "frag" + type);
                    break;
            }
            ft.commit();
        }

        switch (type) {
            case MOVIES:
                setTitle(R.string.chooserMovies);
                break;
            case SHOWS:
                setTitle(R.string.chooserTVShows);
                break;
            case MUSIC:
                setTitle(R.string.drawerMyMusic);
                break;
        }

        selectListIndex(type);

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    protected void getNMJServer() {
        Context context = this;
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.nmj_input, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        alertDialogBuilder.setTitle(getString(R.string.selectLogin));

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput =  promptsView.findViewById(R.id.ip_address);

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void setLastDBValue(int pos) {
        try {
            JSONObject tobj = new JSONObject();
            tobj.put("IP_ADDRESS", NMJLib.getNMJServer());
            tobj.put("PORT", NMJLib.getNMJPort());
            tobj.put("DB_PATH", nmjdb.get(pos).getDbPath());
            tobj.put("DRIVE_PATH", nmjdb.get(pos).getDrivePath());
            tobj.put("JUKEBOX", nmjdb.get(pos).getJukebox());
            tobj.put("NMJ_TYPE", nmjdb.get(pos).getNMJType());
            //System.out.println("To cache : " + tobj.toString());
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("LAST_DB", tobj.toString());
            editor.apply();
        } catch (Exception e) {
            Toast.makeText(this, "Exception Occurred: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected void getNMJDatabase() {
        final AlertDialog dialog;
        LayoutInflater li = LayoutInflater.from(mContext);
        View promptsView = li.inflate(R.layout.nmjdb_select, null);
        alertDialogBuilder.setView(promptsView);
        dialog = alertDialogBuilder.create();
        RelativeLayout relativeLayout = (RelativeLayout) promptsView.findViewById(R.id.segmented_layout);

        ListView listView = (ListView) promptsView.findViewById(R.id.nmjlist);
        //System.out.println("Machine Type: " + NMJLib.getMachineType());
        if (NMJLib.getMachineType().equals(""))
            relativeLayout.setVisibility(View.GONE);
        RadioGroup radioGroup = (RadioGroup) promptsView.findViewById(R.id.segmented_group);
        if (mDriveType.equals("local"))
            radioGroup.check(R.id.local_button);
        else
            radioGroup.check(R.id.network_button);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.local_button:
                        loadDriveData("local");
                        dialog.dismiss();
                        break;
                    case R.id.network_button:
                        loadDriveData("network");
                        dialog.dismiss();
                        break;
                }
            }
        });
        listView.setAdapter(new ListAdapter());
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                if (!nmjdb.get(pos).getJukebox().equals("0") && nmjdb.get(pos).getNMJType().equals("local")) {
                    NMJLib.setDbPath(nmjdb.get(pos).getDbPath());
                    NMJLib.setDrivePath(nmjdb.get(pos).getDrivePath());
                    setLastDBValue(pos);
                    LoadDatabase(dialog);
                } else if (nmjdb.get(pos).getNMJType().equals("network")) {
                    loadDriveDetails(nmjdb.get(pos).getPath(), nmjdb.get(pos).getDeviceType(), dialog);
                } else {
                    showNoJukeBoxMessage();
                }
            }
        });
        dialog.show();
    }

    public void cancel(View v) {
        alertDialog.dismiss();
    }

    public void ok(View v) {
        ip_address = (EditText) alertDialog.findViewById(R.id.ip_address);
        display_name = (EditText) alertDialog.findViewById(R.id.display_name);

        if (ip_address.getText().toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.enterNetworkAddress), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            String stored_db = getPreferences(Context.MODE_PRIVATE).getString("STORED_DB", "");
            JSONArray db;
            JSONObject jObject;
            if (stored_db.equals("")) {
                db = new JSONArray();
            } else {
                jObject = new JSONObject(stored_db);
                db = jObject.getJSONArray("machines");
            }

            JSONObject jobj = new JSONObject();
            jobj.put("IP_ADDRESS", ip_address.getText().toString());
            jobj.put("PORT", "5678");
            jobj.put("DISPLAY_NAME", display_name.getText().toString());
            db.put(jobj);
            JSONObject jobj1 = new JSONObject();
            jobj1.put("machines", db);
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("STORED_DB", jobj1.toString());
            editor.apply();
            System.out.println("Saved Settings: " + jobj1.toString());
        } catch (Exception e) {
            showDatabaseLoadError("Error saving database", e.toString());
        }
        Toast.makeText(this, ip_address.getText().toString() + " added", Toast.LENGTH_LONG).show();
        setupMenuItems(false);
        ((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
        alertDialog.dismiss();
    }

    public void LoadDatabase(AlertDialog dialog) {
        //System.out.println("DBPath : " + NMJLib.getDbPath());
        //System.out.println("Drivepath : " + NMJLib.getDrivePath());
        showToast("Checking Server Script Changes...");
        updatePHPScript();

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                NMJLib.setLibrary(NMJManagerApplication.getContext(), mDbHelper);
                return null;
            }

            protected void onPostExecute(Void result) {
                LocalBroadcastUtils.updateLibraryCount(NMJManagerApplication.getContext());
                //System.out.println("Total : " + NMJManagerApplication.getNMJAdapter().getLibrary().size());
                if (mStartup == 1)
                    LocalBroadcastUtils.loadMovieLibrary(NMJManagerApplication.getContext());
                else if (mStartup == 2)
                    LocalBroadcastUtils.loadTvShowLibrary(NMJManagerApplication.getContext());
            }
        };
        task.execute();

        if(dialog != null)
            dialog.dismiss();
    }

    public void loadDriveData(String driveType) {
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Select a NMJ database");
        mDriveType = driveType;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            JSONObject jObject;
            JSONArray jArray;
            String error, jString;

            protected Void doInBackground(Void... params) {
                nmjdb.clear();
                try {
                    jObject = NMJLib.getJSONObject(mContext, NMJLib.getNMJServerPHPURL() +
                            "action=getDrives&type=" + mDriveType);
                    NMJLib.setMachineType(NMJLib.getStringFromJSONObject(jObject, "machine", ""));
                    jArray = jObject.getJSONArray("data");
                    System.out.println("Output: " + jArray.toString());
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject dObject = jArray.getJSONObject(i);
                        NMJDb tmpdb = new NMJDb(NMJManagerApplication.getContext(),
                                NMJLib.getStringFromJSONObject(dObject, "name", ""),
                                NMJLib.getStringFromJSONObject(dObject, "dbpath", ""),
                                NMJLib.getStringFromJSONObject(dObject, "drivepath", ""));
                        nmjdb.add(i, tmpdb);
                        nmjdb.get(i).setJukebox(NMJLib.getStringFromJSONObject(dObject, "jukebox", "0"));
                        nmjdb.get(i).setPath(NMJLib.getStringFromJSONObject(dObject, "path", ""));
                        nmjdb.get(i).setDeviceType(NMJLib.getStringFromJSONObject(dObject, "device_type", ""));
                        nmjdb.get(i).setNMJType(NMJLib.getStringFromJSONObject(dObject, "type", "local"));
                    }
                } catch (Exception e) {
                    error = e.toString();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                mNMJItems.clear();
                if (nmjdb.size() != 0) {
                    for (int i = 0; i < nmjdb.size(); i++) {
                        mNMJItems.add(new MenuItem(nmjdb.get(i).getName(), MenuItem.SECTION, nmjdb.get(i).getJukebox().equals("0") ? R.drawable.ic_selectsource_icon_harddisk_24dp : R.drawable.ic_selectsource_icon_harddisk_jukebox_24dp));
                    }
                    if (mNMJItems.size() != 0)
                        getNMJDatabase();
                } else
                    showDatabaseLoadError("Error loading database", error);
            }
        };
        task.execute();
    }

    public void updatePHPScript(){
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            JSONObject jObject;
            JSONArray jArray;
            String error, status = "", message;
            ArrayList<NMJDb> nmdb = new ArrayList<>();

            protected Void doInBackground(Void... params) {
                try {
                    String serverURL = "http://www.pchportal.duckdns.org/NMJManager/getData.php?action=getServerMD5sum";
                    String clientURL = NMJLib.getNMJServerPHPURL() + "action=getMD5sum";
                    jObject = NMJLib.getJSONObject(mContext, serverURL);
                    String serverMD5 = NMJLib.getStringFromJSONObject(jObject, "md5sum", "");
                    jObject = NMJLib.getJSONObject(mContext, clientURL);
                    String clientMD5 = NMJLib.getStringFromJSONObject(jObject, "md5sum", "");

                    if (!serverMD5.equals(clientMD5)){
                        String updateURL = NMJLib.getNMJServerPHPURL() + "action=updateProgram";
                        jObject = NMJLib.getJSONObject(mContext, updateURL);
                        status = NMJLib.getStringFromJSONObject(jObject, "status", "");
                        if (!status.equals("success"))
                            error = NMJLib.getStringFromJSONObject(jObject, "error", "");
                    }

                } catch (Exception e) {
                    error = e.toString();
                    System.out.println("Exception: " + error);
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                if(!status.equals("success") && !status.equals("")){
                } else if(status.equals("success")) {
                    showToast("Updated Server script successfully...");
                }
            }
        };
        task.execute();
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void loadDriveDetails(final String path, final String deviceType, final AlertDialog dialog) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            JSONObject jObject;
            JSONArray jArray;
            String error, status, message;
            ArrayList<NMJDb> nmdb = new ArrayList<>();

            protected Void doInBackground(Void... params) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("path", path);
                    obj.put("device_type", deviceType);

                    jObject = NMJLib.getJSONObject(mContext, NMJLib.getNMJServerPHPURL() +
                            "action=getDriveDetails&DATA=" + obj.toString());
                    System.out.println("Output: " + jObject.toString());
                    NMJDb tmpdb = new NMJDb(NMJManagerApplication.getContext(),
                            NMJLib.getStringFromJSONObject(jObject, "name", ""),
                            NMJLib.getStringFromJSONObject(jObject, "dbpath", ""),
                            NMJLib.getStringFromJSONObject(jObject, "drivepath", ""));
                    nmdb.add(0, tmpdb);
                    nmdb.get(0).setJukebox(NMJLib.getStringFromJSONObject(jObject, "jukebox", "0"));
                    status = NMJLib.getStringFromJSONObject(jObject, "status", "success");
                    message = NMJLib.getStringFromJSONObject(jObject, "message", "");

                } catch (Exception e) {
                    error = e.toString();
                    System.out.println("Exception: " + error);
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                if(status.equals("success")){
                    if (!nmdb.get(0).getJukebox().equals("0")) {
                        NMJLib.setDbPath(nmdb.get(0).getDbPath());
                        NMJLib.setDrivePath(nmdb.get(0).getDrivePath());
                        LoadDatabase(dialog);
                    } else {
                        showNoJukeBoxMessage();
                    }
                } else {
                    showDatabaseLoadError("Error loading database", message);
                }
            }
        };
        task.execute();
    }

    public void search(View v) {
        TreeSet<String> uniqueSources = new TreeSet<String>();
        final CharSequence[] items = new CharSequence[uniqueSources.size() + 1];
        items[items.length - 1] = getString(R.string.scanForSources);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.addNMJDatabase));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == (items.length - 1)) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), SearchForNetworkShares.class);
                    startActivity(intent);
                } else {
                    showUserDialog(items, which);
                }
            }
        });
        builder.show();
    }

    private void showUserDialog(final CharSequence[] items, final int which) {

    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        if (!newIntent.hasExtra("fromUpdate")) {
            Intent i;
            if (selectedIndex == MOVIES)
                i = new Intent("NMJManager-movie-actor-search");
            else // TV shows
                i = new Intent("NMJManager-shows-actor-search");
            i.putExtras(newIntent.getExtras());
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("selectedIndex", selectedIndex);
    }

    public void setupMenuItems(boolean refreshThirdPartyApps) {
        mMenuItems.clear();

        // Menu header
        mMenuItems.add(new MenuItem(null, -1, MenuItem.HEADER, null));

        // Regular menu items
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyMovies), mNumMovies, MenuItem.SECTION, null, MOVIES, R.drawable.ic_movie_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyTvShows), mNumShows, MenuItem.SECTION, null, SHOWS, R.drawable.ic_tv_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.drawerMyMusic), mNumMusic, MenuItem.SECTION, null, MUSIC, R.drawable.ic_music_grey600_24dp));

        mMenuItems.add(new MenuItem(MenuItem.SEPARATOR_EXTRA_PADDING));
        mMenuItems.add(new MenuItem(getString(R.string.drawerSelectDB), -1, MenuItem.SECTION, null, SELECT, R.drawable.ic_add_grey600_24dp));

        try {
            nmjsource = new ArrayList<>();
            JSONObject jObject;
            JSONArray db;
            String stored_db = getPreferences(Context.MODE_PRIVATE).getString("STORED_DB", "");
            if (!stored_db.equals("")) {
                jObject = new JSONObject(stored_db);
                db = jObject.getJSONArray("machines");
                if (db.length() > 0)
                    mMenuItems.add(new MenuItem("Available", -1, MenuItem.SUB_HEADER, null));

                for (int i = 0; i < db.length(); i++) {
                    JSONObject dObject = db.getJSONObject(i);
                    NMJSource tmpdb = new NMJSource(NMJManagerApplication.getContext(),
                            NMJLib.getStringFromJSONObject(dObject, "IP_ADDRESS", ""),
                            NMJLib.getStringFromJSONObject(dObject, "PORT", "80"),
                            NMJLib.getStringFromJSONObject(dObject, "DISPLAY_NAME", ""));
                    nmjsource.add(i, tmpdb);
                    mMenuItems.add(new MenuItem(nmjsource.get(i).getDisplayName().equals("") ? nmjsource.get(i).getMachine() : nmjsource.get(i).getDisplayName() + " (" + nmjsource.get(i).getMachine() + ")", -1, MenuItem.NMJ_DB, null, SOURCE, R.drawable.ic_db_grey600_24dp));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Exception Occurred: " + e.toString(), Toast.LENGTH_LONG).show();
        }
        mMenuItems.add(new MenuItem(MenuItem.SEPARATOR_EXTRA_PADDING));

        mMenuItems.add(new MenuItem(getString(R.string.settings_name), MenuItem.SETTINGS_AREA, R.drawable.ic_settings_grey600_24dp));
        mMenuItems.add(new MenuItem(getString(R.string.menuAboutContact), MenuItem.SETTINGS_AREA, R.drawable.ic_help_grey600_24dp));
    }

    private void getDeleteConfirmation(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.remove_selected_entry);
        builder.setMessage(R.string.areYouSure);

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Selected: " + position);
                deleteMachine(position);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void deleteMachine(int position) {
        String stored_db = getPreferences(Context.MODE_PRIVATE).getString("STORED_DB", "");
        JSONObject jObject;
        JSONArray db;
        if (!stored_db.equals("")) {
            try {
                jObject = new JSONObject(stored_db);
                db = jObject.getJSONArray("machines");
                db.remove(position - 7);
                //System.out.println("Available Machines: " + jObject.toString());
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("STORED_DB", jObject.toString());
                editor.apply();
                Toast.makeText(this, nmjsource.get(position - 7).getMachine() + " deleted", Toast.LENGTH_LONG).show();
                setupMenuItems(false);
                ((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
            } catch (Exception e) {
                showDatabaseLoadError("Error deleting database", nmjsource.get(position - 7).getMachine() + ":" + e.toString());
            }
        }
    }

    protected void selectListIndex(int index) {
        if (mMenuItems.get(index).getType() == MenuItem.SECTION) {
            selectedIndex = mMenuItems.get(index).getFragment();
            mDrawerList.setItemChecked(index, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLibraryCounts();
    }

    private void updateLibraryCounts() {
        new Thread() {
            @Override
            public void run() {
                try {
                    mNumMovies = mDbHelper.getMovieCount();
                    mNumShows = mDbHelper.getShowCount();
                    mNumMusic = mDbHelper.getMusicCount();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupMenuItems(false);
                            ((BaseAdapter) mDrawerList.getAdapter()).notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                }
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.openDrawer(mDrawerList);
                } else {
                    mDrawerLayout.closeDrawer(mDrawerList);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
/*        if (mStartup == 0 && !mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer)) && NMJLib.isTablet(this)) { // Welcome screen
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.setClass(getApplicationContext(), Welcome.class);
            startActivity(i);
            finish();
            return;
        }*/

        if (!mDrawerLayout.isDrawerOpen(findViewById(R.id.left_drawer))) {
            if (mConfirmExit) {
                if (mTriedOnce) {
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
                    mTriedOnce = true;
                }
            } else {
                finish();
            }
        } else {
            mDrawerLayout.closeDrawers();
        }
    }

    private void showDatabaseLoadError(String title, String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Setting Dialog Title
        builder.setTitle(title);

        // Setting Dialog Message
        builder.setMessage(error);

        // Setting OK Button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to execute after dialog closed
                //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        Button nbutton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
        nbutton.setTextColor(Color.WHITE);
        // Showing Alert Message
    }

    private void showNoJukeBoxMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Setting Dialog Title
        builder.setTitle("Launch Jukebox Manager");

        // Setting Dialog Message
        builder.setMessage("Please proceed from your Popcorn Hour Jukebox Manager to create NMJ for this device.");

        // Setting OK Button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to execute after dialog closed
                //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        Button nbutton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
        nbutton.setTextColor(Color.WHITE);
    }

    public class ListAdapter extends BaseAdapter {

        ViewHolder holder;
        private String mBackdropPath;
        private LayoutInflater mInflater;

        public ListAdapter() {
            mInflater = LayoutInflater.from(getApplicationContext());
            mBackdropPath = NMJLib.getRandomBackdropPath(getApplicationContext());
        }

        @Override
        public int getCount() {
            return mNMJItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.menu_drawer_item, parent, false);

            // Icon
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(mNMJItems.get(position).getIcon());

            // Title
            TextView title = (TextView) convertView.findViewById(R.id.title);
            title.setText(mNMJItems.get(position).getTitle());
            title.setTypeface(mTfMedium);

            // Description
            TextView description = (TextView) convertView.findViewById(R.id.count);
            description.setTypeface(mTfRegular);
            description.setVisibility(View.GONE);

            int color = Color.parseColor("#FFFFFF");

            title.setTextColor(color);
            //icon.setColorFilter(color);
            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView title;
        }
    }

    public class MenuAdapter extends BaseAdapter {

        private String mBackdropPath;
        private LayoutInflater mInflater;

        public MenuAdapter() {
            mInflater = LayoutInflater.from(getApplicationContext());
            mBackdropPath = NMJLib.getRandomBackdropPath(getApplicationContext());
        }

        @Override
        public int getCount() {
            return mMenuItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 6;
        }

        @Override
        public int getItemViewType(int position) {
            switch (mMenuItems.get(position).getType()) {
                case MenuItem.HEADER:
                    return 0;
                case MenuItem.SEPARATOR:
                    return 1;
                case MenuItem.SEPARATOR_EXTRA_PADDING:
                    return 2;
                case MenuItem.SUB_HEADER:
                    return 3;
                case MenuItem.SECTION:
                case MenuItem.NMJ_DB:
                    return 4;
                default:
                    return 5;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            int type = mMenuItems.get(position).getType();
            return !(type == MenuItem.SEPARATOR ||
                    type == MenuItem.SEPARATOR_EXTRA_PADDING ||
                    type == MenuItem.SUB_HEADER);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (mMenuItems.get(position).getType() == MenuItem.HEADER) {
                convertView = mInflater.inflate(R.layout.menu_drawer_header, parent, false);

                final String fullName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(TRAKT_FULL_NAME, "");
                final String userName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(TRAKT_USERNAME, "");
                final ImageView backgroundImage = ((ImageView) convertView.findViewById(R.id.userCover));
                final ImageView userImage = ((ImageView) convertView.findViewById(R.id.userPhoto));
                final ImageView plusIcon = ((ImageView) convertView.findViewById(R.id.plus_icon));
                final TextView realName = ((TextView) convertView.findViewById(R.id.real_name));
                final TextView userNameTextField = ((TextView) convertView.findViewById(R.id.username));

                realName.setTypeface(mTfMedium);
                userNameTextField.setTypeface(mTfRegular);

                // Full name
                realName.setText(!TextUtils.isEmpty(fullName) ? fullName : "");

                // User name
                if (!TextUtils.isEmpty(userName)) {
                    userNameTextField.setText(String.format(getString(R.string.logged_in_as), userName));
                    plusIcon.setVisibility(View.GONE);
                } else {
                    userNameTextField.setText(R.string.sign_in_with_trakt);
                    plusIcon.setVisibility(View.VISIBLE);
                }

                // This should be loaded in the background, but doesn't matter much at the moment
                Bitmap src = BitmapFactory.decodeFile(new File(NMJManagerApplication.getCacheFolder(getApplicationContext()), "avatar.jpg").getAbsolutePath());
                if (src != null) {
                    RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getResources(), src);
                    dr.setCornerRadius(Math.min(dr.getMinimumWidth(), dr.getMinimumHeight()));
                    dr.setAntiAlias(true);
                    userImage.setImageDrawable(dr);
                } else {
                    userImage.setVisibility(View.GONE);
                }

                // Background image
                if (!TextUtils.isEmpty(mBackdropPath))
                    mPicasso.load(mBackdropPath)
                            .resize(NMJLib.convertDpToPixels(getApplicationContext(), 320),
                                    NMJLib.convertDpToPixels(getApplicationContext(), 180))
                            .into(backgroundImage);
                else
                    mPicasso.load(R.drawable.default_menu_backdrop)
                            .resize(NMJLib.convertDpToPixels(getApplicationContext(), 320),
                                    NMJLib.convertDpToPixels(getApplicationContext(), 180))
                            .into(backgroundImage);

                // Dark color filter on the background image
                backgroundImage.setColorFilter(Color.parseColor("#50181818"), android.graphics.PorterDuff.Mode.SRC_OVER);

            } else if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR) {
                convertView = mInflater.inflate(R.layout.menu_drawer_separator, parent, false);
            } else if (mMenuItems.get(position).getType() == MenuItem.SEPARATOR_EXTRA_PADDING) {
                convertView = mInflater.inflate(R.layout.menu_drawer_separator_extra_padding, parent, false);
            } else if (mMenuItems.get(position).getType() == MenuItem.SUB_HEADER) {
                convertView = mInflater.inflate(R.layout.menu_drawer_header_item, parent, false);
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);
            } else if (mMenuItems.get(position).getType() == MenuItem.SECTION) {
                convertView = mInflater.inflate(R.layout.menu_drawer_item, parent, false);

                // Icon
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                icon.setImageResource(mMenuItems.get(position).getIcon());

                // Title
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);

                // Description
                TextView description = (TextView) convertView.findViewById(R.id.count);
                description.setTypeface(mTfRegular);

                if (mMenuItems.get(position).getType() == MenuItem.SECTION &&
                        mMenuItems.get(position).getFragment() == selectedIndex) {
                    convertView.setBackgroundColor(Color.parseColor("#e8e8e8"));

                    int color = Color.parseColor("#3f51b5");

                    title.setTextColor(color);
                    description.setTextColor(color);
                    icon.setColorFilter(color);
                } else {
                    int color = Color.parseColor("#DD000000");

                    title.setTextColor(color);
                    description.setTextColor(color);
                    icon.setColorFilter(Color.parseColor("#999999"));
                }

                if (mMenuItems.get(position).getCount() >= 0)
                    description.setText(String.valueOf(mMenuItems.get(position).getCount()));
                else
                    description.setVisibility(View.GONE);
            } else {
                convertView = mInflater.inflate(R.layout.menu_drawer_small_item, parent, false);

                // Icon
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

                icon.setImageResource(mMenuItems.get(position).getIcon());
                icon.setColorFilter(Color.parseColor("#737373"));

                ImageButton deletebutton = (ImageButton) convertView.findViewById(R.id.delete_button);
                if (mMenuItems.get(position).getType() == MenuItem.NMJ_DB) {
                    deletebutton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getDeleteConfirmation(position);
                        }
                    });
                } else {
                    deletebutton.setVisibility(View.GONE);
                }

                // Title
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(mMenuItems.get(position).getTitle());
                title.setTypeface(mTfMedium);
            }

            return convertView;
        }
    }
}