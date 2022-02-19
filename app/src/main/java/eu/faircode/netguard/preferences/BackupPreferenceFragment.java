package eu.faircode.netguard.preferences;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.BuildConfig;
import eu.faircode.netguard.DatabaseHelper;
import eu.faircode.netguard.DownloadTask;
import eu.faircode.netguard.R;
import eu.faircode.netguard.ReceiverAutostart;
import eu.faircode.netguard.ServiceSinkhole;
import eu.faircode.netguard.Util;

public class BackupPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = "NetGuard.Settings";
    ActivitySettings activitySettings;
    private Preference hostsImportPref;
    private Preference hostsImportAppendPref;
    private EditTextPreference hostsUrlPref;
    private Preference hostsDownloadPref;
    private SharedPreferences prefs;
    private final ActivityResultLauncher<String> handleExportLauncher =
            registerForActivityResult(new HandleExport(), this::handleExportResult);
    private final ActivityResultLauncher<String[]> openHostsLauncher =
            registerForActivityResult(new OpenDocumentBase(), result ->
                    handleHostsResult(result, false));
    private final ActivityResultLauncher<String[]> openHostsAppendLauncher =
            registerForActivityResult(new OpenDocumentBase(), result ->
                    handleHostsResult(result, true));
    private final ActivityResultLauncher<String[]> handleImportLauncher =
            registerForActivityResult(new OpenDocumentBase(), this::handleImport);

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_backup);
        activitySettings = (ActivitySettings) getActivity();
        prefs = PreferenceManager.getDefaultSharedPreferences(activitySettings);

        final Preference exportPref = findPreference("export");
        exportPref.setOnPreferenceClickListener(preference -> {
            final String exportInput = "netguard_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date().getTime()) + ".xml";
            handleExportLauncher.launch(exportInput);
            return true;
        });

        final Preference importPref = findPreference("import");
        importPref.setOnPreferenceClickListener(preference -> {
            handleImportLauncher.launch(null);
            return true;
        });

        hostsImportPref = findPreference("hosts_import");
        hostsImportAppendPref = findPreference("hosts_import_append");
        hostsUrlPref = findPreference("hosts_url");
        hostsDownloadPref = findPreference("hosts_download");

        if (Util.isPlayStoreInstall(activitySettings)) {
            Log.i(TAG, "Play store install");
            hostsImportPref.setVisible(false);
            hostsImportAppendPref.setVisible(false);
            hostsUrlPref.setVisible(false);
            hostsDownloadPref.setVisible(false);
        } else {
            final String last_import = prefs.getString("hosts_last_import", null);
            final String last_download = prefs.getString("hosts_last_download", null);
            if (last_import != null)
                hostsImportPref.setSummary(getString(R.string.msg_import_last, last_import));
            if (last_download != null)
                hostsDownloadPref.setSummary(getString(R.string.msg_download_last, last_download));

            // https://github.com/Free-Software-for-Android/AdAway/wiki/HostsSources
            hostsImportPref.setOnPreferenceClickListener(preference -> {
                openHostsLauncher.launch(null);
                return true;
            });
            hostsImportAppendPref.setEnabled(hostsImportPref.isEnabled());
            hostsImportAppendPref.setOnPreferenceClickListener(preference -> {
                openHostsAppendLauncher.launch(null);
                return true;
            });

            // Handle hosts file download
            hostsUrlPref.setSummary(hostsUrlPref.getText());
            hostsUrlPref.setOnPreferenceChangeListener((preference, newValue) -> {
                hostsUrlPref.setSummary(prefs.getString("hosts_url", BuildConfig.HOSTS_FILE_URI));
                return true;
            });
            hostsDownloadPref.setOnPreferenceClickListener(preference -> {
                String hosts_url = hostsUrlPref.getText();
                if ("https://www.netguard.me/hosts".equals(hosts_url)) {
                    hosts_url = BuildConfig.HOSTS_FILE_URI;
                }
                runDownloadTask(hosts_url);
                return true;
            });
        }
    }

    private void runDownloadTask(final String hostsUrl) {
        final File tmp = new File(activitySettings.getFilesDir(), "hosts.tmp");
        final File hosts = new File(activitySettings.getFilesDir(), "hosts.txt");
        try {
            new DownloadTask(activitySettings, new URL(hostsUrl), tmp, new DownloadTask.Listener() {
                @Override
                public void onCompleted() {
                    if (hosts.exists()) hosts.delete();
                    tmp.renameTo(hosts);

                    final String last = SimpleDateFormat.getDateTimeInstance().format(new Date().getTime());
                    prefs.edit().putString("hosts_last_download", last).apply();
                    hostsDownloadPref.setSummary(getString(R.string.msg_download_last, last));
                    Toast.makeText(activitySettings, R.string.msg_downloaded, Toast.LENGTH_LONG).show();
                    ServiceSinkhole.reload("hosts file download", activitySettings, false);
                }

                @Override
                public void onCancelled() {
                    if (tmp.exists()) tmp.delete();
                }

                @Override
                public void onException(final Throwable ex) {
                    if (tmp.exists()) tmp.delete();
                    Toast.makeText(activitySettings, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final MalformedURLException ex) {
            Toast.makeText(activitySettings, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void handleExportResult(final Uri uri) {
        new AsyncTask<Object, Object, Throwable>() {
            @Override
            protected Throwable doInBackground(final Object... objects) {
                OutputStream out = null;
                try {
                    Log.i(TAG, "Writing URI=" + uri);
                    out = activitySettings.getContentResolver().openOutputStream(uri);
                    xmlExport(out);
                    return null;
                } catch (final Throwable ex) {
                    Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                    return ex;
                } finally {
                    if (out != null)
                        try {
                            out.close();
                        } catch (final IOException ex) {
                            Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                        }
                }
            }

            @Override
            protected void onPostExecute(final Throwable ex) {
                if (ex == null)
                    Toast.makeText(activitySettings, R.string.msg_completed, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(activitySettings, ex.toString(), Toast.LENGTH_LONG).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleHostsResult(final Uri uri, final boolean append) {
        new AsyncTask<Object, Object, Throwable>() {
            @Override
            protected Throwable doInBackground(final Object... objects) {
                final File hosts = new File(activitySettings.getFilesDir(), "hosts.txt");

                FileOutputStream out = null;
                InputStream in = null;
                try {
                    Log.i(TAG, "Reading URI=" + uri);
                    final ContentResolver resolver = activitySettings.getContentResolver();
                    final String[] streamTypes = resolver.getStreamTypes(uri, "*/*");
                    final String streamType = (streamTypes == null || streamTypes.length == 0 ? "*/*" : streamTypes[0]);
                    final AssetFileDescriptor descriptor = resolver.openTypedAssetFileDescriptor(uri, streamType, null);
                    in = descriptor.createInputStream();
                    out = new FileOutputStream(hosts, append);

                    int len;
                    long total = 0;
                    final byte[] buf = new byte[4096];
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                        total += len;
                    }
                    Log.i(TAG, "Copied bytes=" + total);

                    return null;
                } catch (final Throwable ex) {
                    Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                    return ex;
                } finally {
                    if (out != null)
                        try {
                            out.close();
                        } catch (final IOException ex) {
                            Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                        }
                    if (in != null)
                        try {
                            in.close();
                        } catch (final IOException ex) {
                            Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                        }
                }
            }

            @Override
            protected void onPostExecute(final Throwable ex) {
                if (ex == null) {
                    final String last = SimpleDateFormat.getDateTimeInstance().format(new Date().getTime());
                    prefs.edit().putString("hosts_last_import", last).apply();
                    hostsImportPref.setSummary(getString(R.string.msg_import_last, last));
                    Toast.makeText(activitySettings, R.string.msg_completed, Toast.LENGTH_LONG).show();
                    ServiceSinkhole.reload("hosts import", activitySettings, false);
                } else {
                    Toast.makeText(activitySettings, ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleImport(final Uri uri) {
        new AsyncTask<Object, Object, Throwable>() {
            @Override
            protected Throwable doInBackground(final Object... objects) {
                InputStream in = null;
                try {
                    Log.i(TAG, "Reading URI=" + uri);
                    final ContentResolver resolver = activitySettings.getContentResolver();
                    final String[] streamTypes = resolver.getStreamTypes(uri, "*/*");
                    final String streamType = (streamTypes == null || streamTypes.length == 0 ? "*/*" : streamTypes[0]);
                    final AssetFileDescriptor descriptor = resolver.openTypedAssetFileDescriptor(uri, streamType, null);
                    in = descriptor.createInputStream();
                    xmlImport(in);
                    return null;
                } catch (final Throwable ex) {
                    Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                    return ex;
                } finally {
                    if (in != null)
                        try {
                            in.close();
                        } catch (IOException ex) {
                            Log.e(TAG, ex + "\n" + Log.getStackTraceString(ex));
                        }
                }
            }

            @Override
            protected void onPostExecute(final Throwable ex) {
                if (ex == null) {
                    Toast.makeText(activitySettings, R.string.msg_completed, Toast.LENGTH_LONG).show();
                    ServiceSinkhole.reloadStats("import", activitySettings);
                    // Update theme, request permissions
                    ActivityCompat.recreate(activitySettings);
                } else
                    Toast.makeText(activitySettings, ex.toString(), Toast.LENGTH_LONG).show();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void xmlExport(final OutputStream out) throws IOException {
        final XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(out, "UTF-8");
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, "netguard");

        serializer.startTag(null, "application");
        xmlExport(prefs, serializer);
        serializer.endTag(null, "application");

        serializer.startTag(null, "wifi");
        xmlExport(getSharedPreferences("wifi"), serializer);
        serializer.endTag(null, "wifi");

        serializer.startTag(null, "mobile");
        xmlExport(getSharedPreferences("other"), serializer);
        serializer.endTag(null, "mobile");

        serializer.startTag(null, "screen_wifi");
        xmlExport(getSharedPreferences("screen_wifi"), serializer);
        serializer.endTag(null, "screen_wifi");

        serializer.startTag(null, "screen_other");
        xmlExport(getSharedPreferences("screen_other"), serializer);
        serializer.endTag(null, "screen_other");

        serializer.startTag(null, "roaming");
        xmlExport(getSharedPreferences("roaming"), serializer);
        serializer.endTag(null, "roaming");

        serializer.startTag(null, "lockdown");
        xmlExport(getSharedPreferences("lockdown"), serializer);
        serializer.endTag(null, "lockdown");

        serializer.startTag(null, "apply");
        xmlExport(getSharedPreferences("apply"), serializer);
        serializer.endTag(null, "apply");

        serializer.startTag(null, "notify");
        xmlExport(getSharedPreferences("notify"), serializer);
        serializer.endTag(null, "notify");

        serializer.startTag(null, "filter");
        filterExport(serializer);
        serializer.endTag(null, "filter");

        serializer.startTag(null, "forward");
        forwardExport(serializer);
        serializer.endTag(null, "forward");

        serializer.endTag(null, "netguard");
        serializer.endDocument();
        serializer.flush();
    }

    private void xmlExport(final SharedPreferences prefs, final XmlSerializer serializer) throws IOException {
        final Map<String, ?> settings = prefs.getAll();
        for (final String key : settings.keySet()) {
            final Object value = settings.get(key);

            if ("imported".equals(key))
                continue;
            serializer.startTag(null, "setting");
            serializer.attribute(null, "key", key);
            if (value instanceof Boolean) {
                serializer.attribute(null, "type", "boolean");
                serializer.attribute(null, "value", value.toString());
            } else if (value instanceof Integer) {
                serializer.attribute(null, "type", "integer");
                serializer.attribute(null, "value", value.toString());
            } else if (value instanceof String) {
                serializer.attribute(null, "type", "string");
                serializer.attribute(null, "value", value.toString());
            } else if (value instanceof Set) {
                final Set<String> set = (Set<String>) value;
                serializer.attribute(null, "type", "set");
                serializer.attribute(null, "value", TextUtils.join("\n", set));
            } else {
                Log.e(TAG, "Unknown key=" + key);
            }
            serializer.endTag(null, "setting");
        }
    }

    private void filterExport(final XmlSerializer serializer) throws IOException {
        try (final Cursor cursor = DatabaseHelper.getInstance(activitySettings).getAccess()) {
            final int colUid = cursor.getColumnIndex("uid");
            final int colVersion = cursor.getColumnIndex("version");
            final int colProtocol = cursor.getColumnIndex("protocol");
            final int colDAddr = cursor.getColumnIndex("daddr");
            final int colDPort = cursor.getColumnIndex("dport");
            final int colTime = cursor.getColumnIndex("time");
            final int colBlock = cursor.getColumnIndex("block");
            while (cursor.moveToNext())
                for (final String pkg : getPackages(cursor.getInt(colUid))) {
                    serializer.startTag(null, "rule");
                    serializer.attribute(null, "pkg", pkg);
                    serializer.attribute(null, "version", Integer.toString(cursor.getInt(colVersion)));
                    serializer.attribute(null, "protocol", Integer.toString(cursor.getInt(colProtocol)));
                    serializer.attribute(null, "daddr", cursor.getString(colDAddr));
                    serializer.attribute(null, "dport", Integer.toString(cursor.getInt(colDPort)));
                    serializer.attribute(null, "time", Long.toString(cursor.getLong(colTime)));
                    serializer.attribute(null, "block", Integer.toString(cursor.getInt(colBlock)));
                    serializer.endTag(null, "rule");
                }
        }
    }

    private void forwardExport(final XmlSerializer serializer) throws IOException {
        try (final Cursor cursor = DatabaseHelper.getInstance(activitySettings).getForwarding()) {
            final int colProtocol = cursor.getColumnIndex("protocol");
            final int colDPort = cursor.getColumnIndex("dport");
            final int colRAddr = cursor.getColumnIndex("raddr");
            final int colRPort = cursor.getColumnIndex("rport");
            final int colRUid = cursor.getColumnIndex("ruid");
            while (cursor.moveToNext())
                for (final String pkg : getPackages(cursor.getInt(colRUid))) {
                    serializer.startTag(null, "port");
                    serializer.attribute(null, "pkg", pkg);
                    serializer.attribute(null, "protocol", Integer.toString(cursor.getInt(colProtocol)));
                    serializer.attribute(null, "dport", Integer.toString(cursor.getInt(colDPort)));
                    serializer.attribute(null, "raddr", cursor.getString(colRAddr));
                    serializer.attribute(null, "rport", Integer.toString(cursor.getInt(colRPort)));
                    serializer.endTag(null, "port");
                }
        }
    }

    private String[] getPackages(final int uid) {
        if (uid == 0)
            return new String[]{"root"};
        else if (uid == 1013)
            return new String[]{"mediaserver"};
        else if (uid == 9999)
            return new String[]{"nobody"};
        else {
            final String[] pkgs = activitySettings.getPackageManager().getPackagesForUid(uid);
            if (pkgs == null)
                return new String[0];
            else
                return pkgs;
        }
    }

    private void xmlImport(final InputStream in) throws IOException, SAXException, ParserConfigurationException {
        prefs.unregisterOnSharedPreferenceChangeListener(activitySettings);
        prefs.edit().putBoolean("enabled", false).apply();
        ServiceSinkhole.stop("import", activitySettings, false);

        final XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        final XmlImportHandler handler = new XmlImportHandler(activitySettings);
        reader.setContentHandler(handler);
        reader.parse(new InputSource(in));

        xmlImport(handler.application, prefs);
        xmlImport(handler.wifi, getSharedPreferences("wifi"));
        xmlImport(handler.mobile, getSharedPreferences("other"));
        xmlImport(handler.screen_wifi, getSharedPreferences("screen_wifi"));
        xmlImport(handler.screen_other, getSharedPreferences("screen_other"));
        xmlImport(handler.roaming, getSharedPreferences("roaming"));
        xmlImport(handler.lockdown, getSharedPreferences("lockdown"));
        xmlImport(handler.apply, getSharedPreferences("apply"));
        xmlImport(handler.notify, getSharedPreferences("notify"));

        // Upgrade imported settings
        ReceiverAutostart.upgrade(true, activitySettings);

        DatabaseHelper.clearCache();

        // Refresh UI
        prefs.edit().putBoolean("imported", true).apply();
        prefs.registerOnSharedPreferenceChangeListener(activitySettings);
    }

    private void xmlImport(final Map<String, Object> settings, final SharedPreferences prefs) {
        final SharedPreferences.Editor editor = prefs.edit();

        // Clear existing setting
        for (final String key : prefs.getAll().keySet())
            if (!"enabled".equals(key))
                editor.remove(key);

        // Apply new settings
        for (final String key : settings.keySet()) {
            final Object value = settings.get(key);
            if (value instanceof Boolean)
                editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer)
                editor.putInt(key, (Integer) value);
            else if (value instanceof String)
                editor.putString(key, (String) value);
            else if (value instanceof Set)
                editor.putStringSet(key, (Set<String>) value);
            else
                Log.e(TAG, "Unknown type=" + value.getClass());
        }

        editor.apply();
    }

    private SharedPreferences getSharedPreferences(final String name) {
        return activitySettings.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_backup);
    }

    private static class OpenDocumentBase extends ActivityResultContracts.OpenDocument {
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @NonNull final String[] input) {
            return super.createIntent(context, input) // text/plain for hosts, text/xml for import
                    .addCategory(Intent.CATEGORY_OPENABLE);
        }
    }

    private static class HandleExport extends ActivityResultContracts.CreateDocument {
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @NonNull final String input) {
            return super.createIntent(context, input)
                    .addCategory(Intent.CATEGORY_OPENABLE); // text/xml
        }
    }
}
