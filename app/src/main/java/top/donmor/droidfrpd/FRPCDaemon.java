package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.BuildConfig.BINARY_FRP_C;
import static top.donmor.droidfrpd.Utils.LOG_PTN_C;

public class FRPCDaemon extends FRPDaemon {

	@Override
	public void onCreate() {
		setNotificationChannel(1);
		setNotificationTitleId(R.string.ui_client_d);
		setPkAutoRestart(R.string.pk_client_auto_restart);
		setFrpBinary(BINARY_FRP_C);
		setLogFilePtn(LOG_PTN_C);
		setServer(false);
		super.onCreate();
	}
}
