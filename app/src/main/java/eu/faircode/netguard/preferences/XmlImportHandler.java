package eu.faircode.netguard.preferences;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.faircode.netguard.ActivityPro;
import eu.faircode.netguard.DatabaseHelper;
import eu.faircode.netguard.IAB;
import eu.faircode.netguard.Packet;

public class XmlImportHandler extends DefaultHandler {
    private static final String TAG = "NetGuard.Settings";
    private final Context context;
    public boolean enabled = false;
    public Map<String, Object> application = new HashMap<>();
    public Map<String, Object> wifi = new HashMap<>();
    public Map<String, Object> mobile = new HashMap<>();
    public Map<String, Object> screen_wifi = new HashMap<>();
    public Map<String, Object> screen_other = new HashMap<>();
    public Map<String, Object> roaming = new HashMap<>();
    public Map<String, Object> lockdown = new HashMap<>();
    public Map<String, Object> apply = new HashMap<>();
    public Map<String, Object> notify = new HashMap<>();
    private Map<String, Object> current = null;

    public XmlImportHandler(Context context) {
        this.context = context;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals("netguard"))
            ; // Ignore

        else if (qName.equals("application"))
            current = application;

        else if (qName.equals("wifi"))
            current = wifi;

        else if (qName.equals("mobile"))
            current = mobile;

        else if (qName.equals("screen_wifi"))
            current = screen_wifi;

        else if (qName.equals("screen_other"))
            current = screen_other;

        else if (qName.equals("roaming"))
            current = roaming;

        else if (qName.equals("lockdown"))
            current = lockdown;

        else if (qName.equals("apply"))
            current = apply;

        else if (qName.equals("notify"))
            current = notify;

        else if (qName.equals("filter")) {
            current = null;
            Log.i(TAG, "Clearing filters");
            DatabaseHelper.getInstance(context).clearAccess();

        } else if (qName.equals("forward")) {
            current = null;
            Log.i(TAG, "Clearing forwards");
            DatabaseHelper.getInstance(context).deleteForward();

        } else if (qName.equals("setting")) {
            String key = attributes.getValue("key");
            String type = attributes.getValue("type");
            String value = attributes.getValue("value");

            if (current == null)
                Log.e(TAG, "No current key=" + key);
            else {
                if ("enabled".equals(key))
                    enabled = Boolean.parseBoolean(value);
                else {
                    if (current == application) {
                        // Pro features
                        if ("log".equals(key)) {
                            if (!IAB.isPurchased(ActivityPro.SKU_LOG, context))
                                return;
                        } else if ("theme".equals(key)) {
                            if (!IAB.isPurchased(ActivityPro.SKU_THEME, context))
                                return;
                        } else if ("show_stats".equals(key)) {
                            if (!IAB.isPurchased(ActivityPro.SKU_SPEED, context))
                                return;
                        }

                        if ("hosts_last_import".equals(key) || "hosts_last_download".equals(key))
                            return;
                    }

                    if ("boolean".equals(type))
                        current.put(key, Boolean.parseBoolean(value));
                    else if ("integer".equals(type))
                        current.put(key, Integer.parseInt(value));
                    else if ("string".equals(type))
                        current.put(key, value);
                    else if ("set".equals(type)) {
                        Set<String> set = new HashSet<>();
                        if (!TextUtils.isEmpty(value))
                            for (String s : value.split("\n"))
                                set.add(s);
                        current.put(key, set);
                    } else
                        Log.e(TAG, "Unknown type key=" + key);
                }
            }

        } else if (qName.equals("rule")) {
            String pkg = attributes.getValue("pkg");

            String version = attributes.getValue("version");
            String protocol = attributes.getValue("protocol");

            Packet packet = new Packet();
            packet.version = (version == null ? 4 : Integer.parseInt(version));
            packet.protocol = (protocol == null ? 6 /* TCP */ : Integer.parseInt(protocol));
            packet.daddr = attributes.getValue("daddr");
            packet.dport = Integer.parseInt(attributes.getValue("dport"));
            packet.time = Long.parseLong(attributes.getValue("time"));

            int block = Integer.parseInt(attributes.getValue("block"));

            try {
                packet.uid = getUid(pkg);
                DatabaseHelper.getInstance(context).updateAccess(packet, null, block);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.w(TAG, "Package not found pkg=" + pkg);
            }

        } else if (qName.equals("port")) {
            String pkg = attributes.getValue("pkg");
            int protocol = Integer.parseInt(attributes.getValue("protocol"));
            int dport = Integer.parseInt(attributes.getValue("dport"));
            String raddr = attributes.getValue("raddr");
            int rport = Integer.parseInt(attributes.getValue("rport"));

            try {
                int uid = getUid(pkg);
                DatabaseHelper.getInstance(context).addForward(protocol, dport, raddr, rport, uid);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.w(TAG, "Package not found pkg=" + pkg);
            }

        } else
            Log.e(TAG, "Unknown element qname=" + qName);
    }

    private int getUid(String pkg) throws PackageManager.NameNotFoundException {
        if ("root".equals(pkg))
            return 0;
        else if ("android.media".equals(pkg))
            return 1013;
        else if ("android.multicast".equals(pkg))
            return 1020;
        else if ("android.gps".equals(pkg))
            return 1021;
        else if ("android.dns".equals(pkg))
            return 1051;
        else if ("nobody".equals(pkg))
            return 9999;
        else
            return context.getPackageManager().getApplicationInfo(pkg, 0).uid;
    }
}
