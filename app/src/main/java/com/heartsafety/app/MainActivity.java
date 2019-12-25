package com.heartsafety.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.heartsafety.app.databinding.ActivityMainBinding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;

    //블루투스 요청 액티비티 코드
    private static final int REQUEST_ACCESS_BLUETOOTH = 100;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 101;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 102;

    //BluetoothListAdapter
    private BluetoothAdapter mBluetoothAdapter;

    //Adapter
    private BluetoothListAdapter adapterPaired;
    private BluetoothListAdapter adapterDevice;

    //list - Device 목록 저장
    private ArrayList<BluetoothListItem> dataPaired;
    private ArrayList<BluetoothListItem> dataDevice;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private int selectDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        registerBluetoothStateReceiver();
        registerBluetoothSearchReceiver();
        registerBluetoothScanModeReceiver();

        initBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.setOnClickHandler(new OnClickHandler());
        discoverDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCESS_BLUETOOTH) {//블루투스 활성화 승인
            if (resultCode == Activity.RESULT_OK) {
                getListPairedDevice();
            } else {//블루투스 활성화 거절
                Snackbar.make(mBinding.getRoot(), "블루투스를 활성화해야 합니다.", Snackbar.LENGTH_LONG).show();
                finish();
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBluetoothStateReceiver();
        unregisterBluetoothSearchReceiver();
        unregisterBluetoothScanModeReceiver();
    }

    //블루투스 상태변화 BroadcastReceiver
    private BroadcastReceiver mBluetoothStateReceiver;

    //블루투스 검색결과 BroadcastReceiver
    private BroadcastReceiver mBluetoothSearchReceiver;

    //블루투스 검색응답 모드 BroadcastReceiver
    private BroadcastReceiver mBluetoothScanModeReceiver;

    private void registerBluetoothStateReceiver() {
        mBluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //BluetoothListAdapter.EXTRA_STATE : 블루투스의 현재상태 변화
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                //블루투스 활성화
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        mBinding.txtState.setText("블루투스 활성화");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        mBinding.txtState.setText("블루투스 활성화 중...");
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        mBinding.txtState.setText("블루투스 비활성화");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mBinding.txtState.setText("블루투스 비활성화 중...");
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //BluetoothListAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션
        registerReceiver(mBluetoothStateReceiver, filter);
    }

    private void registerBluetoothSearchReceiver() {
        mBluetoothSearchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    //블루투스 디바이스 검색 시작
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        Log.i("ACTION_DISCOVERY_STARTED");
                        dataDevice.clear();
                        bluetoothDevices.clear();
                        Snackbar.make(mBinding.getRoot(), "블루투스 검색 시작", Snackbar.LENGTH_LONG).show();
                        mBinding.btnSearch.setEnabled(false);
                        mBinding.progressBar.setVisibility(View.VISIBLE);
                        break;

                    //블루투스 디바이스 찾음
                    case BluetoothDevice.ACTION_FOUND:
                        Log.i("ACTION_FOUND");
                        //검색한 블루투스 디바이스의 객체를 구한다
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        //데이터 저장
                        if (device != null) {
                            dataDevice.add(new BluetoothListItem(device.getName(), device.getAddress()));
                            //리스트 목록갱신
                            adapterDevice.notifyDataSetChanged();
                            //블루투스 디바이스 저장
                            bluetoothDevices.add(device);
                        }
                        break;

                    //블루투스 디바이스 검색 종료
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i("ACTION_DISCOVERY_FINISHED");
                        Snackbar.make(mBinding.getRoot(), "블루투스 검색 종료", Snackbar.LENGTH_LONG).show();
                        mBinding.btnSearch.setEnabled(true);
                        mBinding.progressBar.setVisibility(View.GONE);
                        break;

                    //블루투스 디바이스 페어링 상태 변화
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        Log.i("ACTION_BOND_STATE_CHANGED");
                        BluetoothDevice paired = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (paired != null &&
                                paired.getBondState() == BluetoothDevice.BOND_BONDED) {
                            //데이터 저장
                            dataPaired.add(new BluetoothListItem(paired.getName(), paired.getAddress()));
                            //리스트 목록갱신
                            adapterPaired.notifyDataSetChanged();

                            //검색된 목록
                            if (selectDevice != -1) {
                                bluetoothDevices.remove(selectDevice);

                                dataDevice.remove(selectDevice);
                                adapterDevice.notifyDataSetChanged();
                                selectDevice = -1;
                            }
                        }
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //BluetoothListAdapter.ACTION_DISCOVERY_STARTED : 블루투스 검색 시작
        filter.addAction(BluetoothDevice.ACTION_FOUND); //BluetoothDevice.ACTION_FOUND : 블루투스 디바이스 찾음
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //BluetoothListAdapter.ACTION_DISCOVERY_FINISHED : 블루투스 검색 종료
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBluetoothSearchReceiver, filter);
    }

    private void registerBluetoothScanModeReceiver() {
        mBluetoothScanModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                switch (state) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        mBinding.chkFindMe.setChecked(false);
                        mBinding.chkFindMe.setEnabled(true);
                        Snackbar.make(mBinding.getRoot(), "검색응답 모드 종료", Snackbar.LENGTH_LONG).show();
                        break;

                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Snackbar.make(mBinding.getRoot(), "다른 블루투스 기기에서 내 휴대폰을 찾을 수 있습니다.", Snackbar.LENGTH_LONG).show();
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBluetoothScanModeReceiver, filter);
    }

    private void unregisterBluetoothStateReceiver() {
        unregisterReceiver(mBluetoothStateReceiver);
    }

    private void unregisterBluetoothSearchReceiver() {
        unregisterReceiver(mBluetoothSearchReceiver);
    }

    private void unregisterBluetoothScanModeReceiver() {
        unregisterReceiver(mBluetoothScanModeReceiver);
    }

    private void initBluetooth() {
        //Adapter1
        dataPaired = new ArrayList<>();
        adapterPaired = new BluetoothListAdapter(this, dataPaired);
        mBinding.listPaired.setAdapter(adapterPaired);
        //Adapter2
        dataDevice = new ArrayList<>();
        adapterDevice = new BluetoothListAdapter(this, dataDevice);
        mBinding.listDevice.setAdapter(adapterDevice);

        //검색된 블루투스 디바이스 데이터
        bluetoothDevices = new ArrayList<>();
        //선택한 디바이스 없음
        selectDevice = -1;

        //블루투스를 지원하지 않으면 null을 리턴한다
        if (mBluetoothAdapter == null) {
            Snackbar.make(mBinding.getRoot(), "블루투스를 지원하지 않는 단말기 입니다.", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        //1. 블루투스가 꺼져있으면 활성화
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable(); //강제 활성화
        }

        //2. 블루투스가 꺼져있으면 사용자에게 활성화 요청하기
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ACCESS_BLUETOOTH);
        } else {
            getListPairedDevice();
        }

        //검색된 디바이스목록 클릭시 페어링 요청
        mBinding.listDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = bluetoothDevices.get(position);

                try {
                    //선택한 디바이스 페어링 요청
                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(device, (Object[]) null);
                    selectDevice = position;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public class OnClickHandler {
        //블루투스 검색 버튼 클릭
        public void onBluetoothSearch(View v) {
            startDiscover();
        }

        //검색응답 모드 - 블루투스가 외부 블루투스의 요청에 답변하는 슬레이브 상태
        //BluetoothListAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE : 검색응답 모드 활성화 + 페이지 모드 활성화
        //BluetoothListAdapter.SCAN_MODE_CONNECTABLE : 검색응답 모드 비활성화 + 페이지 모드 활성화
        //BluetoothListAdapter.SCAN_MODE_NONE : 검색응답 모드 비활성화 + 페이지 모드 비활성화
        //검색응답 체크박스 클릭
        public void onChkFindMe(View v) {
            //검색응답 체크
            if (mBinding.chkFindMe.isChecked()) {
                if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) { //검색응답 모드가 활성화이면 하지 않음
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);  //60초 동안 상대방이 나를 검색할 수 있도록한다
                    startActivity(intent);
                }
            }
        }
    }

    //이미 페어링된 목록 가져오기
    public void getListPairedDevice() {
        Set<BluetoothDevice> pairedDevice = mBluetoothAdapter.getBondedDevices();

        dataPaired.clear();
        if (pairedDevice.size() > 0) {
            for (BluetoothDevice device : pairedDevice) {
                //데이터 저장
                dataPaired.add(new BluetoothListItem(device.getName(), device.getAddress()));
            }
        }
        //리스트 목록갱신
        adapterPaired.notifyDataSetChanged();
    }

    private void discoverDevices() {
        int result;
        int request;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            request = REQUEST_ACCESS_COARSE_LOCATION;

        } else {
            result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            request = REQUEST_ACCESS_FINE_LOCATION;
        }

        switch (result) {
            case PackageManager.PERMISSION_DENIED:
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH},
                        request);
                break;

            case PackageManager.PERMISSION_GRANTED:
                Log.d("PERMISSION_GRANTED");
                startDiscover();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION:
            case REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Granted");
                    startDiscover();
                } else {
                    Log.w("Denied");
                }
            }
            break;
        }
    }

    private void startDiscover() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();
            boolean res = mBluetoothAdapter.startDiscovery();
            Log.d("res: " + res);
        } else {
            Log.w("adapter is null");
        }
    }
}
