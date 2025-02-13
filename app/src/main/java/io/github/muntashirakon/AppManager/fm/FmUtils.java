// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.fm;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.muntashirakon.AppManager.utils.ContextUtils;
import io.github.muntashirakon.io.Path;
import io.github.muntashirakon.io.fs.VirtualFileSystem;

final class FmUtils {
    @NonNull
    public static String getDisplayablePath(@NonNull Path path) {
        return getDisplayablePath(path.getUri());
    }

    @NonNull
    public static String getDisplayablePath(@NonNull Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return uri.getPath();
        }
        return uri.toString();
    }

    @SuppressWarnings("SuspiciousRegexArgument") // We're not on Windows
    public static List<String> uriToPathParts(@NonNull Uri uri) {
        switch (uri.getScheme()) {
            case ContentResolver.SCHEME_CONTENT: {
                if (isDocumentsProvider(uri.getAuthority())) {
                    List<String> paths = uri.getPathSegments();
                    if (paths.size() == 2) {
                        if ("document".equals(paths.get(0))) {
                            return Collections.singletonList(paths.get(1));
                        }
                    } else if (paths.size() == 4) {
                        if ("tree".equals(paths.get(0)) && "document".equals(paths.get(2))) {
                            String id = paths.get(1);
                            String actualPath = paths.get(3);
                            if (actualPath.length() > (id.length() + 1)) {
                                // Relative path, omitting the first `/`
                                actualPath = actualPath.substring(id.length() + 1);
                            } else actualPath = null;
                            List<String> pathParts = new ArrayList<>();
                            pathParts.add(id);
                            if (actualPath != null) {
                                pathParts.addAll(Arrays.asList(actualPath.split(File.separator)));
                            }
                            return pathParts;
                        }
                    }
                }
                // Deliberate fall-through
            }
            default:
            case ContentResolver.SCHEME_FILE:
            case VirtualFileSystem.SCHEME: {
                List<String> pathParts = new ArrayList<>();
                pathParts.add(File.separator);
                pathParts.addAll(uri.getPathSegments());
                return pathParts;
            }
        }
    }

    public static Uri uriFromPathParts(@NonNull Uri baseUri, @NonNull List<String> pathParts, int endPosition) {
        Uri.Builder builder = baseUri.buildUpon();
        builder.path(null);
        switch (baseUri.getScheme()) {
            case ContentResolver.SCHEME_CONTENT: {
                if (isDocumentsProvider(baseUri.getAuthority())) {
                    List<String> paths = baseUri.getPathSegments();
                    if (paths.size() == 2) {
                        if ("document".equals(paths.get(0))) {
                            // index 0 = document
                            // index 1 = (path) pathParts.get(0)
                            builder.appendPath("document");
                            builder.appendPath(pathParts.get(0));
                            return builder.build();
                        }
                    } else if (paths.size() == 4) {
                        if ("tree".equals(paths.get(0)) && "document".equals(paths.get(2))) {
                            // index 0 = tree
                            // index 1 = (id) paths.get(1)
                            // index 2 = document
                            // index 3 = (path) pathParts.get(0..length)
                            builder.appendPath("tree");
                            builder.appendPath(paths.get(1));
                            builder.appendPath("document");
                            StringBuilder pathBuilder = new StringBuilder();
                            for (int i = 0; i < endPosition; ++i) {
                                pathBuilder.append(pathParts.get(i)).append(File.separator);
                            }
                            pathBuilder.append(pathParts.get(endPosition));
                            builder.appendPath(pathBuilder.toString());
                            return builder.build();
                        }
                    }
                }
                // Deliberate fall-through
            }
            default:
            case ContentResolver.SCHEME_FILE:
            case VirtualFileSystem.SCHEME: {
                if (endPosition == 0) {
                    builder.path("/");
                } else {
                    // Append up-to endPosition, skipping the root (index = 0)
                    for (int i = 1; i <= endPosition; ++i) {
                        builder.appendPath(pathParts.get(i));
                    }
                }
                return builder.build();
            }
        }
    }

    private static boolean isDocumentsProvider(@NonNull String authority) {
        final Intent intent = new Intent(DocumentsContract.PROVIDER_INTERFACE);
        final List<ResolveInfo> infos = ContextUtils.getContext().getPackageManager().queryIntentContentProviders(intent, 0);
        for (ResolveInfo info : infos) {
            if (authority.equals(info.providerInfo.authority)) {
                return true;
            }
        }
        return false;
    }
}
