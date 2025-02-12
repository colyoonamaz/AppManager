// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.apk.whatsnew;

import android.app.Application;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.utils.ThreadUtils;

public class WhatsNewDialogViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ApkWhatsNewFinder.Change>> changesLiveData = new MutableLiveData<>();
    @Nullable
    private Future<?> whatsNewResult;

    public WhatsNewDialogViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        if (whatsNewResult != null) {
            whatsNewResult.cancel(true);
        }
        super.onCleared();
    }

    public LiveData<List<ApkWhatsNewFinder.Change>> getChangesLiveData() {
        return changesLiveData;
    }

    public void loadChanges(PackageInfo newPkgInfo, PackageInfo oldPkgInfo) {
        whatsNewResult = ThreadUtils.postOnBackgroundThread(() -> {
            ApkWhatsNewFinder.Change[][] changes = ApkWhatsNewFinder.getInstance().getWhatsNew(newPkgInfo, oldPkgInfo);
            List<ApkWhatsNewFinder.Change> changeList = new ArrayList<>();
            for (ApkWhatsNewFinder.Change[] changes1 : changes) {
                if (changes1.length > 0) {
                    Collections.addAll(changeList, changes1);
                }
            }
            if (changeList.size() == 0) {
                changeList.add(new ApkWhatsNewFinder.Change(ApkWhatsNewFinder.CHANGE_INFO,
                        getApplication().getString(R.string.no_changes)));
            }
            changesLiveData.postValue(changeList);
        });
    }
}
