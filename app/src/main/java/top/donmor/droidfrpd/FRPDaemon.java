package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.Utils.DIR_LOG;
import static top.donmor.droidfrpd.Utils.KEY_EULA;
import static top.donmor.droidfrpd.Utils.KEY_IS_SVR;
import static top.donmor.droidfrpd.Utils.PARAM_CONF;
import static top.donmor.droidfrpd.Utils.getConf;
import static top.donmor.droidfrpd.Utils.getPreferences;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public abstract class FRPDaemon extends Service {
	private File dir;
	private ProcessBuilder processBuilder = null;
	private Process process = null;

	public void setNotificationChannel(int notificationChannel) {
		this.notificationChannel = notificationChannel;
	}

	public void setNotificationTitleId(int notificationTitleId) {
		this.notificationTitleId = notificationTitleId;
	}

	public void setPkAutoRestart(int pkAutoRestart) {
		this.pkAutoRestart = pkAutoRestart;
	}

	public void setFrpBinary(String frpBinary) {
		this.frpBinary = frpBinary;
	}

	public void setLogFilePtn(String logFilePtn) {
		this.logFilePtn = logFilePtn;
	}

	public void setServer(boolean server) {
		isServer = server;
	}

	protected int notificationChannel;
	protected int notificationTitleId;
	protected int pkAutoRestart;
	protected String frpBinary;
	protected String logFilePtn;
	protected boolean isServer;

	@Override
	public void onCreate() {
		super.onCreate();
		Context ac = getApplicationContext();
		dir = ac.getFilesDir();
		String cid = getPackageName();
		NotificationChannel channel = new NotificationChannel(cid, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
		channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
				.createNotificationChannel(channel);
		startForeground(notificationChannel, new NotificationCompat.Builder(this, cid)
				.setOngoing(true)
				.setContentTitle(getString(notificationTitleId))
				.setContentText(getString(R.string.ui_running))
				.setSmallIcon(R.drawable.ic_notification)
				.setContentIntent(PendingIntent.getActivity(ac, notificationChannel - 1, new Intent(ac, MainActivity.class)
								.setAction(Intent.ACTION_MAIN)
								.addCategory(Intent.CATEGORY_LAUNCHER)
								.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
								.putExtra(KEY_IS_SVR, isServer),
						PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
				.build());
		processBuilder = new ProcessBuilder(
				ac.getApplicationInfo().nativeLibraryDir + '/' + frpBinary,
				PARAM_CONF,
				getConf(ac, isServer).getAbsolutePath())
				.directory(dir)
				.redirectErrorStream(true);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (processBuilder == null) throw new RuntimeException();
		if (!Utils.getPreferences(getApplicationContext()).getBoolean(KEY_EULA, false)) {
			stop(true);
			return super.onStartCommand(intent, flags, startId);
		}
		if (process != null && process.isAlive())
			return super.onStartCommand(intent, flags, startId);
		try {
			long t = System.currentTimeMillis();
			File logDir = new File(dir, DIR_LOG);
			if (!logDir.exists()) //noinspection ResultOfMethodCallIgnored
				logDir.mkdir();
			if (logDir.isDirectory()) {
				File log = new File(logDir, String.format(Locale.getDefault(), logFilePtn, t, t, t, t, t, t, t));
				processBuilder.redirectOutput(log);
			} else processBuilder.inheritIO();
			process = processBuilder.start();
			new Thread(() -> {
				Process mProcess = process;
				try {
					while (true) {
						mProcess.waitFor();
						if (mProcess.exitValue() == 143) {
							stop(true);
							break;
						} else if (getPreferences(getApplicationContext()).getBoolean(getString(pkAutoRestart), false)) {
							new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
									getApplicationContext(), R.string.ui_stopped_restart, Toast.LENGTH_SHORT).show());
							//noinspection BusyWait
							Thread.sleep(3000);
							if (process == null) {
								stop(true);
								break;
							}
							process = processBuilder.start();
							mProcess = process;
						} else {
							new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
									getApplicationContext(), R.string.ui_stopped_err, Toast.LENGTH_SHORT).show());
							stop(true);
							break;
						}
					}
				} catch (InterruptedException ignored) {
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
			stop(true);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		stop(false);
		super.onDestroy();
	}

	@Override
	public void onTimeout(int startId) {
		stop(true);
		super.onTimeout(startId);
	}

	private void stop(boolean intended) {
		if (process != null) {
			process.destroy();
			process = null;
		}
		if (intended) stopSelf();
	}

	public class LocalBinder extends Binder {
		public boolean isDead() {
			return process == null || !process.isAlive();
		}
	}
}
