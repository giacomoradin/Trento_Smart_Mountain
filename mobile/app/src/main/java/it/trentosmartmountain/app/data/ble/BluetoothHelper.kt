package it.trentosmartmountain.app.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothHelper {
  /** Su Android 12+ serve CONNECT per leggere lo stato BT o aprire il dialog di attivazione. */
  fun hasConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

  fun adapter(context: Context): BluetoothAdapter? {
    if (!hasAdvertisePermissions(context)) return null
    return runCatching {
      val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      manager?.adapter
    }.getOrNull()
  }

  fun isBluetoothEnabled(context: Context): Boolean {
    if (!hasConnectPermission(context)) return false
    return runCatching {
      val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      manager?.adapter?.isEnabled == true
    }.getOrDefault(false)
  }

  fun canRequestEnableBluetooth(context: Context): Boolean = hasConnectPermission(context)

  fun createEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

  /** Permessi per trasmettere il beacon SOS (Android 12+). */
  fun requiredAdvertisePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
      )
    } else {
      emptyArray()
    }

  /** Permessi per la scansione beacon (ricevente). */
  fun requiredScanPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
      )
    } else {
      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

  fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  fun hasAdvertisePermissions(context: Context): Boolean =
    hasPermissions(context, requiredAdvertisePermissions())

  fun hasScanPermissions(context: Context): Boolean =
    hasPermissions(context, requiredScanPermissions())
}
