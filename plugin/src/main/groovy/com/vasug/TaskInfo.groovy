package com.vasug

class TaskInfo {

    long time
    String path
    boolean isSkipped
    boolean isSuccess

    TaskInfo(long time, String path, boolean isSkipped, boolean isSuccess) {
        this.time = time
        this.path = path
        this.isSkipped = isSkipped
        this.isSuccess = isSuccess
    }
}