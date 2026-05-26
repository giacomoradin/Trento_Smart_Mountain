package it.trentosmartmountain.app.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build

object BluetoothHelper {
  fun adapter(context: Context): BluetoothAdapter? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return manager?.adapter
  }

  fun isBluetoothEnabled(context: Context): Boolean = adapter(context)?.isEnabled == true

  fun createEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

  fun requiredAdvertisePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
      )
    } else {
      emptyArray()
    }
}
