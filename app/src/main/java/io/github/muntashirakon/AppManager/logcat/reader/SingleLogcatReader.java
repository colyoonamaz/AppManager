// SPDX-License-Identifier: WTFPL AND GPL-3.0-or-later

package io.github.muntashirakon.AppManager.logcat.reader;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import io.github.muntashirakon.AppManager.logcat.helper.LogcatHelper;
import io.github.muntashirakon.AppManager.logs.Log;

// Copyright 2012 Nolan Lawson
public class SingleLogcatReader extends AbsLogcatReader {
    private final Process logcatProcess;
    private final BufferedReader bufferedReader;
    private String lastLine;

    public SingleLogcatReader(boolean recordingMode, @LogcatHelper.LogBufferId int buffers, String lastLine)
            throws IOException {
        super(recordingMode);
        this.lastLine = lastLine;

        // Use the "time" log so we can see what time the logs were logged at
        logcatProcess = LogcatHelper.getLogcatProcess(buffers);
        bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()), 8192);
    }

    @Override
    public void killQuietly() {
        if (logcatProcess != null) {
            logcatProcess.destroy();
            Log.d("SLR", "killed 1 logcat process");
        }
    }

    @Override
    public String readLine() throws IOException {
        String line = bufferedReader.readLine();
        if (recordingMode && lastLine != null) { // Still skipping past the 'last line'
            if (lastLine.equals(line) /*|| isAfterLastTime(line)*/) {
                lastLine = null; // Indicates we've passed the last line
            }
        }
        return line;
    }

    private boolean isAfterLastTime(String line) {
        // Doing a string comparison is sufficient to determine whether this line is chronologically
        // after the last line, because the format they use is exactly the same and
        // lists larger time period before smaller ones
        return isDatedLogLine(lastLine) && isDatedLogLine(line) && line.compareTo(lastLine) > 0;
    }

    private boolean isDatedLogLine(String line) {
        // 18 is the size of the logcat timestamp
        return (!TextUtils.isEmpty(line) && line.length() >= 18 && Character.isDigit(line.charAt(0)));
    }

    @Override
    public boolean readyToRecord() {
        return recordingMode && lastLine == null;
    }

    @Override
    public List<Process> getProcesses() {
        return Collections.singletonList(logcatProcess);
    }
}
