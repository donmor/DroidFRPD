package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.Utils.badConfig;
import static top.donmor.droidfrpd.Utils.getConf;
import static top.donmor.droidfrpd.Utils.getPreferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompleteReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())) return;
		SharedPreferences preferences = getPreferences(context);
		if (preferences.getBoolean(context.getString(R.string.pk_client_start_on_boot), false) && !badConfig(context, getConf(context, false), false))
			context.startService(new Intent(context, FRPCDaemon.class));
		if (preferences.getBoolean(context.getString(R.string.pk_server_start_on_boot), false) && !badConfig(context, getConf(context, true), true))
			context.startService(new Intent(context, FRPSDaemon.class));
	}
}
