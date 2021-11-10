package com.zerir.storage.system

import android.os.Build

fun isSdk29(): Boolean =Build.VERSION.SDK_INT == Build.VERSION_CODES.Q

fun isSdk29OrOver(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun isSdk30OrOver(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R