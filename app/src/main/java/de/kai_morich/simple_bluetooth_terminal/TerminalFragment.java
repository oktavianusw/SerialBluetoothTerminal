package de.kai_morich.simple_bluetooth_terminal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayDeque;
import java.util.Calendar;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }


    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);

        receiveText = view.findViewById(R.id.receive_text);
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText));
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        EditText year = view.findViewById(R.id.edit_year);
        EditText month = view.findViewById(R.id.edit_month);
        EditText day = view.findViewById(R.id.edit_day);
        EditText hour = view.findViewById(R.id.edit_hour);
        EditText minute = view.findViewById(R.id.edit_minute);
        EditText second = view.findViewById(R.id.edit_second);

        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // Months are zero-based
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);

        // Set the default values in the EditText fields
        year.setText(String.valueOf(currentYear));
        month.setText(String.valueOf(currentMonth));
        day.setText(String.valueOf(currentDay));
        hour.setText(String.valueOf(currentHour));
        minute.setText(String.valueOf(currentMinute));
        second.setText(String.valueOf(currentSecond));

//        Setting RTC
        View sendBtnRtc = view.findViewById(R.id.button_send_rtc);
        sendBtnRtc.setOnClickListener(v -> {
            String yearValue = year.getText().toString();
            String monthValue = month.getText().toString();
            String dayValue = day.getText().toString();
            String hourValue = hour.getText().toString();
            String minuteValue = minute.getText().toString();
            String secondValue = second.getText().toString();

            String json = "{\"k\" : \"6\",\"y\" : \"" + yearValue + "\",\"m\" : \"" + monthValue + "\",\"d\" : \"" + dayValue + "\",\"h\" : \"" + hourValue + "\",\"mi\" : \"" + minuteValue + "\",\"s\" : \"" + secondValue + "\"}";

            send(json);
        });

//        Setting Auth Lora
        EditText get_a = view.findViewById(R.id.edit_a);
        EditText get_n = view.findViewById(R.id.edit_n);
        EditText get_p = view.findViewById(R.id.edit_p);

        View sendBtnAuthLora = view.findViewById(R.id.button_send_auth_lora);
        sendBtnAuthLora.setOnClickListener(v -> {
            String value_a = get_a.getText().toString();
            String value_n = get_n.getText().toString();
            String value_p = get_p.getText().toString();

            String json = "{\"k\" : \"3\",\"a\" : \""+ value_a +"\",\"n\" : \""+ value_n +"\",\"p\" : \""+ value_p +"\"}";

            send(json);
        });

//        Setting SD Card
        EditText get_s = view.findViewById(R.id.edit_s);

        View sendBtnSdCard = view.findViewById(R.id.button_send_sd_card);
        sendBtnSdCard.setOnClickListener(v -> {
            String value_s = get_s.getText().toString();

            String json = "{\"k\" : \"5\",\"s\" : \""+ value_s +"\"}";
            send(json);
        });

//        Setting Serial Number
        EditText get_t = view.findViewById(R.id.edit_s);
        EditText get_sn = view.findViewById(R.id.edit_s);

        View sendBtnSerialNumber = view.findViewById(R.id.button_send_serial_number);
        sendBtnSerialNumber.setOnClickListener(v -> {
            String value_t = get_t.getText().toString();
            String value_sn = get_sn.getText().toString();

            String json = "{\"k\" : \"4\",\"t\" : \""+ value_t +"\",\"sn\" : \""+ value_sn +"\"}";
            send(json);
        });

//        Setting Interval Sending
        EditText get_d = view.findViewById(R.id.edit_d);
        EditText get_t_is = view.findViewById(R.id.edit_t_is);
        EditText get_l = view.findViewById(R.id.edit_l);

        View sendBtnIntervalSending = view.findViewById(R.id.button_send_interval_sending);
        sendBtnIntervalSending.setOnClickListener(v -> {
            String value_d = get_d.getText().toString();
            String value_t_is = get_t_is.getText().toString();
            String value_l = get_l.getText().toString();

            String json = "{\"k\" : \"9\",\"d\" : \""+ value_d +"\",\"t\" : \""+ value_t_is +"\",\"l\" : \""+ value_l +"\"}";
            send(json);
        });

//        Setting Connection
//        EditText set_v = view.findViewById(R.id.edit_v);
//        View sendBtnConnection = view.findViewById(R.id.button_send_connection);
//
//        sendBtnConnection.setOnClickListener(v -> {
//            boolean v_value_set = false;
//            String v_value = set_v.getText().toString();
//
//            if (v_value.isEmpty() && !v_value_set) {
//                set_v.setText("1");
//                v_value_set = true;
//                v_value = "1";
//            }
//
//            String json = "{\"k\" : \"2\",\"v\" : \"" + v_value + "\"}";
//
//            send(json);
//        });
        


        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(ArrayDeque<byte[]> datas) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for (byte[] data : datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n');
            } else {
                String msg = new String(data);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        if(spn.length() >= 2) {
                            spn.delete(spn.length() - 2, spn.length());
                        } else {
                            Editable edt = receiveText.getEditableText();
                            if (edt != null && edt.length() >= 2)
                                edt.delete(edt.length() - 2, edt.length());
                        }
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
        receiveText.append(spn);
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        receive(datas);
    }

    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
