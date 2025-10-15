package top.donmor.droidfrpd.ui.daemons;

import static top.donmor.droidfrpd.BuildConfig.ENABLE_CLIENT;
import static top.donmor.droidfrpd.BuildConfig.ENABLE_SERVER;
import static top.donmor.droidfrpd.Utils.badConfig;
import static top.donmor.droidfrpd.Utils.getConf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;

import top.donmor.droidfrpd.BuildConfig;
import top.donmor.droidfrpd.FRPCDaemon;
import top.donmor.droidfrpd.FRPSDaemon;
import top.donmor.droidfrpd.R;
import top.donmor.droidfrpd.databinding.FragmentDaemonsBinding;

public class DaemonsFragment extends Fragment {

	private FragmentDaemonsBinding binding;
	private FRPCDaemon.LocalBinder serviceClient;
	private FRPSDaemon.LocalBinder serviceServer;
	private ToggleButton toggleClient;
	private ToggleButton toggleServer;
	private final ServiceConnection connectionClient = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder svc) {
			serviceClient = (FRPCDaemon.LocalBinder) svc;
			toggleClient.setChecked(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceClient = null;
			toggleClient.setChecked(false);
		}
	};
	private final ServiceConnection connectionServer = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder svc) {
			serviceServer = (FRPSDaemon.LocalBinder) svc;
			toggleServer.setChecked(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceServer = null;
			toggleServer.setChecked(false);
		}
	};

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentDaemonsBinding.inflate(inflater, container, false);
		View root = binding.getRoot();

		final FragmentActivity activity = getActivity();
		assert activity != null;

		toggleClient = binding.toggleDaemonClient;
		toggleServer = binding.toggleDaemonServer;
		for (ToggleButton toggle : new ToggleButton[] {toggleClient, toggleServer}) {
			final boolean isServer = toggle == toggleServer;
			if (!ENABLE_CLIENT && !isServer || !ENABLE_SERVER && isServer) {
				toggle.setVisibility(View.GONE);
				continue;
			}
			final Intent intent = new Intent(activity, isServer ? FRPSDaemon.class : FRPCDaemon.class);
			toggle.setOnClickListener(view -> {
				view.setEnabled(false);
				new Thread(() -> {
					runnable:
					{
						if (toggle.isChecked()) {
							File confFile = getConf(activity, isServer);
							if (badConfig(activity, confFile, isServer)) {
								activity.runOnUiThread(() -> {
									Toast.makeText(activity, isServer ? R.string.ui_server_stopped_err
											: R.string.ui_client_stopped_err, Toast.LENGTH_SHORT).show();
									toggle.setChecked(false);
								});
								break runnable;
							}
							if (activity.startService(intent) == null)
								toggle.setChecked(false);
							if (!activity.bindService(intent, isServer ? connectionServer : connectionClient, Context.BIND_ABOVE_CLIENT))
								toggle.setChecked(false);
						} else activity.stopService(intent);
					}
					activity.runOnUiThread(() -> view.setEnabled(true));
				}).start();
			});
			activity.bindService(intent, isServer ? connectionServer : connectionClient, Context.BIND_ABOVE_CLIENT);
		}
		return root;
	}

	@Override
	public void onResume() {
		if (serviceClient != null && toggleClient != null && serviceClient.isDead())
			toggleClient.setChecked(false);
		if (serviceServer != null && toggleServer != null && serviceServer.isDead())
			toggleServer.setChecked(false);
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		final FragmentActivity activity = getActivity();
		assert activity != null;
		if (ENABLE_CLIENT) activity.unbindService(connectionClient);
		if (ENABLE_SERVER) activity.unbindService(connectionServer);
		binding = null;
	}
}