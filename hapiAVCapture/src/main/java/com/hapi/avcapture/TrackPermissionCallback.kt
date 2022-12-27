package com.hapi.avcapture

interface TrackPermissionCallback {
    fun onPermissionResult(permissionCode: Int)
}