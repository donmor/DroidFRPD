package top.donmor.droidfrpd.ui.server;

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

import top.donmor.droidfrpd.FRPSDaemon;
import top.donmor.droidfrpd.R;
import top.donmor.droidfrpd.databinding.FragmentServerBinding;

public class ServerFragment extends Fragment {

	private FragmentServerBinding binding;
	private FRPSDaemon.LocalBinder service;
	private ToggleButton toggleServer;
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder svc) {
			service = (FRPSDaemon.LocalBinder) svc;
			toggleServer.setChecked(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
			toggleServer.setChecked(false);
		}
	};

	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentServerBinding.inflate(inflater, container, false);
		View root = binding.getRoot();

		final FragmentActivity activity = getActivity();
		assert activity != null;
		Intent intent = new Intent(activity, FRPSDaemon.class);

		toggleServer = binding.toggleServer;
		toggleServer.setOnClickListener(view -> {
			view.setEnabled(false);

			new Thread(() -> {
				runnable:
				{
					if (toggleServer.isChecked()) {
						File confFile = getConf(activity, true);
						if (badConfig(activity, confFile, true)) {
							activity.runOnUiThread(() -> {
								Toast.makeText(activity, R.string.ui_stopped_err, Toast.LENGTH_SHORT).show();
								toggleServer.setChecked(false);
							});
							break runnable;
						}
						if (activity.startService(intent) == null)
							toggleServer.setChecked(false);
						if (!activity.bindService(intent, connection, Context.BIND_ABOVE_CLIENT))
							toggleServer.setChecked(false);
					} else {
						activity.stopService(intent);
					}
				}
				activity.runOnUiThread(() -> view.setEnabled(true));
			}).start();
		});
		activity.bindService(intent, connection, Context.BIND_ABOVE_CLIENT);
		return root;
	}

	@Override
	public void onResume() {
		if (service != null && toggleServer != null && service.isDead())
			toggleServer.setChecked(false);
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		final FragmentActivity activity = getActivity();
		assert activity != null;
		activity.unbindService(connection);
		binding = null;
	}
}