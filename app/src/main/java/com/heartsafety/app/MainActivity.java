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
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.heartsafety.app.databinding.ActivityMainBinding;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;

    //블루투스 요청 액티비티 코드
    private static final int REQUEST_ACCESS_BLUETOOTH = 100;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 101;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 102;

    //BluetoothAdapter
    private BluetoothAdapter mBluetoothAdapter;

    //Adapter
    private SimpleAdapter adapterPaired;
    private SimpleAdapter adapterDevice;

    //list - Device 목록 저장
    private List<Map<String, String>> dataPaired;
    private List<Map<String, String>> dataDevice;
    private List<BluetoothDevice> bluetoothDevices;
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
        switch (requestCode) {
            case REQUEST_ACCESS_BLUETOOTH:
                //블루투스 활성화 승인
                if (resultCode == Activity.RESULT_OK) {
                    GetListPairedDevice();
                }
                //블루투스 활성화 거절
                else {
                    Toast.makeText(this, "블루투스를 활성화해야 합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                break;
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
                //BluetoothAdapter.EXTRA_STATE : 블루투스의 현재상태 변화
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                //블루투스 활성화
                if (state == BluetoothAdapter.STATE_ON) {
                    mBinding.txtState.setText("블루투스 활성화");
                }
                //블루투스 활성화 중
                else if (state == BluetoothAdapter.STATE_TURNING_ON) {
                    mBinding.txtState.setText("블루투스 활성화 중...");
                }
                //블루투스 비활성화
                else if (state == BluetoothAdapter.STATE_OFF) {
                    mBinding.txtState.setText("블루투스 비활성화");
                }
                //블루투스 비활성화 중
                else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    mBinding.txtState.setText("블루투스 비활성화 중...");
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //BluetoothAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션
        registerReceiver(mBluetoothStateReceiver, filter);
    }

    private void registerBluetoothSearchReceiver() {
        mBluetoothSearchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    //블루투스 디바이스 검색 시작
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        Log.i("ACTION_DISCOVERY_STARTED");
                        dataDevice.clear();
                        bluetoothDevices.clear();
                        Toast.makeText(MainActivity.this, "블루투스 검색 시작", Toast.LENGTH_SHORT).show();
                        mBinding.btnSearch.setEnabled(false);
                        mBinding.progressBar.setVisibility(View.VISIBLE);
                        break;
                    //블루투스 디바이스 찾음
                    case BluetoothDevice.ACTION_FOUND:
                        Log.i("ACTION_FOUND");
                        //검색한 블루투스 디바이스의 객체를 구한다
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        //데이터 저장
                        Map map = new HashMap();
                        map.put("name", device.getName()); //device.getName() : 블루투스 디바이스의 이름
                        map.put("address", device.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                        dataDevice.add(map);
                        //리스트 목록갱신
                        adapterDevice.notifyDataSetChanged();

                        //블루투스 디바이스 저장
                        bluetoothDevices.add(device);
                        break;
                    //블루투스 디바이스 검색 종료
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.i("ACTION_DISCOVERY_FINISHED");
                        Toast.makeText(MainActivity.this, "블루투스 검색 종료", Toast.LENGTH_SHORT).show();
                        mBinding.btnSearch.setEnabled(true);
                        mBinding.progressBar.setVisibility(View.GONE);
                        break;
                    //블루투스 디바이스 페어링 상태 변화
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        Log.i("ACTION_BOND_STATE_CHANGED");
                        BluetoothDevice paired = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (paired.getBondState() == BluetoothDevice.BOND_BONDED) {
                            //데이터 저장
                            Map map2 = new HashMap();
                            map2.put("name", paired.getName()); //device.getName() : 블루투스 디바이스의 이름
                            map2.put("address", paired.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                            dataPaired.add(map2);
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
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //BluetoothAdapter.ACTION_DISCOVERY_STARTED : 블루투스 검색 시작
        filter.addAction(BluetoothDevice.ACTION_FOUND); //BluetoothDevice.ACTION_FOUND : 블루투스 디바이스 찾음
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //BluetoothAdapter.ACTION_DISCOVERY_FINISHED : 블루투스 검색 종료
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
                        Toast.makeText(MainActivity.this, "검색응답 모드 종료", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Toast.makeText(MainActivity.this, "다른 블루투스 기기에서 내 휴대폰을 찾을 수 있습니다.", Toast.LENGTH_SHORT).show();
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
        adapterPaired = new SimpleAdapter(this, dataPaired, android.R.layout.simple_list_item_2, new String[]{"name", "address"}, new int[]{android.R.id.text1, android.R.id.text2});
        mBinding.listPaired.setAdapter(adapterPaired);
        //Adapter2
        dataDevice = new ArrayList<>();
        adapterDevice = new SimpleAdapter(this, dataDevice, android.R.layout.simple_list_item_2, new String[]{"name", "address"}, new int[]{android.R.id.text1, android.R.id.text2});
        mBinding.listDevice.setAdapter(adapterDevice);

        //검색된 블루투스 디바이스 데이터
        bluetoothDevices = new ArrayList<>();
        //선택한 디바이스 없음
        selectDevice = -1;

        //블루투스를 지원하지 않으면 null을 리턴한다
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 단말기 입니다.", Toast.LENGTH_SHORT).show();
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
            GetListPairedDevice();
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
        public void OnBluetoothSearch(View v) {
            //검색버튼 비활성화
            discoverDevices();
            final Snackbar snackbar = Snackbar.make(v, "확인 누르면 사라집니다.", Snackbar.LENGTH_LONG);
            snackbar.setAction("확인", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        }

        //검색응답 모드 - 블루투스가 외부 블루투스의 요청에 답변하는 슬레이브 상태
        //BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE : 검색응답 모드 활성화 + 페이지 모드 활성화
        //BluetoothAdapter.SCAN_MODE_CONNECTABLE : 검색응답 모드 비활성화 + 페이지 모드 활성화
        //BluetoothAdapter.SCAN_MODE_NONE : 검색응답 모드 비활성화 + 페이지 모드 비활성화
        //검색응답 체크박스 클릭
        public void OnChkFindMe(View v) {
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
    public void GetListPairedDevice() {
        Set<BluetoothDevice> pairedDevice = mBluetoothAdapter.getBondedDevices();

        dataPaired.clear();
        if (pairedDevice.size() > 0) {
            for (BluetoothDevice device : pairedDevice) {
                //데이터 저장
                Map map = new HashMap();
                map.put("name", device.getName()); //device.getName() : 블루투스 디바이스의 이름
                map.put("address", device.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                dataPaired.add(map);
            }
        }
        //리스트 목록갱신
        adapterPaired.notifyDataSetChanged();
    }

    private void discoverDevices() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BLUETOOTH_ADMIN,
                                    Manifest.permission.BLUETOOTH},
                            REQUEST_ACCESS_COARSE_LOCATION);

                    break;
                case PackageManager.PERMISSION_GRANTED:
                    Log.d("PERMISSION_GRANTED");
                    startDiscover();
                    break;
            }
        } else {
            switch (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BLUETOOTH_ADMIN,
                                    Manifest.permission.BLUETOOTH},
                            REQUEST_ACCESS_FINE_LOCATION);

                    break;
                case PackageManager.PERMISSION_GRANTED:
                    Log.d("PERMISSION_GRANTED");
                    startDiscover();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
