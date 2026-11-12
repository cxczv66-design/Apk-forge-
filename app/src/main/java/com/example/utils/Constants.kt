package com.example.utils

object Constants {
    // حزمة وتفاصيل تطبيق Termux وخدمة RUN_COMMAND
    const val TERMUX_PACKAGE_NAME = "com.termux"
    const val TERMUX_ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    const val TERMUX_PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"
    
    // إضافات Intent التابعة لـ Termux:Tasker / RUN_COMMAND
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    
    // مسار مفسر Bash الافتراضي في بيئة Termux
    const val TERMUX_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
    const val TERMUX_HOME_DIR = "/data/data/com.termux/files/home"
    
    // مسارات مجلدات العمل والتنزيلات
    const val WORKSPACE_SUBDIR = "appforge_workspace"
    const val DOWNLOADS_DIR = "/storage/emulated/0/Download"
}
