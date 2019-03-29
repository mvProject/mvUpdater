package com.mvproject.updater

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.thin.downloadmanager.DownloadRequest
import com.thin.downloadmanager.DownloadStatusListenerV1
import com.thin.downloadmanager.ThinDownloadManager
import org.jetbrains.anko.*
import java.io.File
import java.net.URL
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.fragment.app.FragmentActivity
import com.kotlinpermissions.KotlinPermissions

class Updater(private val view : Activity) {
    private var fileJson = ""
    private var dm: ThinDownloadManager = ThinDownloadManager()
    private val currentAppName: String
    private val currentAppPackage: String
    private val currentAppVersion: String
    private var updateAppVersion = "1.0"
    private var updateAppUrl = "url"
    private var updateAppFileName = "app-release.apk"

    init {
        currentAppName = getCurrentAppName()
        currentAppPackage = getCurrentPackageName()
        currentAppVersion = getCurrentVersionName()
    }

    fun setUpdateJsonUrl(url : String){
        fileJson = url
        view.toast(currentAppVersion)
    }

    fun start() {
        if (fileJson.isEmpty()) {
            view.toast("Json for updates is not specified or empty")

        } else {
            view.doAsync {
                val result = Gson().fromJson((URL(fileJson).readText()), com.mvproject.updater.Update::class.java)
                uiThread {
                    updateAppVersion = result.latestVersion
                    updateAppFileName = getFileFullPath(result.file)
                    updateAppUrl = result.url

                    if (checkUpdateNeeded()){
                        view.alert("Are you want install $currentAppName update?","Update available"){
                            yesButton { executeIfOnline { downloadApk() } }
                            noButton { view.toast("Maybe Later...") }
                        }.show()
                    }
                }
            }
        }
    }

    private fun getCurrentVersionName(): String {
        return view.packageManager.getPackageInfo(view.packageName, 0).versionName
    }

    private fun getCurrentPackageName(): String {
        return view.packageManager.getPackageInfo(view.packageName, 0).packageName
    }

    private fun getCurrentAppName(): String {
        return view.packageManager.getApplicationLabel(
            view.packageManager.getApplicationInfo(
                getCurrentPackageName(),
                PackageManager.GET_META_DATA
            )
        ).toString()
    }

    private fun downloadRequest(): DownloadRequest {
            val downloadUri = Uri.parse("https://github.com/mvProject/MoviePremiers/blob/master/app/release/app-release.apk?raw=true")
            val destinationUri = Uri.parse(updateAppFileName)
            val progressDialog = view.progressDialog("Downloading...")
            return DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setStatusListener(object : DownloadStatusListenerV1 {
                    override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                        dm.release()
                        progressDialog.dismiss()
                        installApk()
                    }

                    override fun onDownloadFailed(
                        downloadRequest: DownloadRequest?,
                        errorCode: Int,
                        errorMessage: String?
                    ) {
                        dm.add(downloadRequest())
                    }

                    override fun onProgress(
                        downloadRequest: DownloadRequest?,
                        totalBytes: Long,
                        downloadedBytes: Long,
                        progress: Int
                    ) {
                        progressDialog.progress = progress
                    }

                })
    }


    private fun downloadApk(){
        KotlinPermissions.with(view as FragmentActivity)
            .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .onAccepted {
                dm.add(downloadRequest())
            }
            .onDenied {
                //List of denied permissions
            }
            .onForeverDenied {
                //List of forever denied permissions
            }
            .ask()
    }
    /**
     * Check versions to perform update
     **/
    private fun checkUpdateNeeded() : Boolean {
        return updateAppVersion > currentAppVersion
    }

    /**
     * Returning full path to file update
     */
    private fun getFileFullPath(filename: String) : String{
        return Environment.getExternalStorageDirectory().absolutePath + "/Download/" + filename
    }
    /**
     * Install specified file update
     */
    private fun installApk(){
        KotlinPermissions.with(view as FragmentActivity)
            .permissions(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            .onAccepted {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(
                    Uri.fromFile(File(updateAppFileName)),"application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                view.startActivity(intent)
            }
            .onDenied {
                //List of denied permissions
            }
            .onForeverDenied {
                //List of forever denied permissions
            }
            .ask()
    }
    /**
    * Check Internet connection avaliable
    */
    private fun hasNetwork(context: Context): Boolean {
        var isConnected = false //
        KotlinPermissions.with(view as FragmentActivity)
            .permissions(Manifest.permission.ACCESS_NETWORK_STATE)
            .onAccepted {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
                if (activeNetwork != null && activeNetwork.isConnected)
                    isConnected = true
            }
            .onDenied {
                //List of denied permissions
            }
            .onForeverDenied {
                //List of forever denied permissions
            }
            .ask()
        return isConnected
    }

    /**
     * Run function only if Internet connection available
     */
    private fun executeIfOnline(f:() -> Unit){
        if (!hasNetwork(view)) view.toast("Apk File not specified") else f()
    }
}