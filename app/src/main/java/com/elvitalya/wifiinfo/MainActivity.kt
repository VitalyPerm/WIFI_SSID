package com.elvitalya.wifiinfo

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlin.coroutines.resume

const val TAG = "check___"

@RequiresApi(Build.VERSION_CODES.S)
class MainActivity : AppCompatActivity() {


    val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.values.find { false } ?: true
            if (granted) {
                Toast.makeText(this, "granted", Toast.LENGTH_SHORT).show()
                getSSID()
            }
        }
    val permissionsArray = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val ssid31 = MutableLiveData<String>()

    val connManager by lazy { getSystemService(ConnectivityManager::class.java) as ConnectivityManager }

    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    val networkCallback =
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d(TAG, "onCapabilitiesChanged: on air!")
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                val ssid = wifiInfo.ssid
                showToast(ssid)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionRequester.launch(permissionsArray)

    }

    private fun getSSID() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            connManager.registerNetworkCallback(request, networkCallback)
            ssid31.observe(this) { ssid ->
                showToast(ssid)
                connManager.unregisterNetworkCallback(networkCallback)
            }
        } else {
            showToast(getSsidOld())
        }
    }

    private fun showToast(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
    }


    private fun getSsidOld(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo.ssid
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun getWifiSsidApiS() = suspendCancellableCoroutine<String> { continuation ->
        val networkCallback =
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    Log.d(TAG, "onCapabilitiesChanged: isActive ${continuation.isActive}")
                    val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                    if (continuation.isActive) {
                        continuation.resume(wifiInfo.ssid)
                        continuation.cancel()
                    }
                }
            }
        connManager.registerNetworkCallback(request, networkCallback)

        continuation.invokeOnCancellation {
            Log.d(TAG, "getWifiSsidApiS: cancel")
            connManager.unregisterNetworkCallback(networkCallback)
        }
    }
}

/*
new way
        val connManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback(
            FLAG_INCLUDE_LOCATION_INFO
        ) {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                val ssid = wifiInfo.ssid
                Log.d("TAG___", "onCapabilitiesChanged: $ssid")
            }
        }
        connManager.registerNetworkCallback(request, networkCallback)
 */