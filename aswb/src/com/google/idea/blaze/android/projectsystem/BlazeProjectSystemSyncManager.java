/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.android.projectsystem;

import static com.android.tools.idea.projectsystem.ProjectSystemSyncUtil.PROJECT_SYSTEM_SYNC_TOPIC;

import com.android.annotations.VisibleForTesting;
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.settings.BlazeUserSettings;
import com.google.idea.blaze.base.sync.BlazeSyncManager;
import com.google.idea.blaze.base.sync.BlazeSyncParams;
import com.google.idea.blaze.base.sync.SyncListener;
import com.google.idea.blaze.base.sync.status.BlazeSyncStatus;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Blaze implementation of {@link ProjectSystemSyncManager} */
public class BlazeProjectSystemSyncManager implements ProjectSystemSyncManager {
  private Project project;

  BlazeProjectSystemSyncManager(Project project) {
    this.project = project;
  }

  @Override
  public boolean isSyncInProgress() {
    return BlazeSyncStatus.getInstance(project).syncInProgress();
  }

  @Override
  public boolean isSyncNeeded() {
    return BlazeSyncStatus.getInstance(project).isDirty();
  }

  @Override
  public SyncResult getLastSyncResult() {
    return LastSyncResultCache.getInstance(project).lastSyncResult;
  }

  @Override
  public ListenableFuture<SyncResult> syncProject(
      ProjectSystemSyncManager.SyncReason reason, boolean requireSourceGeneration) {
    SettableFuture<ProjectSystemSyncManager.SyncResult> syncResult = SettableFuture.create();

    if (BlazeSyncStatus.getInstance(project).syncInProgress()) {
      syncResult.setException(
          new RuntimeException(
              "A sync was requested while one is already in progress."
                  + " Use ProjectSystemSyncManager.isSyncInProgress to detect this scenario."));
    } else {
      BlazeSyncParams syncParams =
          new BlazeSyncParams.Builder("Sync", BlazeSyncParams.SyncMode.INCREMENTAL)
              .addProjectViewTargets(true)
              .addWorkingSet(BlazeUserSettings.getInstance().getExpandSyncToWorkingSet())
              .setBackgroundSync(true)
              .build();

      MessageBusConnection connection = project.getMessageBus().connect(project);
      connection.subscribe(
          PROJECT_SYSTEM_SYNC_TOPIC,
          new SyncResultListener() {
            @Override
            public void syncEnded(@NotNull SyncResult result) {
              connection.disconnect();
              syncResult.set(result);
            }
          });

      try {
        BlazeSyncManager.getInstance(project).requestProjectSync(syncParams);
      } catch (Throwable t) {
        if (!Disposer.isDisposed(connection)) {
          connection.disconnect();
        }

        syncResult.setException(t);
      }
    }

    return syncResult;
  }

  @Contract(pure = true)
  private static SyncResult convertToProjectSystemSyncResult(SyncListener.SyncResult syncResult) {
    switch (syncResult) {
      case SUCCESS:
        return SyncResult.SUCCESS;
      case PARTIAL_SUCCESS:
        return SyncResult.PARTIAL_SUCCESS;
      case CANCELLED:
        return SyncResult.CANCELLED;
      case FAILURE:
        return SyncResult.FAILURE;
    }
    throw new RuntimeException(
        "No ProjectSystemSyncManager.SyncResult equivalent for " + syncResult);
  }

  /**
   * Listens for sync status changes and broadcasts them on the message bus of the project that was
   * synced.
   */
  @VisibleForTesting
  static class SyncStatusPublisher implements SyncListener {
    @Override
    public void afterSync(
        Project project,
        BlazeContext context,
        BlazeSyncParams.SyncMode syncMode,
        SyncResult syncResult) {

      LastSyncResultCache lastSyncResultCache = LastSyncResultCache.getInstance(project);

      lastSyncResultCache.lastSyncResult =
          (syncMode == BlazeSyncParams.SyncMode.STARTUP && syncResult == SyncResult.SUCCESS)
              ? ProjectSystemSyncManager.SyncResult.SKIPPED_OUT_OF_DATE
              : convertToProjectSystemSyncResult(syncResult);

      project
          .getMessageBus()
          .syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
          .syncEnded(lastSyncResultCache.lastSyncResult);
    }
  }

  @VisibleForTesting
  static class LastSyncResultCache {
    SyncResult lastSyncResult = SyncResult.UNKNOWN;

    public static LastSyncResultCache getInstance(Project project) {
      return ServiceManager.getService(project, LastSyncResultCache.class);
    }

    /**
     * For testing only. Makes it possible to clear records of previous sync results between tests
     * so that sync results from one test don't affect other tests.
     */
    public void reset() {
      lastSyncResult = SyncResult.UNKNOWN;
    }
  }
}