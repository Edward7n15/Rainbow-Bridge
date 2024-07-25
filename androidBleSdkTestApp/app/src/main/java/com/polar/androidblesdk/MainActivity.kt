package com.polar.androidblesdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.provider.Settings
import android.provider.Settings.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.Instant

//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInClient
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.common.api.Scope
//import com.google.android.gms.drive.Drive
//import com.google.android.gms.drive.DriveClient
//import com.google.android.gms.drive.DriveContents
//import com.google.android.gms.drive.DriveResourceClient
//import com.google.android.gms.drive.MetadataChangeSet
//import com.google.android.gms.drive.DriveScopes
//import android.content.Intent
//import java.io.FileInputStream



class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2

        const val RC_AUTHORIZE_DRIVE = 1
        const val MY_RECOVERED_DIR = "CealRecovered"
        const val MY_RECOVERED_TEXT_FILE = "MyRecovered.txt"
        const val MY_APP = "MyApp"
        const val MY_APP_LOWER = "myapp"
        const val TEXT = "text/plain"
        const val DRIVE = "drive"
        const val ROOT = "root"
        const val MY_WALLET = "My Wallet"
        const val MY_TEXT_FILE = "My.txt"
        const val GOOGLE_DRIVE = "application/vnd.google-apps.folder"
    }

//    private lateinit var list_adapter: ArrayAdapter<String>
//    private val items: MutableList<String> = mutableListOf()

    private lateinit var polarTimestamp: String
    // ATTENTION! Replace with the device ID from your device.

    private var deviceId = "unknown"
    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_LED_ANIMATION
            )
        )
    }
//    private lateinit var broadcastDisposable: Disposable
    private var scanDisposable: Disposable? = null
    private var autoConnectDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null
    //    private var ecgDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private var gyrDisposable: Disposable? = null
    private var magDisposable: Disposable? = null
    private var ppgDisposable: Disposable? = null
    private var ppiDisposable: Disposable? = null
    private var sdkModeEnableDisposable: Disposable? = null
    private var recordingStartStopDisposable: Disposable? = null
    private var recordingStatusReadDisposable: Disposable? = null

    private var uploadButtonUp: Boolean = true
    private var ofaButtonUp: Boolean = true

    private var sdkModeEnabledStatus = false
    private var deviceConnected = false
    private var bluetoothEnabled = false

//    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//        .requestEmail()
//        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
//        .build()

//    val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)


//    private lateinit var broadcastButton: Button
    private lateinit var connectButton: Button
//    private lateinit var autoConnectButton: Button
//    private lateinit var scanButton: Button
//    private lateinit var hrButton: Button
    //    private lateinit var ecgButton: Button
//    private lateinit var accButton: Button
//    private lateinit var gyrButton: Button
//    private lateinit var magButton: Button
//    private lateinit var ppgButton: Button
//    private lateinit var ppgValue: TextView
//    private lateinit var gpsValue: TextView
//    private lateinit var accValue: TextView
//    private lateinit var ppiButton: Button
    //    private lateinit var listExercisesButton: Button
    //    private lateinit var setTimeButton: Button
//    private lateinit var toggleSdkModeButton: Button

    //Verity Sense offline recording use
    private lateinit var ofaButton: Button
    private lateinit var promptID: Button
    private lateinit var uploadButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    var uploading = false
    private lateinit var latitude: String
    private lateinit var longitude: String

    private val db = Firebase.firestore

    private val fields = "nextPageToken, files(id, name)"

    private val accessDriveScope: Scope = Scope(Scopes.DRIVE_FILE)
    private val scopeEmail: Scope = Scope(Scopes.EMAIL)

    private var isFileRead = false
    private var googleSignInClient: GoogleSignInClient? = null
    private val receivedText: String = "Hello World"

    private var launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK){
            val data: Intent? = result.data
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.getResult(ApiException::class.java)
                checkForGooglePermissions()
            }
            catch(e: ApiException){

            }
        }
    }

    private fun checkForGooglePermissions(){
        if (!GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(this),
            accessDriveScope,
            scopeEmail
            )){
                GoogleSignIn.requestPermissions(
                    this,
                    RC_AUTHORIZE_DRIVE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    accessDriveScope,
                    scopeEmail
                )
        }
        else{
            lifecycle.coroutineScope.launch{driveSetUp()}
        }
    }

    private fun createTempFileInInternalStorage(): File {
        var privateDir = filesDir
        privateDir = File(privateDir, MY_RECOVERED_DIR)
        privateDir.mkdirs()
        privateDir = File(privateDir, MY_RECOVERED_TEXT_FILE)
        return privateDir
    }

    private fun readDataFromFile(file: File): String? {
        try{
            val br = BufferedReader(FileReader(file))
            val line = br.readLines().joinToString()
            br.close()
            return line
        }
        catch(e: Exception){

        }
        return null
    }

    private suspend fun driveSetUp(){
        val myAccount = GoogleSignIn.getLastSignedInAccount(this)
        val credential = GoogleAccountCredential.usingOAuth2(
            this,
            Collections.singleton(Scopes.DRIVE_FILE)
        )

        credential.selectedAccount = myAccount?.account

        val googleDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(MY_WALLET).build()

        if (isFileRead){
            withContext(Dispatchers.Default){
                try{
                    val fileListJob = async{ listDriveFiles(googleDriveService)}
                    val fileList = fileListJob.await()
                    val cealAppEnv = "${MY_APP_LOWER}.txt"

                    if (fileList.isNotEmpty()){
                        if (fileList.any {fileListItem ->
                            (fileListItem.name == MY_TEXT_FILE || fileListItem.name == cealAppEnv)
                        }){
                            for( item in fileList ){
                                if (item.name == cealAppEnv || item.name == MY_TEXT_FILE){
                                    val createFileJob = async{ createTempFileInInternalStorage() }
                                    val file = createFileJob.await()
                                    val downloadFileJob = async{
                                        downloadFile(googleDriveService, file, fileList[0].id)
                                    }

                                    downloadFileJob.await()

                                    val readJob = async{ file?.let{ readDataFromFile(it) } }
                                    val text = readJob.await()
                                    withContext(Dispatchers.Main){ Toast.makeText(this@MainActivity, "$text", Toast.LENGTH_LONG).show()}

                                    break
                                }
                            }
                        }
                        else{

                        }
                    }
                    else{

                    }
                }
                catch(e: Exception){

                }
            }
        }
        else{
            withContext(Dispatchers.Default){
                val folderName = "${deviceId}_${getCurrentDate()}"
                val googleDriveFileHolderJob = async {
                    createFolder(googleDriveService, folderName, null)
                }
                val googleDriveFileHolder = googleDriveFileHolderJob.await()
//                val createFileJob = async { createFileInInternalStorage(receivedText) }
//                val file = createFileJob.await()
                val fileNames = listOf(
                    "PPG_${deviceId}_${getCurrentDate()}.txt",
                    "ACC_${deviceId}_${getCurrentDate()}.txt",
                    "PGS_${deviceId}_${getCurrentDate()}.txt",
                    )
                var allGood = true
                for (fileName in fileNames) {
                    val file = getFileFromDownloads(fileName)
                    if (file !== null) {
                        val uploadFileJob = async {
                            uploadFile(
                                googleDriveService,
                                file,
                                TEXT,
                                googleDriveFileHolder?.id,
                            )
                        }
                        uploadFileJob.await()
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@MainActivity,
//                                "Success! Thank you for uploading data today!",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        }
                    } else {
                        showToast("Seems something went wrong...")
                        allGood = false
                    }
                }
                if (allGood){
                    withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Success! Thank you for uploading data today!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }
    }

    private fun createFolder(
        myDriveService: Drive,
        folderName: String?,
        folderId: String?
    ): GoogleDriveFileHolder?{
        try {
            val googleDriveFileHolder = GoogleDriveFileHolder()
            val root = folderId?.let { listOf(it) } ?: listOf(ROOT)
            val metadata = com.google.api.services.drive.model.File()
                .setParents(root)
                .setMimeType(GOOGLE_DRIVE)
                .setName(folderName)
            val googleFile = myDriveService
                .files()
                .create(metadata)
                .execute()
                ?: throw IOException("IO Exception")

            googleDriveFileHolder.id = googleFile.id
            return googleDriveFileHolder
        }
        catch (e:Exception){

        }
        return null
    }

    private fun uploadSpecFile(myDriveService: Drive, mimeType: String?, folderID: String?, filename: String): GoogleDriveFileHolder {
        // Path to the file created by the ofaButton
        val localFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${filename}.txt")

        if (!localFile.exists()) {
            Log.e(TAG, "File not found: ${localFile.absolutePath}")
            return GoogleDriveFileHolder().apply { name = "File not found"; id = "" }
        }

        val root = folderID?.let { listOf(it) } ?: listOf(ROOT)
        val metadata = com.google.api.services.drive.model.File()
            .setParents(root)
            .setMimeType(mimeType)
            .setName(localFile.name)

        val fileContent = FileContent(mimeType, localFile)
        val fileMeta = myDriveService.files().create(
            metadata,
            fileContent
        ).execute()

        val googleDriveFileHolder = GoogleDriveFileHolder()
        googleDriveFileHolder.id = fileMeta.id
        googleDriveFileHolder.name = fileMeta.name

        return googleDriveFileHolder
    }


    private fun uploadFile(
        myDriveService: Drive,
        localFile: File,
        mimeType: String?,
        folderID: String?
        ): GoogleDriveFileHolder{
        val root = folderID?.let{ listOf(it) }?: listOf(ROOT)
        val metadata = com.google.api.services.drive.model.File()
            .setParents(root)
            .setMimeType(mimeType)
            .setName(localFile.name)

        val fileContent = FileContent(mimeType, localFile)
        val fileMeta = myDriveService.files().create(
            metadata,
            fileContent
        ).execute()

        val googleDriveFileHolder = GoogleDriveFileHolder()
        googleDriveFileHolder.id = fileMeta.id
        googleDriveFileHolder.name = fileMeta.name

        return googleDriveFileHolder
    }

    private fun createFileInInternalStorage(text: String): File?{
        var privateDir = filesDir
        privateDir = File(privateDir, MY_APP)
        privateDir.mkdirs()
        privateDir = File(privateDir, "${MY_APP_LOWER}.txt")

        try {
            val fileOutputStream = FileOutputStream(privateDir)
            fileOutputStream.write(text.toByteArray())
            fileOutputStream.close()
            return privateDir
        }
        catch (e: FileNotFoundException){

        }
        return null
    }

    private fun downloadFile(
        myDriveService: Drive,
        targetFile: File?,
        fileId: String?
    ){
        val outputStream: OutputStream = FileOutputStream(targetFile)
        myDriveService.files()[fileId].executeMediaAndDownloadTo(outputStream)
    }

    private fun listDriveFiles(
        myDriveService: Drive
    ): List<com.google.api.services.drive.model.File>{
        var result: FileList
        var pageToken: String? = null

        do {
            result = myDriveService.files().list()
                .setQ("mimeType='${TEXT}'")
                .setSpaces(DRIVE)
                .setFields(fields)
                .setPageToken(pageToken)
                .execute()
        } while(pageToken != null)
        return result.files
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_edited)

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//            startLocationUpdates()
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
//        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        ofaButton = findViewById(R.id.one_for_all_button)
        promptID = findViewById(R.id.prompt_ID_button)
        deviceId = getData(this, "currentID", "unknown")
        uploadButton = findViewById(R.id.upload_button)

//        broadcastButton = findViewById(R.id.broadcast_button)
        connectButton = findViewById(R.id.connect_button)
//        autoConnectButton = findViewById(R.id.auto_connect_button)
//        scanButton = findViewById(R.id.scan_button)
//        accButton = findViewById(R.id.acc_button)
//        ppgButton = findViewById(R.id.ohr_ppg_button)
//        ppgValue = findViewById(R.id.ppg_value)
//        gpsValue = findViewById(R.id.gps_value)
//        accValue = findViewById(R.id.acc_value)
//        toggleSdkModeButton = findViewById(R.id.toggle_SDK_mode)

        //Verity Sense recording buttons

        api.setPolarFilter(false)

        // If there is need to log what is happening inside the SDK, it can be enabled like this:
        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }
        else {
            startLocationUpdates()
        }

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, deviceId)
                toggleButtonDown(connectButton, buttonText)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
                toggleButtonUp(connectButton, buttonText)
//                toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                // deprecated
            }
        })

//        val listView: ListView = findViewById(R.id.devices_list)
//        list_adapter = ArrayAdapter(this,
//            R.layout.list_item,
//            R.id.text_item,
//            items
//        )
//        val cus_adapter = CustomAdapter(this, items) { position -> onButtonClick(position) }
//        listView.adapter = cus_adapter

        promptID.setOnClickListener {
//            connectButton.text = getString(R.string.connect_to_device, deviceId)
//            items.add(deviceId)
//            cus_adapter.notifyDataSetChanged()
//            saveData(this, "currentID", deviceId)
            val input = EditText(this)
            val dialog = AlertDialog.Builder(this)
                .setTitle("Enter your device ID: ")
                .setView(input)
                .setNeutralButton("clear the list"){dialog, which ->
//                    items.clear()
//                    cus_adapter.notifyDataSetChanged()
                }
                .setPositiveButton("OK") { dialog, which ->
                    deviceId = input.text.toString()
//                    items.add(deviceId)
//                    cus_adapter.notifyDataSetChanged()
                    connectButton.text = getString(R.string.connect_to_device, deviceId)
                    saveData(this, "currentID", deviceId)
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
                .create()
            dialog.show()
        }

        uploadButton.setOnClickListener{
            uploadButtonUp = false
            // resumable upload PPG.txt to google drive
            isFileRead = false
            val gso = GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val signInIntent = googleSignInClient?.signInIntent
            launcher.launch(signInIntent)
        }

//        broadcastButton.setOnClickListener {
//            if (!this::broadcastDisposable.isInitialized || broadcastDisposable.isDisposed) {
//                toggleButtonDown(broadcastButton, R.string.listening_broadcast)
//                broadcastDisposable = api.startListenForPolarHrBroadcasts(null)
//                    .subscribe(
//                        { polarBroadcastData: PolarHrBroadcastData ->
//                            Log.d(TAG, "HR BROADCAST ${polarBroadcastData.polarDeviceInfo.deviceId} HR: ${polarBroadcastData.hr} batt: ${polarBroadcastData.batteryStatus}")
//                        },
//                        { error: Throwable ->
//                            toggleButtonUp(broadcastButton, R.string.listen_broadcast)
//                            Log.e(TAG, "Broadcast listener failed. Reason $error")
//                        },
//                        { Log.d(TAG, "complete") }
//                    )
//            } else {
//                toggleButtonUp(broadcastButton, R.string.listen_broadcast)
//                broadcastDisposable.dispose()
//            }
//        }

        connectButton.text = getString(R.string.connect_to_device, deviceId)
        connectButton.setOnClickListener {
            try {
                if (deviceConnected) {
                    api.disconnectFromDevice(deviceId)
                } else {
                    api.connectToDevice(deviceId)
                }
            } catch (polarInvalidArgument: PolarInvalidArgument) {
                val attempt = if (deviceConnected) {
                    "disconnect"
                } else {
                    "connect"
                }
                Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
            }
        }

//        autoConnectButton.setOnClickListener {
//            if (autoConnectDisposable != null) {
//                autoConnectDisposable?.dispose()
//            }
//            autoConnectDisposable = api.autoConnectToDevice(-60, "180D", null)
//                .subscribe(
//                    { Log.d(TAG, "auto connect search complete") },
//                    { throwable: Throwable -> Log.e(TAG, "" + throwable.toString()) }
//                )
//        }

//        scanButton.setOnClickListener {
//            val isDisposed = scanDisposable?.isDisposed ?: true
//            if (isDisposed) {
//                toggleButtonDown(scanButton, R.string.scanning_devices)
//                scanDisposable = api.searchForDevice()
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                        { polarDeviceInfo: PolarDeviceInfo ->
//                            Log.d(TAG, "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable)
//                            if (polarDeviceInfo.deviceId != deviceId) {
//                                deviceId = polarDeviceInfo.deviceId
//                                promptID.text = deviceId
//
//                                connectButton.text = getString(R.string.connect_to_device, deviceId)
//                                items.add(deviceId)
//                                cus_adapter.notifyDataSetChanged()
//                                saveData(this, "currentID", deviceId)
//                            }
//                        },
//                        { error: Throwable ->
//                            toggleButtonUp(scanButton, "Scan devices")
//                            Log.e(TAG, "Device scan failed. Reason $error")
//                        },
//                        {
//                            toggleButtonUp(scanButton, "Scan devices")
//                            Log.d(TAG, "complete")
//                        }
//                    )
//            } else {
//                toggleButtonUp(scanButton, "Scan devices")
//                scanDisposable?.dispose()
//            }
//        }

        ofaButton.setOnClickListener{
            if (ofaButtonUp){
                ofaButton.setBackgroundColor(ContextCompat.getColor(this, R.color.specialColor))
                ofaButtonUp = false
                ofaButton.text = "end"
                showToast("Record starts.")
            }
            else{
                ofaButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
                ofaButton.text = "start"
                ofaButtonUp = true
                showToast("Record ends.")
            }
            val isDisposed = accDisposable?.isDisposed ?: true
            if (isDisposed) {
//                toggleButtonDown(accButton, R.string.stop_acc_stream)
                accDisposable = requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startAccStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            for (data in polarAccelerometerData.samples) {
//                                accValue.text =
//                                    "ACC    x: ${data.x} y: ${data.y} z: ${data.z}"

                                polarTimestamp = data.timeStamp.toString()

                                if (uploading) {
//                                    var hashedACC = hashMapOf(
//                                        "x" to data.x,
//                                        "y" to data.y,
//                                        "z" to data.z,
//                                        "timeStamp" to data.timeStamp,
//                                    )
                                    verifyStoragePermissions(this)
                                    var accFileName = "ACC_${deviceId}_${getCurrentDate()}.txt"
                                    var unixTimestamp = Instant.now().toEpochMilli().toString()
                                    var accLine = "${getCurrentTimestamp()};$polarTimestamp;${unixTimestamp};${data.x};${data.y};${data.z};"
                                    createOrAppendFileInExternalStorage(accFileName, accLine)

//                                    var accCollection = db.collection(deviceId).document("ACC").collection("timestamp")
//                                    accCollection.document(polarTimestamp)
//                                        .set(hashedACC)
//                                        //                                        .addOnSuccessListener { Log.d(TAG, "acc collected") }
//                                        .addOnFailureListener { e ->
//                                            Log.w(
//                                                TAG,
//                                                "Error adding document",
//                                                e
//                                            )
//                                        }
                                }
                            }
                        },
                        { error: Throwable ->
//                            toggleButtonUp(accButton, R.string.start_acc_stream)
                            Log.e(TAG, "ACC stream failed. Reason $error")
                        },
                        {
                            showToast("ACC stream complete")
                            Log.d(TAG, "ACC stream complete")
                        }
                    )
            } else {
//                toggleButtonUp(accButton, R.string.start_acc_stream)
                // NOTE dispose will stop streaming if it is "running"
                accDisposable?.dispose()
            }

            // PPG and location part
            if (isDisposed) {
//                toggleButtonDown(ppgButton, R.string.stop_ppg_stream)
                ppgDisposable =
                    requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.PPG)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startPpgStreaming(deviceId, settings)
                        }
                        .subscribe(
                            { polarPpgData: PolarPpgData ->
                                if (polarPpgData.type == PolarPpgData.PpgDataType.PPG3_AMBIENT1) {
                                    uploading = true
                                    Log.d(uploading.toString(), "record GPS status")
                                    for (data in polarPpgData.samples) {
//                                        runOnUiThread {
//                                            ppgValue.text =
//                                                "average: ${(data.channelSamples[0] + data.channelSamples[1] + data.channelSamples[2]) / 3}\nppg0: ${data.channelSamples[0]}\nppg1: ${data.channelSamples[1]}\nppg2: ${data.channelSamples[2]}\nambient: ${data.channelSamples[3]}\ntimeStamp: ${data.timeStamp}"
//                                        }

                                        polarTimestamp = data.timeStamp.toString()
                                        var ppgFileName = "PPG_${deviceId}_${getCurrentDate()}.txt"
                                        var unixTimestamp = Instant.now().toEpochMilli().toString()
                                        var ppgLine = "${getCurrentTimestamp()};${data.timeStamp};${unixTimestamp};${data.channelSamples[0]};${data.channelSamples[1]};${data.channelSamples[2]};${data.channelSamples[3]};"
                                        createOrAppendFileInExternalStorage(ppgFileName, ppgLine)
                                        // we might want to normalize the ppg values
//                                        var hashedPPG = hashMapOf(
//                                            "ave" to (data.channelSamples[0] + data.channelSamples[1] + data.channelSamples[2]) / 3,
//                                            "ppg0" to data.channelSamples[0],
//                                            "ppg1" to data.channelSamples[1],
//                                            "ppg2" to data.channelSamples[2],
//                                            "timeStamp" to data.timeStamp,
//                                        )
//                                        var ppgCollection = db.collection(deviceId).document("PPG").collection("timestamp")
//                                        ppgCollection.document(polarTimestamp)
//                                            .set(hashedPPG)
//                                            .addOnFailureListener { Log.d(TAG, "ppg not collected") }
                                    }
                                }
                            },
                            { error: Throwable ->
//                                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                                Log.e(TAG, "PPG stream failed. Reason $error")
                            },
                            { Log.d(TAG, "PPG stream complete") }
                        )
            } else {
//                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                if (uploading) {
                    uploading = false
                }
                Log.d(uploading.toString(), "upload status")
                // NOTE dispose will stop streaming if it is "running"
                ppgDisposable?.dispose()
            }

        }

//        accButton.setOnClickListener {
//            val isDisposed = accDisposable?.isDisposed ?: true
//            if (isDisposed) {
//                toggleButtonDown(accButton, R.string.stop_acc_stream)
//                accDisposable = requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
//                    .flatMap { settings: PolarSensorSetting ->
//                        api.startAccStreaming(deviceId, settings)
//                    }
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                        { polarAccelerometerData: PolarAccelerometerData ->
//                            for (data in polarAccelerometerData.samples) {
//                                accValue.text =
//                                    "ACC    x: ${data.x} y: ${data.y} z: ${data.z}"
//
//                                polarTimestamp = data.timeStamp.toString()
//
//                                if (uploading) {
//                                    var hashedACC = hashMapOf(
//                                        "x" to data.x,
//                                        "y" to data.y,
//                                        "z" to data.z,
//                                        "timeStamp" to data.timeStamp,
//                                    )
//                                    verifyStoragePermissions(this)
//                                    var accFileName = "ACC.txt"
//                                    var line = "$polarTimestamp,${data.x},${data.y},${data.z}"
//                                    createOrAppendFileInExternalStorage(accFileName, line)
//
//                                var accCollection = db.collection(deviceId).document("ACC").collection("timestamp")
//                                accCollection.document(polarTimestamp)
//                                    .set(hashedACC)
//                                    //                                        .addOnSuccessListener { Log.d(TAG, "acc collected") }
//                                    .addOnFailureListener { e ->
//                                        Log.w(
//                                            TAG,
//                                            "Error adding document",
//                                            e
//                                        )
//                                    }
//                                }
//                            }
//                        },
//                        { error: Throwable ->
//                            toggleButtonUp(accButton, R.string.start_acc_stream)
//                            Log.e(TAG, "ACC stream failed. Reason $error")
//                        },
//                        {
//                            showToast("ACC stream complete")
//                            Log.d(TAG, "ACC stream complete")
//                        }
//                    )
//            } else {
//                toggleButtonUp(accButton, R.string.start_acc_stream)
//                // NOTE dispose will stop streaming if it is "running"
//                accDisposable?.dispose()
//            }
//        }
//
//        ppgButton.setOnClickListener {
//            val isDisposed = ppgDisposable?.isDisposed ?: true
//            if (isDisposed) {
//                toggleButtonDown(ppgButton, R.string.stop_ppg_stream)
//                ppgDisposable =
//                    requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.PPG)
//                        .flatMap { settings: PolarSensorSetting ->
//                            api.startPpgStreaming(deviceId, settings)
//                        }
//                        .subscribe(
//                            { polarPpgData: PolarPpgData ->
//                                if (polarPpgData.type == PolarPpgData.PpgDataType.PPG3_AMBIENT1) {
//                                    uploading = true
//                                    Log.d(uploading.toString(), "record GPS status")
//                                    for (data in polarPpgData.samples) {
//                                        runOnUiThread {
//                                            ppgValue.text =
//                                                "average: ${(data.channelSamples[0] + data.channelSamples[1] + data.channelSamples[2]) / 3}\nppg0: ${data.channelSamples[0]}\nppg1: ${data.channelSamples[1]}\nppg2: ${data.channelSamples[2]}\nambient: ${data.channelSamples[3]}\ntimeStamp: ${data.timeStamp}"
//                                        }
//
//                                        polarTimestamp = data.timeStamp.toString()
//                                        // we might want to normalize the ppg values
//                                        var hashedPPG = hashMapOf(
//                                            "ave" to (data.channelSamples[0] + data.channelSamples[1] + data.channelSamples[2]) / 3,
//                                            "ppg0" to data.channelSamples[0],
//                                            "ppg1" to data.channelSamples[1],
//                                            "ppg2" to data.channelSamples[2],
//                                            "timeStamp" to data.timeStamp,
//                                        )
//                                        var ppgCollection = db.collection(deviceId).document("PPG").collection("timestamp")
//                                        ppgCollection.document(polarTimestamp)
//                                            .set(hashedPPG)
//                                            .addOnFailureListener { Log.d(TAG, "ppg not collected") }
//                                    }
//                                }
//                            },
//                            { error: Throwable ->
//                                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
//                                Log.e(TAG, "PPG stream failed. Reason $error")
//                            },
//                            { Log.d(TAG, "PPG stream complete") }
//                        )
//            } else {
//                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
//                if (uploading) {
//                    uploading = false
//                }
//                Log.d(uploading.toString(), "upload status")
//                // NOTE dispose will stop streaming if it is "running"
//                ppgDisposable?.dispose()
//            }
//        }
//
//        toggleSdkModeButton.setOnClickListener {
//            toggleSdkModeButton.isEnabled = false
//            if (!sdkModeEnabledStatus) {
//                sdkModeEnableDisposable = api.enableSDKMode(deviceId)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                        {
//                            Log.d(TAG, "SDK mode enabled")
//                            // at this point dispose all existing streams. SDK mode enable command
//                            // stops all the streams but client is not informed. This is workaround
//                            // for the bug.
//                            disposeAllStreams()
//                            toggleSdkModeButton.isEnabled = true
//                            sdkModeEnabledStatus = true
//                            toggleButtonDown(toggleSdkModeButton, R.string.disable_sdk_mode)
//                        },
//                        { error ->
//                            toggleSdkModeButton.isEnabled = true
//                            val errorString = "SDK mode enable failed: $error"
//                            showToast(errorString)
//                            Log.e(TAG, errorString)
//                        }
//                    )
//            } else {
//                sdkModeEnableDisposable = api.disableSDKMode(deviceId)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                        {
//                            Log.d(TAG, "SDK mode disabled")
//                            toggleSdkModeButton.isEnabled = true
//                            sdkModeEnabledStatus = false
//                            toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
//                        },
//                        { error ->
//                            toggleSdkModeButton.isEnabled = true
//                            val errorString = "SDK mode disable failed: $error"
//                            showToast(errorString)
//                            Log.e(TAG, errorString)
//                        }
//                    )
//            }
//        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    private fun onButtonClick(position: Int) {
        try {
            if (deviceConnected) {
//                api.disconnectFromDevice(items[position])
            } else {
//                api.connectToDevice(items[position])
            }
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            val attempt = if (deviceConnected) {
                "disconnect"
            } else {
                "connect"
            }
            Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (index in permissions.indices) {
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    when (permissions[index]) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            startLocationUpdates()
                            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            Toast.makeText(this, "Write permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this, "Read permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_SCAN -> {
                            Toast.makeText(this, "Bluetooth Scan permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_CONNECT -> {
                            Toast.makeText(this, "Bluetooth Connect permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH -> {
                            Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    allPermissionsGranted = false
                    when (permissions[index]) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_SCAN -> {
                            Toast.makeText(this, "Bluetooth Scan permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_CONNECT -> {
                            Toast.makeText(this, "Bluetooth Connect permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH -> {
                            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "Needed permissions are granted")
                enableAllButtons()
            } else {
                Log.w(TAG, "No sufficient permissions")
                disableAllButtons()
                showToast("No sufficient permissions")
            }
        }
    }


    public override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), PERMISSION_REQUEST_CODE)
        }
    }


    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply{
            interval = 550
            fastestInterval = 550
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
                    updateUI(location)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun updateUI(location: Location){
//        gpsValue.text = "Latitude: ${location.latitude}\nLongitude: ${location.longitude}"
        // store the value here and keep updating it
        // upload it to firebase when PPG button is clicked
        longitude = (location.longitude).toString()
        latitude = (location.latitude).toString()

        if (uploading){
//        var hashedLocation = hashMapOf(
//            "lat" to location.latitude,
//            "lon" to location.longitude
//        )
        var gpsFileName = "PGS_${deviceId}_${getCurrentDate()}.txt"
        var unixTimestamp = Instant.now().toEpochMilli().toString()
        var gpsLine = "${getCurrentTimestamp()};${polarTimestamp};${unixTimestamp};${location.latitude};${location.longitude};"
        createOrAppendFileInExternalStorage(gpsFileName, gpsLine)

//        var gpsCollection = db.collection(deviceId).document("GPS").collection("timestamp")
//        gpsCollection.document(polarTimestamp)
//            .set(hashedLocation)
//            .addOnFailureListener { e -> Log.w(TAG, "Error adding document", e) }
        }
    }

    private fun toggleButtonDown(button: Button, text: String? = null) {
        toggleButton(button, true, text)
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButtonUp(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, false, getString(resourceId))
    }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun requestStreamSettings(identifier: String, feature: PolarBleApi.PolarDeviceDataType): Flowable<PolarSensorSetting> {
        val availableSettings = api.requestStreamSettings(identifier, feature)
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(TAG, "Full stream settings are not available for feature $feature. REASON: $error")
                PolarSensorSetting(emptyMap())
            }
        return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
            if (available.settings.isEmpty()) {
                throw Throwable("Settings are not available")
            } else {
                Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
                Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
                return@zip android.util.Pair(available, all)
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable()
            .flatMap { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                DialogUtility.showAllSettingsDialog(
                    this@MainActivity,
                    sensorSettings.first.settings,
                    sensorSettings.second.settings
                ).toFlowable()
            }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun showSnackbar(message: String) {
        val contextView = findViewById<View>(R.id.buttons_container)
        Snackbar.make(contextView, message, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Respond to positive button press
            }
            .show()
    }

    fun saveData(context: Context?, key: String, value: String) {
        context?.let {
            val sharedPreferences = it.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(key, value)
            editor.apply()
        }
    }

    fun getData(context: Context?, key: String, default: String): String {
        return context?.let {
            val sharedPreferences = it.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            sharedPreferences.getString(key, null) ?: default
        } ?: default
    }


    private fun disableAllButtons() {
//        broadcastButton.isEnabled = false
        connectButton.isEnabled = false
//        autoConnectButton.isEnabled = false
//        scanButton.isEnabled = false
//        accButton.isEnabled = false
//        ppgButton.isEnabled = false
//        toggleSdkModeButton.isEnabled = false
    }

    private fun enableAllButtons() {
//        broadcastButton.isEnabled = true
        connectButton.isEnabled = true
//        autoConnectButton.isEnabled = true
//        scanButton.isEnabled = true
//        accButton.isEnabled = true
//        ppgButton.isEnabled = true
//        toggleSdkModeButton.isEnabled = true
        //Verity Sense recording buttons
    }

    private fun disposeAllStreams() {
        accDisposable?.dispose()
        ppgDisposable?.dispose()
        ppgDisposable?.dispose()
    }

    val REQUEST_EXTERNAL_STORAGE = 1
    val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    fun createOrAppendFileInExternalStorage(filename:String, line: String) {
        // Check if external storage is available
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            // Get the Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                val file = File(downloadsDir, filename)
                try {
                    val fos = FileOutputStream(file, true) // Open file in append mode
                    fos.write((line + "\n").toByteArray())
                    fos.close()
//                    Toast.makeText(this, "File updated successfully in Downloads", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error updating file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Downloads directory not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "External storage is not mounted", Toast.LENGTH_SHORT).show()
        }
    }

    fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getFileFromDownloads(fileName: String): File? {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            // Get the Downloads directory
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                val file = File(downloadsDir, fileName)
                return file
            }
            else{
                return null
            }
        }
        else{
            return null
        }
    }
}