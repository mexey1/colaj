package com.gecko.canvass.interfaces

/**
 * interface implemented by activities that require certain permissions to perform their operations
 */
interface PermissionsInterface {
    fun showRequestPermissionRationale(requestCode:Int)
    fun requestPermissions(requestCode:Int,permissions:Array<String>)
    fun permissionNotGranted(feature : Int)
    fun isPermissionGranted(feature:String):Boolean

}