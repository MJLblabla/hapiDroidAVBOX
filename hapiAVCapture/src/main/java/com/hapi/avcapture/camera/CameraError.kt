package com.hapi.avcapture.camera

/**
 * Class that encapsulates camera errors
 *
 * @param message the error message
 */
class CameraError(message: String) : Throwable(message)