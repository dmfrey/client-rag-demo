package com.example.sharepointbatch.sharepoint.application.port.out;

public interface DownloadItemContentPort {

    byte[] download(String driveItemId);
}
