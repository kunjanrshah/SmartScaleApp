package com.krs.smart

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.*

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.krs.smart.adapters.WeighingScaleListAdapter
import com.krs.smart.bleUtils.BluetoothLEService
import com.krs.smart.bleUtils.BluetoothUtils
import com.krs.smart.callbacks.ControlButtonsClickListener
import com.krs.smart.room.model.ScaleType
import com.krs.smart.utils.applyTint
import com.krs.smart.viewmodel.WeighingScaleViewModel
import com.krs.smart.viewmodel.WeighingScaleViewModelFactory
import com.krs.smart.wifiUtils.WifiClientThread
import com.krs.smart.wifiUtils.WifiUtils.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.krs.smart.databinding.ActivityMainBinding
import com.krs.smart.mqtt.Message
import com.krs.smart.mqtt.MessageService
import javax.inject.Inject


class MainActivity : AppCompatActivity(), ControlButtonsClickListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    @Inject
    lateinit var factory: WeighingScaleViewModelFactory
    private var recyclerView: RecyclerView? = null
    private var llParent:LinearLayout?=null
    private lateinit var scaleViewModel: WeighingScaleViewModel
    private val REQUEST_LOCATION = 199
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var result: PendingResult<LocationSettingsResult>? = null
    private var connected: Boolean? = null
    private var service:Intent?=null
    companion object {
        lateinit var weighingScaleListAdapter: WeighingScaleListAdapter
        var bluetoothDevice: BluetoothDevice?=null
        var clientThread: WifiClientThread?=null
    }
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLEService: BluetoothLEService? = null
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val TAG = MainActivity::class.java.simpleName

    private var alertReceiver: BroadcastReceiver? = null
    private var messageReceiver: BroadcastReceiver? = null
    private var connectionReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityMainBinding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        injectActivity()
        setUpToolBar()
        setUpRecyclerView(activityMainBinding)
        setUpViewModel(activityMainBinding)
        llParent=activityMainBinding.llParent;

        alertReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                displayAlert(intent.getStringExtra(MessageService.EVENT_ALERT_MESSAGE))
            }
        }

        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                changeConnectionStatus(intent.extras!!.getBoolean(MessageService.EVENT_BROKER_CONNECTION_STATUS))
            }
        }

        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                displayMessage(intent.getParcelableExtra<Parcelable>(MessageService.EVENT_MESSAGE_CONTENT) as Message?)
            }
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            Log.e(TAG, "App crashed!! Exception:", ex)
        }

        Handler().postDelayed({
            if(weighingScaleListAdapter.mqttScaleWeight!=null){
                service = Intent(applicationContext, MessageService::class.java)
                applicationContext.startService(service)
            }
        },2000)
    }


    private fun displayAlert(alert: String?) {
        Log.i(TAG, "Alert received: $alert")
        Snackbar.make(findViewById(android.R.id.content), alert.toString(), Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }

    private fun displayMessage(message1: Message?) {
        Log.i(TAG, "MQTT message incoming: " + message1?.message)
        weighingScaleListAdapter.mqttScaleWeight?.text=message1?.message

    }

    private fun changeConnectionStatus(status: Boolean) {
        Log.i(TAG, "Connection status: $status")
        connected = status
    }


    private fun gattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BluetoothLEService.ACTION_GATT_CONNECTED == action -> {

                    weighingScaleListAdapter.getItemAtPosition(weighingScaleListAdapter.blePos).name = bluetoothDevice!!.address
                    weighingScaleListAdapter.notifyDataSetChanged()

                }
                BluetoothLEService.ACTION_GATT_DISCONNECTED == action -> {
                    //TODO
                }
                BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
                    displayGattServices(mBluetoothLEService!!.supportedGattServices)
                }
                BluetoothLEService.ACTION_DATA_AVAILABLE == action -> {
                    val dataInput = mNotifyCharacteristic!!.value
                    displayData(dataInput)
                }
            }
        }
    }

    private fun displayData(data: ByteArray?) {
        try {
            if (data != null) {
                var output = ""
                for (i in data.indices) {
                    output += data[i].toInt().toChar()
                }
                Log.d(TAG, "output: " + output)
                weighingScaleListAdapter.bleScaleWeight?.text = output.trim()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        var serviceString: String? = "unknown service"
        var charaString: String? = "unknown characteristic"
        for (gattService in gattServices) {
            uuid = gattService.uuid.toString()
            serviceString = BluetoothUtils.lookup(uuid)
            if (serviceString != null) {
                val gattCharacteristics = gattService.characteristics
                for (gattCharacteristic in gattCharacteristics) {
                    val currentCharaData = HashMap<String, String>()
                    uuid = gattCharacteristic.uuid.toString()
                    charaString = BluetoothUtils.lookup(uuid)
                    if (charaString != null) {
                        //  serviceName.setText(charaString);
                    }
                    mNotifyCharacteristic = gattCharacteristic
                    if (mNotifyCharacteristic != null) {
                        val charaProp = mNotifyCharacteristic?.properties
                        if (charaProp != null) {
                            if ((charaProp or BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                             //   mBluetoothLEService!!.readCharacteristic(mNotifyCharacteristic!!)
                            }

                            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                                mBluetoothLEService!!.setCharacteristicNotification(
                                    mNotifyCharacteristic!!,
                                    true
                                )
                            }
                        }

                    }
                    return
                }
            }
        }
    }

    private fun getSSID():String{
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return info.ssid.replace("\"", "")
    }

    private val wifiStateReceiver:BroadcastReceiver=object :BroadcastReceiver(){
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context?, intent: Intent) {

            var flag =false
            scaleViewModel.getAllScales.observe(this@MainActivity){ scalesList ->

                scalesList.forEach {
                    if(it.type == ScaleType.WIFI){
                        flag=true
                    }
                }
            }

            if(flag){
                if (getSSID().equals(SSID)){
                    val snbar:Snackbar=Snackbar.make(llParent!!, WIFI_MSG_SCALE_CONNECTED, Snackbar.LENGTH_LONG);
                    snbar.show()
                }else{
                    val snbar:Snackbar=Snackbar.make(llParent!!, WIFI_MSG_CONNECT_SCALE, Snackbar.LENGTH_INDEFINITE);
                    snbar.show()
                }
            }
        }
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    alert()
                }
            }
            if (LocationManager.PROVIDERS_CHANGED_ACTION == action) { mGoogleApiClient = GoogleApiClient.Builder(this@MainActivity).addApi(LocationServices.API).addConnectionCallbacks(this@MainActivity).addOnConnectionFailedListener(this@MainActivity).build()
                mGoogleApiClient.connect()
            }
        }
    }

    private fun alert() {
        AlertDialog.Builder(this) //set icon
            .setIcon(android.R.drawable.ic_dialog_alert) //set title
            .setTitle(resources.getString(R.string.app_name)) //set message
            .setMessage("Please turn on Bluetooth") //set positive button
            .setPositiveButton("Ok") { dialogInterface, i ->
                setBluetooth(true)

                setupBleIfInList()
            }.show()
    }

    private fun setBluetooth(enable: Boolean) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val isEnabled = bluetoothAdapter.isEnabled
        if (enable && !isEnabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            bluetoothAdapter.enable()
        } else if (!enable && isEnabled) {
            bluetoothAdapter.disable()
        }
        // No need to change bluetooth state
    }

    override fun onResume() {
        super.onResume()
        try {
            registerReceiver(wifiStateReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
            registerReceiver(mGattUpdateReceiver, gattUpdateIntentFilter())
            registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            registerReceiver(mReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

            val localBroadcastManager = LocalBroadcastManager.getInstance(this)
            localBroadcastManager.registerReceiver(alertReceiver!!, IntentFilter(MessageService.EVENT_ALERT))
            localBroadcastManager.registerReceiver(connectionReceiver!!, IntentFilter(
                MessageService.EVENT_BROKER_CONNECTION))
            localBroadcastManager.registerReceiver(messageReceiver!!, IntentFilter(
                MessageService.EVENT_MESSAGE))

        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }

        mGoogleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()
        mGoogleApiClient.connect()

        setupBleIfInList()
    }

    private fun setupBleIfInList() {

        if (weighingScaleListAdapter.blePos != -1) {
            setUpBleAdapter()
            turnOnBleGps()
        }
    }

    private fun turnOnBleGps() {

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show()
            finish()
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(this@MainActivity, "Bluetooth not supported!", Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), BluetoothUtils.REQUEST_LOCATION_ENABLE_CODE)
        }

        if (mBluetoothAdapter?.enable() == false) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, BluetoothUtils.REQUEST_BLUETOOTH_ENABLE_CODE)
        }

        if (mBluetoothLEService != null) {
            val result = mBluetoothLEService?.connect(bluetoothDevice!!.address)
            Log.d(TAG, "Connect request result=$result")
        }

        registerReceiver(mGattUpdateReceiver, gattUpdateIntentFilter())
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(mReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        registerReceiver(wifiStateReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

        mGoogleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()
        mGoogleApiClient.connect()
    }

    private fun setUpBleAdapter() {
        mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(this@MainActivity)
        if (mBluetoothAdapter == null) {
            Toast.makeText(this@MainActivity, "Bluetooth not supported!", Toast.LENGTH_SHORT).show()
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bluetoothLeScanner = mBluetoothAdapter?.bluetoothLeScanner
        }
    }

    private fun setUpViewModel(activityMainBinding: ActivityMainBinding) {
        scaleViewModel = ViewModelProvider(this, factory).get(WeighingScaleViewModel::class.java)
        activityMainBinding.viewModel = scaleViewModel

        scaleViewModel.getAllScales.observe(this) { scalesList ->
            scaleViewModel.showEmptyView.set(scalesList.isNullOrEmpty())
            weighingScaleListAdapter.setAllScales(scalesList.toMutableList())
            setupBleIfInList()
        }
    }

    private fun setUpRecyclerView(activityMainBinding: ActivityMainBinding) {
        recyclerView = activityMainBinding.weighingScaleRecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.setHasFixedSize(true)

        weighingScaleListAdapter = WeighingScaleListAdapter(this)
        recyclerView?.adapter = weighingScaleListAdapter
        weighingScaleListAdapter.setContext(this)
    }

    private fun setUpToolBar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        connected?.let { changeConnectionStatus(it) } // to ensure the latest status is displayed

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val icon = item.icon
            if (icon != null) {
                item.icon = icon.applyTint(this)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_bluetooth -> {

                if (weighingScaleListAdapter.blePos == -1) {
                    val scale = weighingScaleListAdapter.getWeighingScale(ScaleType.BLUETOOTH)
                    scaleViewModel.insertScale(scale)
                    setUpBleAdapter()
                    turnOnBleGps()
                    showToast("${scale.name} Added!!")
                } else {
                    showToast("Not Supported!!")
                }

                true
            }
            R.id.action_local_wifi -> {

                if(getSSID().equals(SSID)){
                    val scale = weighingScaleListAdapter.getWeighingScale(ScaleType.WIFI)
                    scaleViewModel.insertScale(scale)
                    showToast("${scale.name} Added!!")
                }else{
                    showToast(WIFI_MSG_CONNECT_SCALE)
                }
                true
            }
            R.id.action_internet -> {
                val scale = weighingScaleListAdapter.getWeighingScale(ScaleType.INTERNET)
                scaleViewModel.insertScale(scale)

                // schedule message service
                service = Intent(applicationContext, MessageService::class.java)
                applicationContext.startService(service)

                showToast("${scale.name} Added!!")
                true
            }
            R.id.action_refresh -> {
              //  showToast("Reset Button Clicked!!")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showToast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLEService = (service as BluetoothLEService.LocalBinder).service
            if (mBluetoothLEService?.initialize() == false) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }

            mBluetoothLEService?.connect(bluetoothDevice!!.address)
            startScanning(false)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLEService = null
        }
    }


    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            bluetoothDevice = result.device
            val gattServiceIntent = Intent(this@MainActivity, BluetoothLEService::class.java)
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
            Log.d(TAG, "address: " + bluetoothDevice?.address)
            weighingScaleListAdapter.getWeighingScale(ScaleType.BLUETOOTH).visible = View.GONE
            weighingScaleListAdapter.notifyDataSetChanged()
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "Scanning Failed $errorCode")
        }
    }

    private fun startScanning(enable: Boolean) {
        val mHandler = Handler()
        if (enable) {
            val arrFilters = ArrayList<ScanFilter>()
            val settings = ScanSettings.Builder().build()
            val scanFilter1: ScanFilter = ScanFilter.Builder().setServiceUuid(
                ParcelUuid.fromString(
                    BluetoothUtils.UUID1
                )
            ).build()
            val scanFilter2: ScanFilter = ScanFilter.Builder().setServiceUuid(
                ParcelUuid.fromString(
                    BluetoothUtils.UUID2
                )
            ).build()
            val scanFilter3: ScanFilter = ScanFilter.Builder().setServiceUuid(
                ParcelUuid.fromString(
                    BluetoothUtils.UUID2
                )
            ).build()
            arrFilters.add(scanFilter1)
            arrFilters.add(scanFilter2)
            arrFilters.add(scanFilter3)
            mHandler.postDelayed({
                if (bluetoothLeScanner != null) {
                    try {
                        weighingScaleListAdapter.getItemAtPosition(weighingScaleListAdapter.blePos).visible = View.GONE
                        weighingScaleListAdapter.notifyDataSetChanged()
                    } catch (e: java.lang.Exception) {
                        e.message
                    }

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return@postDelayed
                    }
                    bluetoothLeScanner?.stopScan(scanCallback)
                }
            }, BluetoothUtils.SCAN_PERIOD)
            if (bluetoothLeScanner != null) {
                try {
                    weighingScaleListAdapter.getItemAtPosition(weighingScaleListAdapter.blePos).visible = View.VISIBLE
                    weighingScaleListAdapter.notifyDataSetChanged()
                } catch (e: java.lang.Exception) {
                    e.message
                }
                bluetoothLeScanner?.startScan(arrFilters, settings, scanCallback)
            }
        } else {
            mHandler.post {
                if (bluetoothLeScanner != null) {
                    try {
                        weighingScaleListAdapter.getItemAtPosition(weighingScaleListAdapter.blePos).visible =View.GONE
                        weighingScaleListAdapter.notifyDataSetChanged()
                    } catch (e: java.lang.Exception) {
                        e.message
                    }
                    bluetoothLeScanner?.stopScan(scanCallback)
                }
            }
        }
    }

    override fun onTareClick(position: Int) {
        if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.WIFI){

            if (null != clientThread) {
                clientThread?.sendMessage("T")
            }else{
                Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
            }
        }else if (mNotifyCharacteristic != null) {
            mBluetoothLEService?.writeCharacteristic(mNotifyCharacteristic!!, "T")
        } else {
            Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
        }

//        showToast(
//            "${resources.getString(R.string.tare)} Button of ${
//                weighingScaleListAdapter?.getItemAtPosition(position)?.name
//            } Clicked"
//        )
    }

    override fun onModeClick(position: Int) {
        if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.WIFI){

            if (null != clientThread) {
                clientThread?.sendMessage("M")
            }else{
                Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
            }
        }else if (mNotifyCharacteristic != null) {
            mBluetoothLEService?.writeCharacteristic(mNotifyCharacteristic!!, "M")
        } else {
            Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
        }

//        showToast(
//            "${resources.getString(R.string.mode)} Button of ${
//                weighingScaleListAdapter?.getItemAtPosition(position)?.name
//            } Clicked!!"
//        )
    }

    override fun onMPlusIncClick(position: Int) {


        if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.WIFI){

            if (null != clientThread) {
                clientThread?.sendMessage("I")
            }else{
                Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
            }
        }else if (mNotifyCharacteristic != null) {
            mBluetoothLEService?.writeCharacteristic(mNotifyCharacteristic!!, "I")
        } else {
            Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
        }

//        showToast(
//            "${resources.getString(R.string.m_plus_Inc)} Button of ${
//                weighingScaleListAdapter?.getItemAtPosition(position)?.name
//            } Clicked!!"
//        )
    }

    override fun onMRShiftClick(position: Int) {

        if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.WIFI){

            if (null != clientThread) {
                clientThread?.sendMessage("S")
            }else{
                Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
            }
        }else if (mNotifyCharacteristic != null) {
            mBluetoothLEService?.writeCharacteristic(mNotifyCharacteristic!!, "S")
        } else {
            Toast.makeText(this@MainActivity, "Please connect again!", Toast.LENGTH_SHORT).show()
        }

//        showToast(
//            "${resources.getString(R.string.mr_shift)} Button of ${
//                weighingScaleListAdapter.getItemAtPosition(position).name
//            } Clicked!!"
//        )
    }

    override fun onRefreshScaleFABClick(position: Int) {

        if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.WIFI){
            if(getSSID().equals(SSID)){
                if (clientThread == null) {
                    clientThread = WifiClientThread()
                }
                val thread = Thread(clientThread)
                thread.start()
            }else{
                showToast(WIFI_MSG_CONNECT_SCALE)
            }

        }else if(weighingScaleListAdapter.getItemAtPosition(position).type==ScaleType.BLUETOOTH){
            try {
                setUpBleAdapter()
                turnOnBleGps()

                if (bluetoothDevice != null) {
                    mBluetoothLEService?.connect(bluetoothDevice!!.address)
                } else {
                    startScanning(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onRemoveClick(position: Int) {
        val scale = weighingScaleListAdapter.getItemAtPosition (position)
        if (scale.type == ScaleType.BLUETOOTH) {
            closeConnection()
            setBluetooth(false)
            weighingScaleListAdapter.blePos = -1
        }else if(scale.type == ScaleType.INTERNET){
            if(service!=null){
                stopService(service)
            }
        }
        scaleViewModel.deleteScale(scale)
        showToast("${scale.name} Removed!!")
    }

    private fun injectActivity() {
        getApp().appComponent.inject(this)
    }

    private fun getApp(): WeighingScaleApplication {
        return applicationContext as WeighingScaleApplication
    }

    override fun onConnected(p0: Bundle?) {
        mLocationRequest = LocationRequest.create()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = (30 * 1000).toLong()
        mLocationRequest.fastestInterval = (5 * 1000).toLong()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        builder.setAlwaysShow(true)

        result = LocationServices.SettingsApi.checkLocationSettings(
            mGoogleApiClient,
            builder.build()
        )

        result?.setResultCallback(ResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                         // Location settings are not satisfied. But could be fixed by showing the user
                    // a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(this@MainActivity, REQUEST_LOCATION)
                    } catch (e: java.lang.Exception) {
                        // Ignore the error.
                    }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                }
            }
        })
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("onActivityResult()", Integer.toString(resultCode))

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        when (requestCode) {
            REQUEST_LOCATION -> when (resultCode) {
                RESULT_OK -> {

                    // All required changes were successfully made
                    Toast.makeText(
                        this@MainActivity,
                        "Location enabled by user!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                RESULT_CANCELED -> {

                    // The user was asked to change settings, but chose not to
                    Toast.makeText(
                        this@MainActivity,
                        "Location not enabled, user cancelled.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                }
            }
        }
    }

    private fun closeConnection() {
        mBluetoothLEService?.disconnect()
        mBluetoothLEService?.close()
        try {
            unregisterReceiver(mGattUpdateReceiver)
            unregisterReceiver(mReceiver)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        if(weighingScaleListAdapter.blePos!=-1){
            setBluetooth(false)
            closeConnection()
        }
    }

}



