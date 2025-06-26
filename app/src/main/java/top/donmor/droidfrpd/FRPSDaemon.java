package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.BuildConfig.BINARY_FRP_S;
import static top.donmor.droidfrpd.Utils.LOG_PTN_S;

public class FRPSDaemon extends FRPDaemon {

	@Override
	public void onCreate() {
		setNotificationChannel(2);
		setNotificationTitleId(R.string.ui_server_d);
		setPkAutoRestart(R.string.pk_server_auto_restart);
		setFrpBinary(BINARY_FRP_S);
		setLogFilePtn(LOG_PTN_S);
		setServer(true);
		super.onCreate();
	}
}
