package com.hapi.avcapture.permission;

import java.util.List;

public interface PermissionCallback {
    void onComplete(List<String> grantedPermissions, List<String> deniedPermissions, List<String> alwaysDeniedPermissions);
}
