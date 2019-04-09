package com.mvproject.updater

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import java.net.URL
import android.os.AsyncTask
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.FileProvider


class Updater(private val view : Activity) : AppCompatActivity() {
    private val currentAppName: String
    private val currentAppVersion: String
    private var update: Update? = null
    private val permWriteStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val permGranted = PackageManager.PERMISSION_GRANTED
    private val permWriteStorageCode = 1000
    private val permInstallCode = 1001
    private var currentApi = 0

    init {
        currentAppName = getCurrentAppName()
        currentAppVersion = getCurrentVersionName()
        currentApi = Build.VERSION.SDK_INT
    }

    /**
     *  start updater
     */
    fun checkUpdateFromUrl(url : String){
        if (isNetworkConnected())
            CheckUpdate().execute(url)
        else
            toast(view.resources.getString(R.string.no_internet_message))
    }
    /**
    * check internet connection is available
    */
    private fun isNetworkConnected(): Boolean {
        val cm = view.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }

    /**
     * check json for available uodate
     */
    @SuppressLint("StaticFieldLeak")
    inner class CheckUpdate : AsyncTask<String, Int, Update?>() {

        override fun doInBackground(vararg params: String?): Update? {
            update = Gson().fromJson((URL(params[0]).readText()), com.mvproject.updater.Update::class.java)
            return update
        }

        override fun onPostExecute(result: Update?) {
            super.onPostExecute(result)
            promptForUpdate()
        }
    }

    /**
     * check update info and show dialog if update needed
     */
    private fun promptForUpdate(){
        if(checkUpdateNeeded()) showUpdateDialog()
    }

    /**
     * create and show updating dialog
     */
    private fun showUpdateDialog(){
        val dialog = AlertDialog.Builder(view).create()
        dialog.setTitle(currentAppName)
        dialog.setMessage(view.resources.getString(R.string.update_message))

        dialog.setButton(AlertDialog.BUTTON_POSITIVE,view.resources.getString(R.string.btn_update_text)){ _, _ ->
            checkForPermissionWriteStorage(permWriteStorage)
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,view.resources.getString(R.string.btn_later_text)){ _, _ ->
            toast(view.resources.getString(R.string.later_message))
        }

        dialog.show()

        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
    }


    /**
     * promt if needed for permission to write to storage
     */
    private fun checkForPermissionWriteStorage(permission : String){
        if (ContextCompat.checkSelfPermission(view, permission) != permGranted) {
            ActivityCompat.requestPermissions(view,arrayOf(permission),permWriteStorageCode)
        }
        else downloadFile(update)
    }

    /**
     * download file update from specified url
     */
    private fun downloadFile(update: Update?){
        deleteIfExist(update?.file)
        val request = DownloadManager.Request(Uri.parse(update?.url))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle(currentAppName)
        request.setDescription(view.resources.getString(R.string.upgrade_download_message))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,update?.file)
        val dm = view.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        view.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        dm.enqueue(request)
    }

    /**
     * delete previous update file if it exists
     */
    private fun deleteIfExist(filename : String?){
        filename?.let {
            val file = File(getFileFullPath(filename))

            if (file.exists())
                file.delete()
        }
    }

    /**
     * promt if needed for permission to install package
     */

    @SuppressLint("InlinedApi")
    fun checkForPermissionInstall(){
        when(currentApi) {
            in 23..25 ->{
                if (ContextCompat.checkSelfPermission(
                        view,
                        Manifest.permission.REQUEST_INSTALL_PACKAGES
                    ) != permGranted
                ) {
                    ActivityCompat.requestPermissions(
                        view,
                        arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
                        permInstallCode
                    )
                } else {
                    installUpdate(update?.file)
                }
            }
            else -> {
                installUpdate(update?.file)
            }
        }
    }

    /**
     *
     */
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permWriteStorageCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == permGranted)) {
                    downloadFile(update)
                } else {
                    ActivityCompat.requestPermissions(view,arrayOf(permissions[0]),permWriteStorageCode)
                }
                return
            }
            permInstallCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == permGranted)) {
                    installUpdate(update?.file)
                } else {
                    ActivityCompat.requestPermissions(view,arrayOf(permissions[0]),permInstallCode)
                }
                return
            }
            else -> {
            }
        }
    }

    /**
     * get the current app version
     */
    private fun getCurrentVersionName(): String {
        return view.packageManager.getPackageInfo(view.packageName, 0).versionName
    }

    /**
     * get the current app package name
     */
    private fun getCurrentPackageName(): String {
        return view.packageManager.getPackageInfo(view.packageName, 0).packageName
    }

    /**
     * get the current app name
     */
    private fun getCurrentAppName(): String {
        return view.packageManager.getApplicationLabel(
            view.packageManager.getApplicationInfo(
                getCurrentPackageName(),
                PackageManager.GET_META_DATA
            )
        ).toString()
    }

    /**
     * Check versions to perform update
     **/
    private fun checkUpdateNeeded() : Boolean {
        return update?.latestVersion.toString() > currentAppVersion
    }

    /**
     * Returning full path to file update
     */
    private fun getFileFullPath(filename: String) : String{
        return Environment.getExternalStorageDirectory().absolutePath + "/Download/" + filename
    }

    /**
     * install apk with app update
     */
    private fun installUpdate(filename: String?){
        filename?.let {
            val updateFile = getFileFullPath(filename)
            val intent = Intent(Intent.ACTION_VIEW)
            when (currentApi) {
                in 19..25 -> {
                    intent.setDataAndType(
                        Uri.fromFile(File(updateFile)), "application/vnd.android.package-archive"
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                in 26..28 -> {
                    val uriFile = getFileUri(view, (File(updateFile)))
                    intent.setDataAndType(
                        uriFile, "application/vnd.android.package-archive"
                    )
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            }
            view.startActivity(intent)
        }
    }

    private fun getFileUri(context : Context, file : File) : Uri {
        return FileProvider.getUriForFile(context,
                getCurrentPackageName() + ".provider" ,file)
    }

    /**
     * simplify toast message
     */
    private fun toast(msg : String){
        Toast.makeText(view,msg,Toast.LENGTH_SHORT).show()
    }

    /**
     * watch when download ending and start installing update
     */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                checkForPermissionInstall()
            }
        }
    }
}