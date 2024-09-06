package com.polar.androidblesdk

//import LocationService
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

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

//    private lateinit var polarTimestamp: String
    // ATTENTION! Replace with the device ID from your device.

    private var deviceId = "unknown"
    private var selectedDate = ""
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
    private var isLocationServiceRunning: Boolean = false

    private var sdkModeEnabledStatus = false
    private var deviceConnected = false
    private var bluetoothEnabled = false

//    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//        .requestEmail()
//        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
//        .build()

//    val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)


    private lateinit var connectButton: Button

    //Verity Sense offline recording use
    private lateinit var ofaButton: Button
    private lateinit var promptID: Button
    private lateinit var uploadButton: Button
    private lateinit var wakeLock: PowerManager.WakeLock

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
                checkForGooglePermissions(deviceId, getCurrentDate())
            }
            catch(e: ApiException){

            }
        }
    }

    private var specLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK){
            val data: Intent? = result.data
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.getResult(ApiException::class.java)
                checkForGooglePermissions(deviceId, selectedDate)
            }
            catch(e: ApiException){

            }
        }
    }

    private fun checkForGooglePermissions(sensorId: String, date: String){
        showToast("Checking Permissions...")
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
            lifecycle.coroutineScope.launch{driveSetUp(sensorId, date)}
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

    private suspend fun driveSetUp(sensorId: String, date: String){
        showToast("Setting up Google Drive...")
        uploadButton.setBackgroundColor(ContextCompat.getColor(this, R.color.specialColor))
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
                val folderName = "${sensorId}_${date}"
                val googleDriveFileHolderJob = async {
                    createFolder(googleDriveService, folderName, null)
                }
                val googleDriveFileHolder = googleDriveFileHolderJob.await()
//                val createFileJob = async { createFileInInternalStorage(receivedText) }
//                val file = createFileJob.await()
                val fileNames = listOf(
                    "PPG_${sensorId}_${date}.zip",
                    "ACC_${sensorId}_${date}.zip",
                    "GPS_${sensorId}_${date}.zip",
                    )
                var allGood = true
                for (fileName in fileNames) {
                    var file = getFileFromDownloads(fileName)
                    if (file == null) {
                        val originalFileName = fileName.replace(".zip", ".txt")
                        compressFile(originalFileName)
                        file = getFileFromDownloads(fileName) // Try getting the compressed file again
                    }
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
                    uploadButton.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primaryColor))
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

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_edited)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")
        acquireWakeLock()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        ofaButton = findViewById(R.id.one_for_all_button)
        promptID = findViewById(R.id.prompt_ID_button)
        deviceId = getData(this, "currentID", "unknown")
        uploadButton = findViewById(R.id.upload_button)
        connectButton = findViewById(R.id.connect_button)
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
//            startLocationUpdates()
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

        promptID.setOnClickListener {
            val input = EditText(this)
            val dialog = AlertDialog.Builder(this)
                .setTitle("Enter your device ID: ")
                .setView(input)
                .setNeutralButton("tools") { _, _ ->
                    val toolDialog = AlertDialog.Builder(this)
                        .setTitle("Choose an option:")
                        .setNegativeButton("Upload a specific date") { _, _ ->
//                            val dateInput = EditText(this)
                            val dateDialog = AlertDialog.Builder(this@MainActivity)
                                .setTitle("Select the date you want to upload")
//                                .setView(dateInput)
                                .setPositiveButton("Pick Date") { _, _ ->
                                    showDatePickerDialog(this) { selected ->
//                                        dateInput.setText(selectedDate)
                                        Log.d(TAG, "Selected date: $selectedDate")
                                        selectedDate = selected
                                        isFileRead = false
                                        val gso = GoogleSignInOptions
                                            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestEmail()
                                            .build()

                                        googleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)

                                        val signInIntent = googleSignInClient?.signInIntent
                                        specLauncher.launch(signInIntent)
                                    }
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.cancel()
                                }
                                .create()
                            dateDialog.show()
                        }
                        .setPositiveButton("Delete all data") { _, _ ->
                            val ensureDialog = AlertDialog.Builder(this)
                                .setTitle("Are you sure?:")
                                .setPositiveButton("Yes, I want to DELETE all data.") { _, _ ->
                                    deleteAllDataForDevice()
                                }
                                .setNegativeButton("No, I want to KEEP the data.") { ensureDialog, _ ->
                                    ensureDialog.cancel()
                                }
                                .create()
                            ensureDialog.show()
                        }
                        .setNeutralButton("Cancel") { toolDialog, _ ->
                            toolDialog.cancel()
                        }
                        .create()
                    toolDialog.show()
                }
                .setPositiveButton("OK") { _, _ ->
                    deviceId = input.text.toString()
                    connectButton.text = getString(R.string.connect_to_device, deviceId)
                    saveData(this, "currentID", deviceId)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
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
                showToast("Failed to $attempt. Please contact a researcher to support.")
            }
        }

        ofaButton.setOnClickListener{
            val mainScope = MainScope()
            if (ofaButtonUp){
                ofaButton.setBackgroundColor(ContextCompat.getColor(this, R.color.specialColor))
                ofaButtonUp = false
                ofaButton.text = "end"
//                showToast("Record starts.")
//                startLocationUpdates()
                startLocationService()
                isLocationServiceRunning = true


            }
            else{
                ofaButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))
//                releaseWakeLock()
                ofaButton.text = "start"
                ofaButtonUp = true
                stopLocationService()
                isLocationServiceRunning = false
                showToast("Record ends.")
                try{
                    compressFile("ACC_${deviceId}_${getCurrentDate()}.txt")
                    compressFile("PPG_${deviceId}_${getCurrentDate()}.txt")
                    compressFile("GPS_${deviceId}_${getCurrentDate()}.txt")
                    showToast("Files compressed")
                }
                catch(e: Exception){
                    showToast("Error compressing file")
                }
            }
            mainScope.launch {
                val isDisposed = accDisposable?.isDisposed ?: true
                if (isDisposed) {
                    //                toggleButtonDown(accButton, R.string.stop_acc_stream)
                    accDisposable =
                        requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
                            .flatMap { settings: PolarSensorSetting ->
                                api.startAccStreaming(deviceId, settings)
                            }
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { polarAccelerometerData: PolarAccelerometerData ->
                                    for (data in polarAccelerometerData.samples) {
                                        //                                accValue.text =
                                        //                                    "ACC    x: ${data.x} y: ${data.y} z: ${data.z}"

                                        //                                polarTimestamp = data.timeStamp.toString()

                                        if (ofaButtonUp == false) {
                                            //                                    var hashedACC = hashMapOf(
                                            //                                        "x" to data.x,
                                            //                                        "y" to data.y,
                                            //                                        "z" to data.z,
                                            //                                        "timeStamp" to data.timeStamp,
                                            //                                    )
//                                            verifyStoragePermissions(this)
                                            var accFileName =
                                                "ACC_${deviceId}_${getCurrentDate()}.txt"
                                            var unixTimestamp =
//                                                Instant.now().nano.toString()
                                                getNano()
                                            var accLine =
                                                "${unixTimestamp};${data.x};${data.y};${data.z};"
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
                    // NOTE dispose will stop streaming if it is "running"
                    accDisposable?.dispose()
                }
            }

            // PPG and location part
            mainScope.launch {
                val isDisposed = ppgDisposable?.isDisposed ?: true
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
                                        //                                    Log.d(uploading.toString(), "record GPS status")
                                        if (ofaButtonUp == false) {
                                            for (data in polarPpgData.samples) {
                                                var ppgFileName =
                                                    "PPG_${deviceId}_${getCurrentDate()}.txt"
//                                                var unixTimestamp =
//                                                    Instant.now().nano.toString()
                                                var unixTimestamp = getNano()
                                                var ppgLine =
                                                    "${unixTimestamp};${data.channelSamples[0]};${data.channelSamples[1]};${data.channelSamples[2]};${data.channelSamples[3]};"
                                                createOrAppendFileInExternalStorage(
                                                    ppgFileName,
                                                    ppgLine
                                                )
                                            }
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
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = "com.polar.androidblesdk"
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
//                showBatteryOptimizationDialog()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val packageName = packageName
                    val intent = Intent()
                    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                }
            } else {

            }
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
//                            startLocationUpdates()
                            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                        }
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
//                            Toast.makeText(this, "Write permission granted", Toast.LENGTH_SHORT).show()
//                        }
//                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
//                            Toast.makeText(this, "Read permission granted", Toast.LENGTH_SHORT).show()
//                        }
                        Manifest.permission.BLUETOOTH_SCAN -> {
                            Toast.makeText(this, "Bluetooth Scan permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_CONNECT -> {
                            Toast.makeText(this, "Bluetooth Connect permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH -> {
                            Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.WAKE_LOCK -> {
                            Toast.makeText(this, "Wake Lock permission granted", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    allPermissionsGranted = false
                    when (permissions[index]) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                        }
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
//                            Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show()
//                        }
//                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
//                            Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show()
//                        }
                        Manifest.permission.BLUETOOTH_SCAN -> {
                            Toast.makeText(this, "Bluetooth Scan permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH_CONNECT -> {
                            Toast.makeText(this, "Bluetooth Connect permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.BLUETOOTH -> {
                            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                        }
                        Manifest.permission.WAKE_LOCK -> {
                            Toast.makeText(this, "Wake Lock permission denied", Toast.LENGTH_SHORT).show()
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
//            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
//        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), PERMISSION_REQUEST_CODE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS), PERMISSION_REQUEST_CODE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WAKE_LOCK), PERMISSION_REQUEST_CODE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {

            } else {

            }
        }
    }


    public override fun onDestroy() {
        super.onDestroy()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        api.shutDown()
    }

    @OptIn(ExperimentalTime::class)
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply{
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations){
//                    updateUI(location)
                    longitude = (location.longitude).toString()
                    latitude = (location.latitude).toString()

                    if (ofaButtonUp == false){
                        var gpsFileName = "GPS_${deviceId}_${getCurrentDate()}.txt"
//                        var unixTimestamp = Instant.now().nano.toString()
                        var unixTimestamp = getNano()
                        var gpsLine = "${unixTimestamp};${location.latitude};${location.longitude};\n"
                        createOrAppendFileInExternalStorage(gpsFileName, gpsLine)
                        showToast("gps stored")
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java).apply {
            putExtra("deviceId", deviceId)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopLocationService() {
        val stopIntent = Intent(this, LocationService::class.java)
        stopIntent.action = LocationService.ACTION_STOP
        ContextCompat.startForegroundService(this, stopIntent)
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

    fun deleteFileFromExternalStorage(filename: String) {
        // Check if external storage is available
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            // Get the Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                val file = File(downloadsDir, filename)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Toast.makeText(this, "File deleted successfully from Downloads", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error deleting file", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "File not found in Downloads", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Downloads directory not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "External storage is not mounted", Toast.LENGTH_SHORT).show()
        }
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

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire()
            Toast.makeText(this, "Wake Lock Acquired", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
            Toast.makeText(this, "Wake Lock Released", Toast.LENGTH_SHORT).show()
        }
    }
    fun deleteAllDataForDevice() {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                val files = downloadsDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.startsWith("ACC_") ||
                            file.name.startsWith("PPG_") ||
                            file.name.startsWith("GPS_")) {
                            val deleted = file.delete()
                            if (deleted) {
                                Toast.makeText(this, "${file.name} deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Error deleting ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Downloads directory not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "External storage is not mounted", Toast.LENGTH_SHORT).show()
        }
        showToast("Process finished.")
    }

    private fun showDatePickerDialog(context: Context, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDateString = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(selectedDateString)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    fun compressFile(fileName: String) {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val inputFile = File(downloadsDirectory, fileName)
        val outputFile = File(downloadsDirectory, "${fileName.replace(".txt", ".zip")}")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            FileInputStream(inputFile).use { fis ->
                val zipEntry = ZipEntry(inputFile.name)
                zos.putNextEntry(zipEntry)
                fis.copyTo(zos, bufferSize = 1024)
                zos.closeEntry()
            }
        }

        println("File compressed to ${outputFile.absolutePath}")
    }

    @OptIn(ExperimentalTime::class)
    fun getNano(): String{
        var nanoTime = TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
        var milli = Instant.now().toEpochMilli()
        var rlt = milli * 1_000_000 + nanoTime
        return rlt.toString()
    }
}