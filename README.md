# Android-BLEPeripheral

android 从4.3系统开始可以连接BLE设备,iOS是从7.0版本开始支持BLE

android 从5.0系统开始可以模拟设备发出BLE广播,这个新功能是对标于iOS系统的手机模拟iBeacon设备

BLE设备之所以能被手机扫描到,是因为BLE设备在每隔一段时间广播一次,这个广播里面包含很多数据

### 1.检查是否支持BLE、BLE Peripheral
```
    /**
     * 首先检查是否支持BLE、BLE Peripheral
     */
    private void init() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
//            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //判断是否打开蓝牙,如果没有打开需要回调打开蓝牙,这里省略,请自行打开蓝牙
        }

        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "This device not support peripheral", Toast.LENGTH_SHORT).show();
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "This device not support peripheral", Toast.LENGTH_SHORT).show();
        }
    }
```
### 2.准备发送广播
发送广播
```
mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(true, 0), createAdvertiseData(), mAdvertiseCallback);
```
广播设置参数AdvertiseSettings
```
/** create AdvertiseSettings */
		 public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
			 AdvertiseSettings.Builder mSettingsbuilder = new AdvertiseSettings.Builder();
			 mSettingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
			 mSettingsbuilder.setConnectable(connectable);
			 mSettingsbuilder.setTimeout(timeoutMillis);
			 mSettingsbuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
			 AdvertiseSettings mAdvertiseSettings = mSettingsbuilder.build();
				if(mAdvertiseSettings == null){
					if(D){
						Toast.makeText(mContext, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
						Log.e(TAG,"mAdvertiseSettings == null");
					}
				}
			return mAdvertiseSettings;
		 }
```
广播数据AdvertiseData
```
public static AdvertiseData createAdvertiseData(){		 
		 	AdvertiseData.Builder    mDataBuilder = new AdvertiseData.Builder();
			mDataBuilder.addServiceUuid(ParcelUuid.fromString(HEART_RATE_SERVICE));
			AdvertiseData mAdvertiseData = mDataBuilder.build();
			if(mAdvertiseData==null){
				if(D){
					Toast.makeText(mContext, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
					Log.e(TAG,"mAdvertiseSettings == null");
				}
			}	
			return mAdvertiseData;
	 }
```
广播回调Callback
```
private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
		@Override
		  public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			super.onStartSuccess(settingsInEffect);
			 if (settingsInEffect != null) {
				 Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel()	 + " mode=" + settingsInEffect.getMode()
				 + " timeout=" + settingsInEffect.getTimeout());
				 } else {
				 Log.e(TAG, "onStartSuccess, settingInEffect is null");
				 }
				showText("1. initGattServer Success");
			 	Log.e(TAG,"onStartSuccess settingsInEffect" + settingsInEffect);
		    }
		
		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			showText("1. initGattServer failed");
	    }
	};
```
### 广播关闭
```
private void stopAdvertise() {
			 if (mBluetoothLeAdvertiser != null) {
				 mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
				 mBluetoothLeAdvertiser = null;
			 }
		 }
```
